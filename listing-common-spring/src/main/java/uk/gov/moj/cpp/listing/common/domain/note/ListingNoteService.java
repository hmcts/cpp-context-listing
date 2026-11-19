package uk.gov.moj.cpp.listing.common.domain.note;

import static java.util.Collections.emptyList;

import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.listing.events.DeletedListingNote;
import uk.gov.justice.listing.events.ListingNoteEdited;
import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.NotesRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listing notes (MBD refactor slice P1a; Spring port P2.3.0, unchanged apart from the DI annotations). Replaces {@code ListingNoteCommandHandler}, the
 * {@code ListingNote} aggregate and {@code ListingNoteEventListener}.
 *
 * <p>The aggregate's only state was {@code exists}; the equivalent fact is a {@code listing_notes}
 * row with the note's id. Note ids stay deterministic ({@link NoteUUIDService}), so there is one
 * note per courtroom and date, and the query side can still find notes by recomputing the id.
 *
 * <p>As in the aggregate, a rejected command is logged and produces no events; it never throws.
 * Callers must run each method inside a transaction and write the returned events to the outbox
 * in that same transaction.
 */
@Service
public class ListingNoteService {

    public static final String CREATED_LISTING_NOTE = "listing.events.created-listing-note";
    public static final String LISTING_NOTE_EDITED = "listing.events.listing-note-edited";
    public static final String DELETED_LISTING_NOTE = "listing.events.deleted-listing-note";

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingNoteService.class);

    private final NotesRepository notesRepository;
    private final NoteUUIDService noteUUIDService;


    public ListingNoteService(final NotesRepository notesRepository, final NoteUUIDService noteUUIDService) {
        this.notesRepository = notesRepository;
        this.noteUUIDService = noteUUIDService;
    }

    public List<DomainEvent> createNote(final UUID courtRoomId, final String hearingDate, final String noteDescription) {
        final LocalDate date = LocalDate.parse(hearingDate);
        final UUID id = noteUUIDService.getNoteId(courtRoomId, date);

        if (exists(id)) {
            LOGGER.error("Note already exists with noteId {} ", id);
            return emptyList();
        }

        notesRepository.save(new Notes(id, courtRoomId, date, noteDescription));
        return List.of(new DomainEvent(CREATED_LISTING_NOTE, id,
                new CreatedListingNote(courtRoomId, hearingDate, id, noteDescription)));
    }

    public List<DomainEvent> editNote(final UUID noteId, final String noteDescription) {
        final Notes notes = notesRepository.findOptionalById(noteId);
        if (notes == null) {
            LOGGER.error("Invalid note id supplied to edit listing note  {} ", noteId);
            return emptyList();
        }

        notes.setNote(noteDescription);
        notesRepository.save(notes);
        return List.of(new DomainEvent(LISTING_NOTE_EDITED, noteId, new ListingNoteEdited(noteDescription, noteId)));
    }

    public List<DomainEvent> deleteNote(final UUID noteId) {
        final Notes notes = notesRepository.findOptionalById(noteId);
        if (notes == null) {
            LOGGER.error("Invalid note id supplied to delete listing note  {} ", noteId);
            return emptyList();
        }

        notesRepository.remove(notes);
        return List.of(new DomainEvent(DELETED_LISTING_NOTE, noteId, new DeletedListingNote(noteId)));
    }

    private boolean exists(final UUID id) {
        return notesRepository.findOptionalById(id) != null;
    }
}
