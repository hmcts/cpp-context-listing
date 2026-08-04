package uk.gov.moj.cpp.listing.it.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges Artemis JMS queues via the Jolokia management REST API.
 * <p>
 * This is needed because the DatabaseCleaner clears the event store between tests,
 * but stale JMS messages in Artemis still reference deleted events. When the event
 * processor tries to replay these stale messages, it gets a null payload, creating
 * poison messages that block the entire event processing pipeline.
 */
public class ArtemisQueuePurger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisQueuePurger.class);
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String JOLOKIA_BASE = "http://" + HOST + ":8161/console/jolokia/";
    private static final String AUTH = Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));

    private static final List<String> ANYCAST_QUEUES = List.of(
            "jms.queue.listing.controller.command",
            "jms.queue.listing.handler.command",
            "jms.queue.DLQ"
    );

    /**
     * Bounded budget for the consume-side quiescence wait before table truncation. Generous
     * because loaded vld nodes stretch event processing well past the old 5s budget (the give-up
     * path is what let teardown truncation race in-flight projections on builds 765431-765702);
     * the wait returns as soon as counts hit zero, so the healthy-case cost is unchanged.
     */
    private static final long QUIESCE_MAX_WAIT_MILLIS = 30_000;
    private static final long QUIESCE_POLL_INTERVAL_MILLIS = 100;
    /** Consecutive polls with unreadable DeliveringCounts before giving up loudly. */
    private static final int QUIESCE_MAX_UNREADABLE_POLLS = 3;
    /** Sentinel: the delivering count could not be read, NOT the same as quiesced. */
    private static final long DELIVERING_COUNT_UNKNOWN = -1;

    private static final List<String> EVENT_TOPICS = List.of(
            "jms.topic.listing.event",
            "jms.topic.public.event"
    );

    /**
     * Waits (bounded) for the listing event processor to finish any in-flight projection before the
     * caller truncates {@code stream_status} / {@code stream_buffer} / the view-store tables.
     *
     * <p>This closes the B2 teardown-vs-processor race: the next test's {@code @BeforeEach} truncation
     * could yank {@code stream_status} out from under a projection transaction that the PREVIOUS test's
     * event still has in flight, producing {@code JsonValue.NULL} / {@code StreamStatusLockingException}
     * noise (and the occasional hang) in the server log.</p>
     *
     * <p>The signal is each listing/public subscriber queue's {@code DeliveringCount} (messages dispatched
     * to the MDB but not yet acked). When it reaches zero, no projection is mid-flight. This is a pure
     * <em>consume-side</em> read — it never drains the event-store publish relay itself; the publish side
     * is handled separately by {@code DatabaseCleaner#awaitPublishQueuesEmpty}, which
     * {@code AbstractIT#setUp} runs immediately before this wait so events the relay releases are then
     * quiesced here and purged after. Best-effort: if the count has not settled within
     * {@value #QUIESCE_MAX_WAIT_MILLIS}ms (e.g. a failing redelivery loop), it returns and lets cleanup
     * proceed rather than blocking the run; unreadable counts are retried and then abandoned loudly
     * instead of being silently treated as quiesced.</p>
     */
    public static void quiesceListingEventProcessing() {
        final long deadline = System.currentTimeMillis() + QUIESCE_MAX_WAIT_MILLIS;
        int unreadablePolls = 0;
        while (System.currentTimeMillis() < deadline) {
            final long delivering = totalDeliveringCount();
            if (delivering == 0) {
                return;
            }
            if (delivering == DELIVERING_COUNT_UNKNOWN) {
                // Unreadable is NOT quiesced: silently treating it as zero is how a broken
                // Jolokia path degrades the whole quiesce to a no-op. Retry a few times, then
                // give up loudly rather than blocking every test's setup.
                if (++unreadablePolls >= QUIESCE_MAX_UNREADABLE_POLLS) {
                    LOGGER.error("Cannot read DeliveringCounts from Jolokia after {} attempts; "
                            + "proceeding with cleanup UNQUIESCED — truncation may race in-flight projections", unreadablePolls);
                    return;
                }
            } else {
                unreadablePolls = 0;
            }
            try {
                Thread.sleep(QUIESCE_POLL_INTERVAL_MILLIS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        LOGGER.error("Event processing did not quiesce within {}ms; proceeding with cleanup — "
                + "truncation may race in-flight projections", QUIESCE_MAX_WAIT_MILLIS);
    }

    private static long totalDeliveringCount() {
        long total = 0;
        for (final String topic : EVENT_TOPICS) {
            final List<String> subscriberQueues = getSubscriberQueuesOrNull(topic);
            if (subscriberQueues == null) {
                return DELIVERING_COUNT_UNKNOWN;
            }
            for (final String subscriberQueue : subscriberQueues) {
                final long count = readDeliveringCount(topic, subscriberQueue);
                if (count == DELIVERING_COUNT_UNKNOWN) {
                    return DELIVERING_COUNT_UNKNOWN;
                }
                total += count;
            }
        }
        return total;
    }

    private static long readDeliveringCount(final String topicAddress, final String subscriberQueue) {
        try {
            final String url = JOLOKIA_BASE + "read/org.apache.activemq.artemis:address=%22"
                    + topicAddress.replace(".", "%2E")
                    + "%22,broker=%22default%22,component=addresses,queue=%22"
                    + subscriberQueue.replace(".", "%2E")
                    + "%22,routing-type=%22multicast%22,subcomponent=queues/DeliveringCount";
            final String value = extractValue(httpGet(url));
            return Long.parseLong(value.trim());
        } catch (final Exception e) {
            LOGGER.warn("Failed to read DeliveringCount for {}: {}", subscriberQueue, e.getMessage());
            return DELIVERING_COUNT_UNKNOWN;
        }
    }

    /**
     * Purges all listing-related Artemis queues (anycast queues, DLQ, and event topic subscriber queues).
     */
    public static void purgeAllListingQueues() {
        for (final String queue : ANYCAST_QUEUES) {
            purgeAnycastQueue(queue);
        }
        purgeTopicSubscribers("jms.topic.listing.event");
        purgeTopicSubscribers("jms.topic.public.event");
    }

    private static void purgeAnycastQueue(final String queueName) {
        final String mbean = String.format(
                "org.apache.activemq.artemis:address=\"%s\",broker=\"default\",component=addresses,queue=\"%s\",routing-type=\"anycast\",subcomponent=queues",
                queueName, queueName);
        executeRemoveAllMessages(mbean, queueName);
    }

    private static void purgeTopicSubscribers(final String topicAddress) {
        final List<String> subscriberQueues = getSubscriberQueues(topicAddress);
        for (final String subscriberQueue : subscriberQueues) {
            final String mbean = String.format(
                    "org.apache.activemq.artemis:address=\"%s\",broker=\"default\",component=addresses,queue=\"%s\",routing-type=\"multicast\",subcomponent=queues",
                    topicAddress, subscriberQueue);
            executeRemoveAllMessages(mbean, subscriberQueue);
        }
    }

    private static List<String> getSubscriberQueues(final String topicAddress) {
        final List<String> subscriberQueues = getSubscriberQueuesOrNull(topicAddress);
        return subscriberQueues == null ? List.of() : subscriberQueues;
    }

    /**
     * As {@link #getSubscriberQueues(String)} but returns {@code null} on failure so the
     * quiesce path can distinguish "no subscribers" from "cannot see the broker" — the purge
     * path degrades to an empty list, but for quiesce that conflation silently disables the
     * whole wait.
     */
    private static List<String> getSubscriberQueuesOrNull(final String topicAddress) {
        try {
            final String url = JOLOKIA_BASE + "read/org.apache.activemq.artemis:address=%22"
                    + topicAddress.replace(".", "%2E")
                    + "%22,broker=%22default%22,component=addresses/QueueNames";
            final String response = httpGet(url);
            return parseQueueNames(response);
        } catch (final Exception e) {
            LOGGER.warn("Failed to get subscriber queues for {}: {}", topicAddress, e.getMessage());
            return null;
        }
    }

    private static void executeRemoveAllMessages(final String mbean, final String displayName) {
        try {
            final String body = String.format("{\"type\":\"exec\",\"mbean\":\"%s\",\"operation\":\"removeAllMessages\"}", mbean);
            final String response = httpPost(JOLOKIA_BASE, body);
            if (response.contains("\"status\":200")) {
                final String value = extractValue(response);
                if (!"0".equals(value)) {
                    LOGGER.info("Purged {} messages from {}", value, displayName);
                }
            }
        } catch (final Exception e) {
            LOGGER.warn("Failed to purge queue {}: {}", displayName, e.getMessage());
        }
    }

    private static String httpGet(final String urlStr) throws Exception {
        final HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Authorization", "Basic " + AUTH);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static String httpPost(final String urlStr, final String body) throws Exception {
        final HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + AUTH);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static List<String> parseQueueNames(final String json) {
        // Simple JSON array extraction: find "value":["name1","name2"]
        final int start = json.indexOf("\"value\":[");
        if (start < 0) return List.of();
        final int arrayStart = json.indexOf('[', start);
        final int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) return List.of();
        final String arrayContent = json.substring(arrayStart + 1, arrayEnd);
        if (arrayContent.isBlank()) return List.of();
        return java.util.Arrays.stream(arrayContent.split(","))
                .map(s -> s.trim().replace("\"", "").replace("\\\\", "\\"))
                .toList();
    }

    private static String extractValue(final String json) {
        final int idx = json.indexOf("\"value\":");
        if (idx < 0) return "?";
        final int start = idx + 8;
        final int end = json.indexOf(',', start);
        return end > 0 ? json.substring(start, end).trim() : json.substring(start).replace("}", "").trim();
    }
}
