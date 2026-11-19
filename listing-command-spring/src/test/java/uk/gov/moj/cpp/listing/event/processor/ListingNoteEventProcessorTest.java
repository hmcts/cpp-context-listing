package uk.gov.moj.cpp.listing.event.processor;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ListingNoteEventProcessorTest {

    private final EventOutbox outbox = mock(EventOutbox.class);
    private final ListingNoteEventProcessor processor = new ListingNoteEventProcessor(new ProcessorSender(outbox));

    private final UUID commandId = randomUUID();
    private final UUID noteId = randomUUID();
    private final UUID courtRoomId = randomUUID();

    @Test
    void subscribesToTheThreeNoteEvents() {
        assertThat(processor.handledEvents()).containsExactlyInAnyOrder("listing.events.created-listing-note",
                "listing.events.listing-note-edited", "listing.events.deleted-listing-note");
    }

    @Test
    void createdNoteIsRepublishedWithTheLegacyPublicPayload() {
        final Envelope created = privateEvent("listing.events.created-listing-note", Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString())
                .add("hearingDate", "2026-11-20")
                .add("id", noteId.toString())
                .add("noteDescription", "a note")
                .build());

        final Envelope published = handle(created);

        assertThat(published.name()).isEqualTo("public.listing.created-listing-note");
        assertThat(published.payload()).isEqualTo(Json.createObjectBuilder()
                .add("id", noteId.toString())
                .add("date", "2026-11-20")
                .add("courtRoomId", courtRoomId.toString())
                .add("note", "a note")
                .build());
        assertThatMetadataIsCarriedOver(created, published);
    }

    @Test
    void editedNoteIsRepublishedWithTheSamePayload() {
        final JsonObject payload = Json.createObjectBuilder().add("noteDescription", "edited").add("noteId", noteId.toString()).build();
        final Envelope edited = privateEvent("listing.events.listing-note-edited", payload);

        final Envelope published = handle(edited);

        assertThat(published.name()).isEqualTo("public.listing.note-edited");
        assertThat(published.payload()).isEqualTo(payload);
        assertThatMetadataIsCarriedOver(edited, published);
    }

    @Test
    void deletedNoteIsRepublishedWithTheSamePayload() {
        final JsonObject payload = Json.createObjectBuilder().add("noteId", noteId.toString()).build();

        final Envelope published = handle(privateEvent("listing.events.deleted-listing-note", payload));

        assertThat(published.name()).isEqualTo("public.listing.deleted-listing-note");
        assertThat(published.payload()).isEqualTo(payload);
    }

    @Test
    void thePublicEventIdIsStableForARedeliveredPrivateEvent() {
        final Envelope deleted = privateEvent("listing.events.deleted-listing-note", Json.createObjectBuilder().add("noteId", noteId.toString()).build());

        processor.handle(deleted);
        processor.handle(deleted);

        final ArgumentCaptor<Envelope> published = ArgumentCaptor.forClass(Envelope.class);
        verify(outbox, org.mockito.Mockito.times(2)).appendIfAbsent(published.capture());
        assertThat(published.getAllValues().get(0).metadata().id()).isEqualTo(published.getAllValues().get(1).metadata().id());
    }

    @Test
    void onlyPublicNamesCanBeSent() {
        final Envelope event = privateEvent("listing.events.deleted-listing-note", Json.createObjectBuilder().build());

        assertThatThrownBy(() -> new ProcessorSender(outbox).sendPublic(event, "listing.events.x", event.payload()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Envelope handle(final Envelope privateEvent) {
        processor.handle(privateEvent);
        final ArgumentCaptor<Envelope> published = ArgumentCaptor.forClass(Envelope.class);
        verify(outbox).appendIfAbsent(published.capture());
        return published.getValue();
    }

    private Envelope privateEvent(final String name, final JsonObject payload) {
        return new Envelope(Metadata.builder()
                .withId(randomUUID())
                .withName(name)
                .createdAt(ZonedDateTime.parse("2026-11-19T12:00:00.123Z"))
                .withCausation(List.of(commandId))
                .withUserId("user-1")
                .withSessionId("session-1")
                .withClientCorrelationId("client-1")
                .withStreamId(noteId)
                .build(), payload);
    }

    private static void assertThatMetadataIsCarriedOver(final Envelope privateEvent, final Envelope published) {
        final Metadata in = privateEvent.metadata();
        final Metadata out = published.metadata();
        assertThat(out.id()).isNotEqualTo(in.id()).isEqualTo(ProcessorSender.derivedId(in.id(), published.name()));
        assertThat(out.causation()).isEqualTo(in.causation());
        assertThat(out.createdAt()).isEqualTo(in.createdAt());
        assertThat(out.userId()).isEqualTo(in.userId());
        assertThat(out.sessionId()).isEqualTo(in.sessionId());
        assertThat(out.clientCorrelationId()).isEqualTo(in.clientCorrelationId());
        assertThat(out.streamId()).isEqualTo(in.streamId());
    }
}
