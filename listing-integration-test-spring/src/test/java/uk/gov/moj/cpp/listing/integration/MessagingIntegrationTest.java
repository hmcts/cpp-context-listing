package uk.gov.moj.cpp.listing.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.listing.integration.FrameworkEnvelopeFormat.CPPNAME;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * P2.2 messaging against the compose Artemis (MBD refactor §10.1 P2.2 exit criteria, ADR 013):
 * <ol type="a">
 *     <li>an {@code event_outbox} row is published by the relay to {@code public.event} in the
 *     framework wire format (TextMessage, {@code CPPNAME}, envelope body);</li>
 *     <li>a framework-format message with a subscribed name is consumed, dispatched to its real
 *     handler, and a duplicate ({@code _metadata.id}) is ignored;</li>
 *     <li>a message whose handler always throws is dead-lettered after the broker's redeliveries;</li>
 *     <li>a non-subscribed name is not consumed (the selector filters it).</li>
 * </ol>
 * P2.4: the P2.2 test-only probe handler is gone; (b)–(d) use the ported handlers. {@value #PROBED}
 * (JEE {@code ListingEventProcessor.handleHearingMarkedAsDuplicate}) first releases the hearing's
 * courtscheduler sessions — one {@code DELETE …/sessions/{hearingId}} to WireMock, counted from its
 * request journal, so each handled delivery is observable — then marks the hearing as duplicate.
 * {@value #FAILING} without a {@code hearing} fails in its handler on every delivery, as in JEE.
 */
class MessagingIntegrationTest {

    static final String ARTEMIS_URL = System.getProperty("artemis.url", "tcp://localhost:61626");
    static final String PUBLIC_EVENT_TOPIC = "jms.topic.public.event";
    static final String SUBSCRIPTION_QUEUE = "listing\\.event\\.processor\\.public\\.event";
    static final String PROBED = "public.events.hearing.marked-as-duplicate";
    static final String FAILING = "public.events.hearing.hearing-resulted";
    private static final String WIREMOCK_URL = System.getProperty("wiremock.baseUrl", "http://localhost:8191");
    private static final String SESSIONS_PATH = "/listingcourtscheduler-api/rest/courtscheduler/sessions/";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String USER = "11111111-1111-1111-1111-111111111111";
    private static final DateTimeFormatter CREATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static jakarta.jms.Connection jms;

    @BeforeAll
    static void connect() throws JMSException {
        jms = new ActiveMQConnectionFactory(ARTEMIS_URL).createConnection("listing", "listing");
        jms.start();
    }

    @AfterAll
    static void disconnect() throws JMSException {
        if (jms != null) {
            jms.close();
        }
    }

    @Test
    void a_outboxRowIsPublishedToPublicEventInTheFrameworkWireFormat() throws Exception {
        final String name = "public.listing.p22-relay-probe";
        final UUID eventId = UUID.randomUUID();
        final UUID streamId = UUID.randomUUID();
        // Same structure (and field order) as the message captured from the JEE app.
        final String envelope = "{\"_metadata\":{\"name\":\"" + name + "\",\"context\":{\"user\":\"" + USER + "\"},"
                + "\"id\":\"" + eventId + "\",\"createdAt\":\"" + now() + "\",\"causation\":[\"" + UUID.randomUUID() + "\"],"
                + "\"stream\":{\"id\":\"" + streamId + "\"}},\"id\":\"" + streamId + "\",\"date\":\"2031-03-07\","
                + "\"courtRoomId\":\"" + UUID.randomUUID() + "\",\"note\":\"p22 relay probe\"}";

        try (Session session = jms.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageConsumer consumer = session.createConsumer(session.createTopic(PUBLIC_EVENT_TOPIC), CPPNAME + " = '" + name + "'")) {
            insertOutboxRow(eventId, name, streamId, envelope);

            final Message message = consumer.receive(30_000);

            assertThat(message).as("published by the outbox relay").isInstanceOf(TextMessage.class);
            assertThat(message.getStringProperty(CPPNAME)).isEqualTo(name);
            assertThat(applicationPropertyNames(message)).containsExactly(CPPNAME);
            assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
            assertThat(message.getJMSPriority()).isEqualTo(4);
            assertThat(message.getJMSExpiration()).isZero();
            assertThat(message.getJMSType()).isNull();
            assertThat(message.getJMSCorrelationID()).isNull();
            assertThat(message.getJMSReplyTo()).isNull();

            final String body = ((TextMessage) message).getText();
            assertThat(body).isEqualTo(envelope);
            final JsonObject published = FrameworkEnvelopeFormat.accept(body);
            FrameworkEnvelopeFormat.assertSameShapeAsLegacy(published);
            assertThat(published.getJsonObject("_metadata").getString("id")).isEqualTo(eventId.toString());
        }
        await().atMost(10, SECONDS).until(() -> !outboxContains(eventId));
    }

    @Test
    void b_subscribedEventIsDispatchedToTheHandlerAndADuplicateIsIgnored() throws Exception {
        final UUID eventId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final String inbound = inboundEnvelope(eventId, PROBED, duplicateHearing(hearingId));

        try (Session session = jms.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            publish(session, PROBED, inbound);

            await().atMost(30, SECONDS).until(() -> dedupCount(eventId) == 1);
            await().atMost(10, SECONDS).until(() -> sessionReleases(hearingId) == 1);

            // The same message again (same _metadata.id), then a marker so we know the duplicate was consumed.
            publish(session, PROBED, inbound);
            final UUID marker = UUID.randomUUID();
            final UUID markerHearingId = UUID.randomUUID();
            publish(session, PROBED, inboundEnvelope(marker, PROBED, duplicateHearing(markerHearingId)));

            await().atMost(30, SECONDS).until(() -> sessionReleases(markerHearingId) == 1);
            await().during(3, SECONDS).atMost(10, SECONDS).until(() -> sessionReleases(hearingId) == 1);
            assertThat(dedupCount(eventId)).isEqualTo(1);
            assertThat(dedupCount(marker)).isEqualTo(1);
        }
    }

    @Test
    void c_messageWhoseHandlerAlwaysFailsEndsInTheDlqAfterRedeliveries() throws Exception {
        final UUID eventId = UUID.randomUUID();

        try (Session session = jms.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageConsumer dlq = session.createConsumer(session.createQueue("DLQ"), CPPNAME + " = '" + FAILING + "'")) {
            // hearing-resulted without "hearing": the handler fails on every delivery
            publish(session, FAILING, inboundEnvelope(eventId, FAILING, "{}"));

            final Message deadLettered = awaitMessageWithId(dlq, eventId, 90_000);
            assertThat(deadLettered).as("dead-lettered after max-delivery-attempts (10)").isNotNull();
            assertThat(deadLettered.getStringProperty("_AMQ_ORIG_ADDRESS")).isEqualTo(PUBLIC_EVENT_TOPIC);
            // The app consumed through the legacy processor's durable shared subscription queue.
            assertThat(deadLettered.getStringProperty("_AMQ_ORIG_QUEUE")).isEqualTo(SUBSCRIPTION_QUEUE);
            assertThat(deadLettered.getStringProperty(CPPNAME)).isEqualTo(FAILING);

            // Every attempt rolled back: no dedup row.
            assertThat(dedupCount(eventId)).isZero();
        }
    }

    @Test
    void d_nonSubscribedEventIsNotConsumed() throws Exception {
        final UUID notSubscribed = UUID.randomUUID();
        final String name = "public.progression.p22-not-subscribed";
        final UUID marker = UUID.randomUUID();

        try (Session session = jms.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            publish(session, name, inboundEnvelope(notSubscribed, name, "{}"));
            publish(session, PROBED, inboundEnvelope(marker, PROBED, duplicateHearing(UUID.randomUUID())));

            await().atMost(30, SECONDS).until(() -> dedupCount(marker) == 1);

            // The listener records every message it consumes; the selector kept this one away.
            await().during(3, SECONDS).atMost(10, SECONDS).until(() -> dedupCount(notSubscribed) == 0);
        }
    }

    private static String duplicateHearing(final UUID hearingId) {
        return "{\"hearingId\":\"" + hearingId + "\"}";
    }

    /** DELETE requests WireMock received for the hearing's sessions (the handler's first step). */
    private static int sessionReleases(final UUID hearingId) throws Exception {
        final HttpRequest count = HttpRequest.newBuilder(URI.create(WIREMOCK_URL + "/__admin/requests/count"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"method\":\"DELETE\",\"url\":\"" + SESSIONS_PATH + hearingId + "\"}"))
                .build();
        final HttpResponse<String> response = HTTP.send(count, HttpResponse.BodyHandlers.ofString());
        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            return reader.readObject().getInt("count");
        }
    }

    private static String inboundEnvelope(final UUID id, final String name, final String payloadObject) {
        final JsonObject payload;
        try (JsonReader reader = Json.createReader(new StringReader(payloadObject))) {
            payload = reader.readObject();
        }
        final var envelope = Json.createObjectBuilder()
                .add("_metadata", Json.createObjectBuilder()
                        .add("id", id.toString())
                        .add("name", name)
                        .add("createdAt", now())
                        .add("causation", Json.createArrayBuilder().add(UUID.randomUUID().toString()))
                        .add("context", Json.createObjectBuilder().add("user", USER))
                        .add("stream", Json.createObjectBuilder().add("id", UUID.randomUUID().toString()).add("version", 3)))
                .add("caseId", UUID.randomUUID().toString());
        payload.forEach(envelope::add);
        return envelope.build().toString();
    }

    /** Sends the way the framework's JmsSender did: TextMessage + CPPNAME, persistent, non-transacted. */
    private static void publish(final Session session, final String name, final String body) throws JMSException {
        try (MessageProducer producer = session.createProducer(session.createTopic(PUBLIC_EVENT_TOPIC))) {
            final TextMessage message = session.createTextMessage(body);
            message.setStringProperty(CPPNAME, name);
            producer.send(message);
        }
    }

    private static Message awaitMessageWithId(final MessageConsumer consumer, final UUID id, final long timeoutMillis) throws JMSException {
        final long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            final Message message = consumer.receive(1_000);
            if (message instanceof TextMessage text
                    && FrameworkEnvelopeFormat.accept(text.getText()).getJsonObject("_metadata").getString("id").equals(id.toString())) {
                return message;
            }
        }
        return null;
    }

    private static List<String> applicationPropertyNames(final Message message) throws JMSException {
        final List<String> names = new ArrayList<>();
        final Enumeration<?> all = message.getPropertyNames();
        while (all.hasMoreElements()) {
            final String name = (String) all.nextElement();
            if (!name.startsWith("JMSX")) {
                names.add(name);
            }
        }
        return names;
    }

    private static String now() {
        return Instant.now().atOffset(ZoneOffset.UTC).format(CREATED_AT);
    }

    private static void insertOutboxRow(final UUID eventId, final String name, final UUID streamId, final String envelope)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(ViewstoreFixtures.DB_URL, "listing", "listing");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO event_outbox (event_id, event_name, stream_id, envelope, created_at) VALUES (?, ?, ?, ?, ?)")) {
            insert.setObject(1, eventId);
            insert.setString(2, name);
            insert.setObject(3, streamId);
            insert.setString(4, envelope);
            insert.setTimestamp(5, Timestamp.from(Instant.now()));
            insert.executeUpdate();
        }
    }

    private static boolean outboxContains(final UUID eventId) throws SQLException {
        return count("SELECT COUNT(*) FROM event_outbox WHERE event_id = ?", eventId) > 0;
    }

    private static int dedupCount(final UUID eventId) throws SQLException {
        return count("SELECT COUNT(*) FROM inbound_event_dedup WHERE event_id = ?", eventId);
    }

    private static int count(final String sql, final UUID id) throws SQLException {
        try (Connection connection = DriverManager.getConnection(ViewstoreFixtures.DB_URL, "listing", "listing");
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setObject(1, id);
            try (ResultSet rs = query.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
