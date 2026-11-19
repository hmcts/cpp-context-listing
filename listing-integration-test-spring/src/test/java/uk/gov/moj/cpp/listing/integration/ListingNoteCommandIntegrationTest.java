package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.moj.cpp.listing.integration.FrameworkEnvelopeFormat.CPPNAME;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.json.JsonObject;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * P2.3.0 first command slice end to end (MBD refactor, ADR 014): the three listing-note commands run
 * synchronously (202), change {@code listing_notes} and, through the outbox, the ported processor
 * republishes the public note events on {@code public.event} in the framework wire format, with the
 * metadata carried from the command (user, causation) as the JEE app did.
 */
class ListingNoteCommandIntegrationTest extends AbstractIntegrationTest {

    private static final String EDIT = "application/vnd.listing.command.edit-listing-note+json";
    private static final String DELETE = "application/vnd.listing.command.delete-listing-note+json";
    private static final String NOTES_PATH = COMMAND_API + "/listing-notes/";

    private static jakarta.jms.Connection jms;

    @BeforeAll
    static void connect() throws JMSException {
        jms = new ActiveMQConnectionFactory(MessagingIntegrationTest.ARTEMIS_URL).createConnection("listing", "listing");
        jms.start();
    }

    @AfterAll
    static void disconnect() throws JMSException {
        if (jms != null) {
            jms.close();
        }
    }

    @Test
    void createEditAndDeleteANoteWritesTheViewstoreAndPublishesThePublicEvents() throws Exception {
        final UUID courtRoomId = UUID.randomUUID();
        final String date = "2031-04-0" + (1 + (int) (Math.random() * 8));
        final UUID noteId = UUID.nameUUIDFromBytes((courtRoomId + "/" + date).getBytes(StandardCharsets.UTF_8));

        try (Session session = jms.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageConsumer created = consumer(session, "public.listing.created-listing-note");
             MessageConsumer edited = consumer(session, "public.listing.note-edited");
             MessageConsumer deleted = consumer(session, "public.listing.deleted-listing-note")) {

            accepted(post(COMMAND_API + "/listing-note", LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE,
                    "{\"courtRoomId\":\"" + courtRoomId + "\",\"hearingDate\":\"" + date + "\",\"noteDescription\":\"first\"}"));
            assertThat(note(noteId)).contains("first");
            final JsonObject createdEvent = receive(created, noteId, "id");
            assertThat(createdEvent.getString("date")).isEqualTo(date);
            assertThat(createdEvent.getString("courtRoomId")).isEqualTo(courtRoomId.toString());
            assertThat(createdEvent.getString("note")).isEqualTo("first");
            final JsonObject metadata = createdEvent.getJsonObject("_metadata");
            assertThat(metadata.getJsonObject("context").getString("user")).isEqualTo(LISTING_OFFICER_USER_ID.toString());
            assertThat(metadata.getJsonObject("stream").getString("id")).isEqualTo(noteId.toString());
            assertThat(metadata.getJsonArray("causation")).hasSize(1);
            FrameworkEnvelopeFormat.assertSameShapeAsLegacy(createdEvent);

            accepted(post(NOTES_PATH + noteId, LISTING_OFFICER_USER_ID, EDIT, "{\"noteDescription\":\"second\"}"));
            assertThat(note(noteId)).contains("second");
            final JsonObject editedEvent = receive(edited, noteId, "noteId");
            assertThat(editedEvent.getString("noteDescription")).isEqualTo("second");

            accepted(post(NOTES_PATH + noteId, LISTING_OFFICER_USER_ID, DELETE, "{}"));
            assertThat(note(noteId)).isEmpty();
            receive(deleted, noteId, "noteId");
        }
    }

    @Test
    void aDuplicateCreateIsAcceptedAndChangesNothing() throws Exception {
        final UUID courtRoomId = UUID.randomUUID();
        final String date = LocalDate.of(2031, 5, 1).toString();
        final UUID noteId = UUID.nameUUIDFromBytes((courtRoomId + "/" + date).getBytes(StandardCharsets.UTF_8));
        final String body = "{\"courtRoomId\":\"" + courtRoomId + "\",\"hearingDate\":\"" + date + "\",\"noteDescription\":\"%s\"}";

        accepted(post(COMMAND_API + "/listing-note", LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, body.formatted("original")));
        accepted(post(COMMAND_API + "/listing-note", LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, body.formatted("duplicate")));

        assertThat(note(noteId)).contains("original");
    }

    private static void accepted(final ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private static MessageConsumer consumer(final Session session, final String name) throws JMSException {
        return session.createConsumer(session.createTopic(MessagingIntegrationTest.PUBLIC_EVENT_TOPIC), CPPNAME + " = '" + name + "'");
    }

    /** The next event on {@code consumer} whose {@code idField} is {@code noteId} (other tests' events are skipped). */
    private static JsonObject receive(final MessageConsumer consumer, final UUID noteId, final String idField) throws JMSException {
        final long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            final Message message = consumer.receive(1_000);
            if (message instanceof TextMessage text) {
                final JsonObject event = FrameworkEnvelopeFormat.accept(text.getText());
                if (noteId.toString().equals(event.getString(idField, null))) {
                    return event;
                }
            }
        }
        throw new AssertionError("No public event for note " + noteId + " within 30s");
    }

    private static Optional<String> note(final UUID noteId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(ViewstoreFixtures.DB_URL, "listing", "listing");
             PreparedStatement statement = connection.prepareStatement("SELECT note FROM listing_notes WHERE id = ?")) {
            statement.setObject(1, noteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getString(1)) : Optional.empty();
            }
        }
    }
}
