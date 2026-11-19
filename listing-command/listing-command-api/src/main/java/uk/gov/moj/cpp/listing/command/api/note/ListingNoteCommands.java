package uk.gov.moj.cpp.listing.command.api.note;

import static javax.ejb.TransactionAttributeType.REQUIRED;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.outbox.EventOutboxWriter;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.common.domain.note.ListingNoteService;

import java.util.List;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.json.JsonObject;

/**
 * JEE adapter for slice P1a (listing notes): unpacks the command envelope, calls
 * {@link ListingNoteService} and writes its events to the outbox, all in one transaction.
 * Replaces the hop through {@code listing.handler.command} and the event store.
 *
 * <p>A {@code @Stateless} EJB with a container-managed transaction, not a CDI
 * {@code @Transactional} bean: the DeltaSpike repositories use {@code BeanManagedUserTransactionStrategy}
 * ({@code apache-deltaspike.properties}), and WildFly forbids {@code UserTransaction} access inside
 * {@code @Transactional} methods (WFTXN0084). This matches how the framework's MDBs run the event
 * listeners today.
 */
@Stateless
@TransactionAttribute(REQUIRED)
public class ListingNoteCommands {

    @Inject
    private ListingNoteService listingNoteService;

    @Inject
    private EventOutboxWriter eventOutboxWriter;

    public void createNote(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        record(command, listingNoteService.createNote(
                UUID.fromString(payload.getString("courtRoomId")),
                payload.getString("hearingDate"),
                payload.getString("noteDescription")));
    }

    public void editNote(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        record(command, listingNoteService.editNote(
                UUID.fromString(payload.getString("noteId")),
                payload.getString("noteDescription")));
    }

    public void deleteNote(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        record(command, listingNoteService.deleteNote(UUID.fromString(payload.getString("noteId"))));
    }

    private void record(final JsonEnvelope command, final List<DomainEvent> events) {
        eventOutboxWriter.write(command.metadata(), events);
    }
}
