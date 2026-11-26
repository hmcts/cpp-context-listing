package uk.gov.moj.cpp.listing.it.util;

import uk.gov.justice.services.messaging.JsonObjects;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges Artemis JMS queues via the Jolokia management REST API.
 * <p>
 * This is needed because the DatabaseCleaner clears the event store between tests,
 * but stale JMS messages in Artemis still reference deleted events. When the event
 * processor tries to replay these stale messages, it gets a null payload, creating
 * poison messages that block the entire event processing pipeline.
 * <p>
 * Every call is a Jolokia bulk POST whose body is built with {@link JsonObjects} and whose MBean
 * names are built with {@link ObjectName#quote(String)}. The service's own subscriptions are named
 * with escaped dots ({@code listing\.event\.listener\.listing\.event}); hand-built URLs and
 * {@code String.format} JSON bodies produced "Invalid escape sequence" / "Invalid JSON request"
 * errors for them, so the quiesce always gave up and the purge never removed anything.
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
    /** Sentinel: the count could not be read, NOT the same as quiesced. */
    private static final long DELIVERING_COUNT_UNKNOWN = -1;

    private static final List<String> EVENT_TOPICS = List.of(
            "jms.topic.listing.event",
            "jms.topic.public.event"
    );

    private static final List<String> PIPELINE_COMMAND_QUEUES = List.of(
            "jms.queue.listing.controller.command",
            "jms.queue.listing.handler.command"
    );

    /** Non-durable subscriptions created by test JMS consumers are named by a bare UUID. */
    private static final Pattern TEST_CONSUMER_QUEUE =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    /**
     * Waits (bounded) for the listing event processor to finish any in-flight projection before the
     * caller truncates {@code stream_status} / {@code stream_buffer} / the view-store tables.
     *
     * <p>This closes the B2 teardown-vs-processor race: the next test's {@code @BeforeEach} truncation
     * could yank {@code stream_status} out from under a projection transaction that the PREVIOUS test's
     * event still has in flight, producing {@code JsonValue.NULL} / {@code StreamStatusLockingException}
     * noise (and the occasional hang) in the server log.</p>
     *
     * <p>The signal is the {@code DeliveringCount} of the service's own listing/public subscriptions
     * (messages dispatched to the MDB but not yet acked; test-consumer subscriptions are excluded). When
     * it reaches zero, no projection is mid-flight. This is a pure
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

    /**
     * Messages still anywhere in listing's own async pipeline: both listing command queues plus the
     * service's durable subscriptions on the listing/public event topics. Uses {@code MessageCount}
     * (routed-but-not-yet-dispatched messages count too), unlike the {@code DeliveringCount} the
     * setUp quiesce reads. Test-consumer subscriptions (UUID-named) are excluded: they legitimately
     * hold messages the test has not read yet. Returns {@value #DELIVERING_COUNT_UNKNOWN} when any
     * count is unreadable.
     */
    public static long pipelineMessageCount() {
        final Map<String, List<String>> subscriberQueues = subscriberQueuesOrNull();
        if (subscriberQueues == null) {
            return DELIVERING_COUNT_UNKNOWN;
        }
        final List<JsonObject> reads = new ArrayList<>();
        PIPELINE_COMMAND_QUEUES.forEach(queue -> reads.add(read(queueMBean(queue, queue, "anycast"), "MessageCount")));
        subscriberQueues.forEach((topic, queues) -> queues.stream()
                .filter(queue -> !TEST_CONSUMER_QUEUE.matcher(queue).matches())
                .forEach(queue -> reads.add(read(queueMBean(topic, queue, "multicast"), "MessageCount"))));
        return sumOrUnknown(reads);
    }

    private static long totalDeliveringCount() {
        final Map<String, List<String>> subscriberQueues = subscriberQueuesOrNull();
        if (subscriberQueues == null) {
            return DELIVERING_COUNT_UNKNOWN;
        }
        // Only the service's own subscriptions. A test-consumer subscription lives for the whole test
        // class (JmsResourceManagementExtension closes it in afterAll and drains it in beforeEach, i.e.
        // BEFORE this setUp): an event from the previous test's async tail that lands in it after the
        // drain sits "delivering" in an idle consumer until the next drain, which would hold this wait
        // for the full budget although no projection is in flight.
        final List<JsonObject> reads = new ArrayList<>();
        subscriberQueues.forEach((topic, queues) -> queues.stream()
                .filter(queue -> !TEST_CONSUMER_QUEUE.matcher(queue).matches())
                .forEach(queue -> reads.add(read(queueMBean(topic, queue, "multicast"), "DeliveringCount"))));
        return sumOrUnknown(reads);
    }

    /**
     * Purges all listing-related Artemis queues (anycast queues, DLQ, and event topic subscriber queues).
     */
    public static void purgeAllListingQueues() {
        final List<JsonObject> purges = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        for (final String queue : ANYCAST_QUEUES) {
            purges.add(removeAllMessages(queueMBean(queue, queue, "anycast")));
            names.add(queue);
        }
        final Map<String, List<String>> subscriberQueues = subscriberQueuesOrNull();
        if (subscriberQueues != null) {
            subscriberQueues.forEach((topic, queues) -> queues.forEach(queue -> {
                purges.add(removeAllMessages(queueMBean(topic, queue, "multicast")));
                names.add(queue);
            }));
        }
        try {
            final JsonArray responses = bulk(purges);
            for (int i = 0; i < responses.size(); i++) {
                final JsonObject response = responses.getJsonObject(i);
                if (response.getInt("status", 0) != 200) {
                    LOGGER.warn("Failed to purge queue {}: {}", names.get(i), response.getString("error", "?"));
                } else if (response.getJsonNumber("value") != null && response.getJsonNumber("value").longValue() != 0) {
                    LOGGER.info("Purged {} messages from {}", response.getJsonNumber("value").longValue(), names.get(i));
                }
            }
        } catch (final IOException e) {
            LOGGER.warn("Failed to purge listing queues: {}", e.getMessage());
        }
    }

    /** Subscriber queue names per event topic, or {@code null} when the broker cannot be read. */
    private static Map<String, List<String>> subscriberQueuesOrNull() {
        final List<JsonObject> reads = new ArrayList<>();
        EVENT_TOPICS.forEach(topic -> reads.add(read(
                "org.apache.activemq.artemis:address=" + ObjectName.quote(topic) + ",broker=\"default\",component=addresses",
                "QueueNames")));
        try {
            final JsonArray responses = bulk(reads);
            final Map<String, List<String>> queuesByTopic = new LinkedHashMap<>();
            for (int i = 0; i < responses.size(); i++) {
                final JsonObject response = responses.getJsonObject(i);
                if (response.getInt("status", 0) != 200) {
                    LOGGER.warn("Failed to get subscriber queues for {}: {}", EVENT_TOPICS.get(i), response.getString("error", "?"));
                    return null;
                }
                queuesByTopic.put(EVENT_TOPICS.get(i), response.getJsonArray("value").getValuesAs(JsonString.class)
                        .stream().map(JsonString::getString).toList());
            }
            return queuesByTopic;
        } catch (final IOException e) {
            LOGGER.warn("Failed to get subscriber queues: {}", e.getMessage());
            return null;
        }
    }

    private static long sumOrUnknown(final List<JsonObject> reads) {
        try {
            long total = 0;
            final JsonArray responses = bulk(reads);
            for (int i = 0; i < responses.size(); i++) {
                final JsonObject response = responses.getJsonObject(i);
                if (response.getInt("status", 0) != 200 || response.getJsonNumber("value") == null) {
                    LOGGER.warn("Failed to read queue count: {}", response.getString("error", "?"));
                    return DELIVERING_COUNT_UNKNOWN;
                }
                total += response.getJsonNumber("value").longValue();
            }
            return total;
        } catch (final IOException e) {
            LOGGER.warn("Failed to read queue counts: {}", e.getMessage());
            return DELIVERING_COUNT_UNKNOWN;
        }
    }

    private static String queueMBean(final String address, final String queue, final String routingType) {
        return "org.apache.activemq.artemis:address=" + ObjectName.quote(address)
                + ",broker=\"default\",component=addresses,queue=" + ObjectName.quote(queue)
                + ",routing-type=\"" + routingType + "\",subcomponent=queues";
    }

    private static JsonObject read(final String mbean, final String attribute) {
        return JsonObjects.createObjectBuilder().add("type", "read").add("mbean", mbean).add("attribute", attribute).build();
    }

    private static JsonObject removeAllMessages(final String mbean) {
        return JsonObjects.createObjectBuilder().add("type", "exec").add("mbean", mbean).add("operation", "removeAllMessages").build();
    }

    /** One HTTP round trip for any number of Jolokia requests; responses come back in request order. */
    private static JsonArray bulk(final List<JsonObject> requests) throws IOException {
        if (requests.isEmpty()) {
            return JsonObjects.createArrayBuilder().build();
        }
        final JsonArrayBuilder body = JsonObjects.createArrayBuilder();
        requests.forEach(body::add);
        final HttpURLConnection conn = (HttpURLConnection) new URL(JOLOKIA_BASE).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + AUTH);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.build().toString().getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream in = conn.getInputStream();
             JsonReader reader = JsonObjects.createReader(new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8)))) {
            return reader.readArray();
        } finally {
            conn.disconnect();
        }
    }
}
