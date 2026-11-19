package uk.gov.moj.cpp.listing.common.domain.note;

import static java.util.Collections.emptyList;

import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.NotesRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Listing notes (MBD refactor slice P1a; Spring port P2.3.0; reworked to ADR 015 in P2.3.1).
 * Replaces {@code ListingNoteCommandHandler}, the {@code ListingNote} aggregate,
 * {@code ListingNoteEventListener} and {@code ListingNoteEventProcessor}.
 *
 * <p>Each method follows the ADR 015 command pattern, with no private event in between:
 * <ol>
 *     <li>load the viewstore state: the aggregate's only state was {@code exists}; the equivalent
 *     fact is a {@code listing_notes} row with the note's id. Note ids stay deterministic
 *     ({@link NoteUUIDService}), one note per courtroom and date;</li>
 *     <li>the aggregate guard: a rejected command is logged and changes nothing (no viewstore
 *     write, no public message); it never throws;</li>
 *     <li>the viewstore change the JEE listener made for the would-be private event;</li>
 *     <li>the public message the JEE processor published for that event, with the same name and
 *     payload. The caller writes it to the outbox in the same transaction.</li>
 * </ol>
 */
@Service
public class ListingNoteService {

    public static final String PUBLIC_CREATED_LISTING_NOTE = "public.listing.created-listing-note";
    public static final String PUBLIC_NOTE_EDITED = "public.listing.note-edited";
    public static final String PUBLIC_DELETED_LISTING_NOTE = "public.listing.deleted-listing-note";

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingNoteService.class);

    private final NotesRepository notesRepository;
    private final NoteUUIDService noteUUIDService;

    public ListingNoteService(final NotesRepository notesRepository, final NoteUUIDService noteUUIDService) {
        this.notesRepository = notesRepository;
        this.noteUUIDService = noteUUIDService;
    }

    /**
     * {@code listing.command.create-listing-note}. JEE: {@code listing.events.created-listing-note}
     * ({@code courtRoomId}, {@code hearingDate}, {@code id}, {@code noteDescription}) → listener
     * inserts the note → processor publishes {@code public.listing.created-listing-note} with the
     * fields renamed to {@code id}, {@code date}, {@code courtRoomId}, {@code note}.
     */
    public List<DomainEvent> createNote(final UUID courtRoomId, final String hearingDate, final String noteDescription) {
        final LocalDate date = LocalDate.parse(hearingDate);
        final UUID id = noteUUIDService.getNoteId(courtRoomId, date);

        if (exists(id)) {
            LOGGER.error("Note already exists with noteId {} ", id);
            return emptyList();
        }

        notesRepository.save(new Notes(id, courtRoomId, date, noteDescription));
        return List.of(new DomainEvent(PUBLIC_CREATED_LISTING_NOTE, id, Json.createObjectBuilder()
                .add("id", id.toString())
                .add("date", hearingDate)
                .add("courtRoomId", courtRoomId.toString())
                .add("note", noteDescription)
                .build()));
    }

    /**
     * {@code listing.command.edit-listing-note}. JEE: {@code listing.events.listing-note-edited}
     * → listener updates the note → processor publishes {@code public.listing.note-edited} with the
     * private payload unchanged ({@code noteDescription}, {@code noteId}).
     */
    public List<DomainEvent> editNote(final UUID noteId, final String noteDescription) {
        final Notes notes = notesRepository.findOptionalById(noteId);
        if (notes == null) {
            LOGGER.error("Invalid note id supplied to edit listing note  {} ", noteId);
            return emptyList();
        }

        notes.setNote(noteDescription);
        notesRepository.save(notes);
        return List.of(new DomainEvent(PUBLIC_NOTE_EDITED, noteId, Json.createObjectBuilder()
                .add("noteDescription", noteDescription)
                .add("noteId", noteId.toString())
                .build()));
    }

    /**
     * {@code listing.command.delete-listing-note}. JEE: {@code listing.events.deleted-listing-note}
     * → listener deletes the note → processor publishes {@code public.listing.deleted-listing-note}
     * with the private payload unchanged ({@code noteId}).
     */
    public List<DomainEvent> deleteNote(final UUID noteId) {
        final Notes notes = notesRepository.findOptionalById(noteId);
        if (notes == null) {
            LOGGER.error("Invalid note id supplied to delete listing note  {} ", noteId);
            return emptyList();
        }

        notesRepository.remove(notes);
        return List.of(new DomainEvent(PUBLIC_DELETED_LISTING_NOTE, noteId, Json.createObjectBuilder()
                .add("noteId", noteId.toString())
                .build()));
    }

    private boolean exists(final UUID id) {
        return notesRepository.findOptionalById(id) != null;
    }
}
