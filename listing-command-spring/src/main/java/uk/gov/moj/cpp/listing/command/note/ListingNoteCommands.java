package uk.gov.moj.cpp.listing.command.note;

import uk.gov.moj.cpp.listing.common.domain.note.ListingNoteService;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;

import java.util.UUID;

import jakarta.json.JsonObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The listing-note commands (MBD refactor P2.3.0; ADR 015 since P2.3.1). One transaction per
 * command (D5: synchronous in the request): {@link ListingNoteService} checks the rule against
 * {@code listing_notes}, changes it and returns the public message the JEE processor published,
 * which goes to the outbox in the same transaction. No private event exists.
 *
 * <p>As in the legacy code, a rejected command (note already exists / unknown note id) is logged by
 * the service and changes nothing; the request is still accepted (202).
 */
@Service
public class ListingNoteCommands {

    public static final String CREATE_LISTING_NOTE = "listing.command.create-listing-note";
    public static final String EDIT_LISTING_NOTE = "listing.command.edit-listing-note";
    public static final String DELETE_LISTING_NOTE = "listing.command.delete-listing-note";

    private final ListingNoteService listingNoteService;
    private final EventOutbox eventOutbox;

    public ListingNoteCommands(final ListingNoteService listingNoteService, final EventOutbox eventOutbox) {
        this.listingNoteService = listingNoteService;
        this.eventOutbox = eventOutbox;
    }

    @Transactional
    public void createNote(final Metadata command, final JsonObject payload) {
        eventOutbox.write(command, listingNoteService.createNote(
                UUID.fromString(payload.getString("courtRoomId")),
                payload.getString("hearingDate"),
                payload.getString("noteDescription")));
    }

    @Transactional
    public void editNote(final Metadata command, final JsonObject payload) {
        eventOutbox.write(command, listingNoteService.editNote(
                UUID.fromString(payload.getString("noteId")),
                payload.getString("noteDescription")));
    }

    @Transactional
    public void deleteNote(final Metadata command, final JsonObject payload) {
        eventOutbox.write(command, listingNoteService.deleteNote(UUID.fromString(payload.getString("noteId"))));
    }
}
