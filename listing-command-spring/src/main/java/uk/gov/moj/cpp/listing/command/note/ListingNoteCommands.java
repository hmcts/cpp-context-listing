package uk.gov.moj.cpp.listing.command.note;

import uk.gov.moj.cpp.listing.command.recorder.EventRecorder;
import uk.gov.moj.cpp.listing.common.domain.note.ListingNoteService;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.UUID;

import jakarta.json.JsonObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The listing-note commands (MBD refactor P2.3.0; port of the Phase 1 JEE adapter
 * {@code ListingNoteCommands}). One transaction per command (D5: synchronous in the request):
 * {@link ListingNoteService} changes {@code listing_notes} and returns the private events, which
 * {@link EventRecorder} records in the same transaction (the processor republishes them as public
 * events after commit).
 *
 * <p>As in the legacy code, a rejected command (note already exists / unknown note id) is logged by
 * the service and raises no events; the request is still accepted (202).
 */
@Service
public class ListingNoteCommands {

    public static final String CREATE_LISTING_NOTE = "listing.command.create-listing-note";
    public static final String EDIT_LISTING_NOTE = "listing.command.edit-listing-note";
    public static final String DELETE_LISTING_NOTE = "listing.command.delete-listing-note";

    private final ListingNoteService listingNoteService;
    private final EventRecorder eventRecorder;

    public ListingNoteCommands(final ListingNoteService listingNoteService, final EventRecorder eventRecorder) {
        this.listingNoteService = listingNoteService;
        this.eventRecorder = eventRecorder;
    }

    @Transactional
    public void createNote(final Metadata command, final JsonObject payload) {
        eventRecorder.record(command, listingNoteService.createNote(
                UUID.fromString(payload.getString("courtRoomId")),
                payload.getString("hearingDate"),
                payload.getString("noteDescription")));
    }

    @Transactional
    public void editNote(final Metadata command, final JsonObject payload) {
        eventRecorder.record(command, listingNoteService.editNote(
                UUID.fromString(payload.getString("noteId")),
                payload.getString("noteDescription")));
    }

    @Transactional
    public void deleteNote(final Metadata command, final JsonObject payload) {
        eventRecorder.record(command, listingNoteService.deleteNote(UUID.fromString(payload.getString("noteId"))));
    }
}
