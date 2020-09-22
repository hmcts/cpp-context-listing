package uk.gov.moj.cpp.listing.command.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.listing.events.DeletedListingNote;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.domain.aggregate.ListingNote;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@ServiceComponent(COMMAND_HANDLER)
public class ListingNoteCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingNoteCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private NoteUUIDService noteUUIDService;

    @Handles("listing.command.create-listing-note")
    public void handleCreateNote(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        final CreatedListingNote command =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), CreatedListingNote.class);
        final UUID id = noteUUIDService.getNoteId(command.getCourtRoomId(), LocalDate.parse(command.getHearingDate()));
        final EventStream eventStream = eventSource.getStreamById(id);
        final ListingNote listingNotes = aggregateService.get(eventStream, ListingNote.class);
        final Stream<Object> events = listingNotes.createNote(id, command.getCourtRoomId(), command.getHearingDate(),
                command.getNoteDescription());
        appendEventsToStream(jsonEnvelope, eventStream, events);
    }

    @Handles("listing.command.handler.edit-listing-note")
    public void handleEditNote(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final String noteId = payload.getString("noteId");
        final String noteDescription = payload.getString("noteDescription");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.edit-listing-note' received with payload {}",
                    jsonEnvelope.toObfuscatedDebugString());
        }
        final UUID streamId = UUID.fromString(noteId);
        final EventStream eventStream = eventSource.getStreamById(streamId);
        final ListingNote note = aggregateService.get(eventStream, ListingNote.class);
        final Stream<Object> events = note.editNote(streamId, noteDescription);
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @Handles("listing.command.handler.delete-listing-note")
    public void handleDeleteNote(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        final DeletedListingNote command =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DeletedListingNote.class);
        final UUID noteId = command.getNoteId();
        final EventStream eventStream = eventSource.getStreamById(noteId);
        final ListingNote listingNote = aggregateService.get(eventStream, ListingNote.class);
        final Stream<Object> events = listingNote.deleteNote(noteId);
        appendEventsToStream(jsonEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
