package uk.gov.moj.cpp.listing.common.domain.note;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.common.domain.note.ListingNoteService.CREATED_LISTING_NOTE;
import static uk.gov.moj.cpp.listing.common.domain.note.ListingNoteService.DELETED_LISTING_NOTE;
import static uk.gov.moj.cpp.listing.common.domain.note.ListingNoteService.LISTING_NOTE_EDITED;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ports {@code ListingNoteTest} (aggregate guards) and {@code ListingNoteEventListenerTest}
 * (viewstore writes) for slice P1a.
 */
@ExtendWith(MockitoExtension.class)
class ListingNoteServiceTest {

    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final String HEARING_DATE = "2026-11-20";

    @Mock
    private NotesRepository notesRepository;

    private ListingNoteService service;
    private UUID noteId;

    @BeforeEach
    void setUp() {
        final NoteUUIDService noteUUIDService = new NoteUUIDService();
        service = new ListingNoteService(notesRepository, noteUUIDService);
        noteId = noteUUIDService.getNoteId(COURT_ROOM_ID, LocalDate.parse(HEARING_DATE));
    }

    @Test
    void createNoteSavesTheNoteAndRaisesCreatedListingNote() {
        when(notesRepository.findOptionalById(noteId)).thenReturn(null);

        final List<DomainEvent> events = service.createNote(COURT_ROOM_ID, HEARING_DATE, "note text");

        final ArgumentCaptor<Notes> saved = ArgumentCaptor.forClass(Notes.class);
        verify(notesRepository).save(saved.capture());
        assertThat(saved.getValue(), is(new Notes(noteId, COURT_ROOM_ID, LocalDate.parse(HEARING_DATE), "note text")));
        assertThat(saved.getValue().getNote(), is("note text"));
        assertThat(saved.getValue().getCourtRoomId(), is(COURT_ROOM_ID));

        assertThat(events, contains(new DomainEvent(CREATED_LISTING_NOTE, noteId,
                new CreatedListingNote(COURT_ROOM_ID, HEARING_DATE, noteId, "note text"))));
    }

    @Test
    void createNoteIsRejectedWhenANoteAlreadyExistsForTheCourtroomAndDate() {
        when(notesRepository.findOptionalById(noteId)).thenReturn(existingNote());

        final List<DomainEvent> events = service.createNote(COURT_ROOM_ID, HEARING_DATE, "second note");

        assertThat(events, is(empty()));
        verify(notesRepository, never()).save(any());
    }

    @Test
    void editNoteUpdatesTheDescriptionAndRaisesListingNoteEdited() {
        final Notes existing = existingNote();
        when(notesRepository.findOptionalById(noteId)).thenReturn(existing);

        final List<DomainEvent> events = service.editNote(noteId, "edited");

        verify(notesRepository).save(existing);
        assertThat(existing.getNote(), is("edited"));
        assertThat(events, contains(new DomainEvent(LISTING_NOTE_EDITED, noteId, new ListingNoteEdited("edited", noteId))));
    }

    @Test
    void editNoteIsRejectedWhenTheNoteDoesNotExist() {
        when(notesRepository.findOptionalById(noteId)).thenReturn(null);

        final List<DomainEvent> events = service.editNote(noteId, "edited");

        assertThat(events, is(empty()));
        verify(notesRepository, never()).save(any());
    }

    @Test
    void deleteNoteRemovesTheNoteAndRaisesDeletedListingNote() {
        final Notes existing = existingNote();
        when(notesRepository.findOptionalById(noteId)).thenReturn(existing);

        final List<DomainEvent> events = service.deleteNote(noteId);

        verify(notesRepository).remove(existing);
        assertThat(events, contains(new DomainEvent(DELETED_LISTING_NOTE, noteId, new DeletedListingNote(noteId))));
    }

    @Test
    void deleteNoteIsRejectedWhenTheNoteDoesNotExist() {
        when(notesRepository.findOptionalById(noteId)).thenReturn(null);

        final List<DomainEvent> events = service.deleteNote(noteId);

        assertThat(events, is(empty()));
        verify(notesRepository, never()).remove(any());
    }

    private Notes existingNote() {
        return new Notes(noteId, COURT_ROOM_ID, LocalDate.parse(HEARING_DATE), "original");
    }
}
