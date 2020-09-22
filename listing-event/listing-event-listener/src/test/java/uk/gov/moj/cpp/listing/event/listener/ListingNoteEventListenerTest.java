package uk.gov.moj.cpp.listing.event.listener;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.listing.events.DeletedListingNote;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.NotesRepository;
import uk.gov.justice.listing.events.ListingNoteEdited;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

@RunWith(MockitoJUnitRunner.class)
public class ListingNoteEventListenerTest {
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final LocalDate HEARING_DATE = now();
    private static final UUID NOTE_ID = randomUUID();

    @Mock
    private NotesRepository notesRepository;

    @Mock
    private Envelope<CreatedListingNote> envelope;

    @Mock
    private Envelope<DeletedListingNote> deletedListingNoteEnvelope;

    @InjectMocks
    private ListingNoteEventListener listingNoteEventListener;

    private final ArgumentCaptor<Notes> noteArgumentCaptor = ArgumentCaptor.forClass(Notes.class);

    private final Notes notes = new Notes();

    @Test
    public void shouldCreateNote() {
        final UUID courtRoomId = randomUUID();
        final LocalDate date = now();
        final UUID id = randomUUID();
        final CreatedListingNote noteCreatedForListing = new CreatedListingNote(courtRoomId, date.toString(), id, "noteDescription");
        when(notesRepository.findByCourtRoomIdAndDate(courtRoomId, date)).thenReturn(emptyList());
        when(envelope.payload()).thenReturn(noteCreatedForListing);
        listingNoteEventListener.handleCreateListingNote(envelope);
        verify(notesRepository, times(1)).save(any(Notes.class));
    }

    @Test
    public void shouldNotCreateNote() {
        createNote();
        listingNoteEventListener.handleCreateListingNote(envelope);
        verify(notesRepository, times(0)).save(any(Notes.class));
    }

    @Test
    public void shouldDeleteNote() {
        createNote();
        final DeletedListingNote deletedListingNote = new DeletedListingNote(NOTE_ID);
        when(deletedListingNoteEnvelope.payload()).thenReturn(deletedListingNote);
        when(notesRepository.findBy(any(UUID.class))).thenReturn(getNotes());
        listingNoteEventListener.handleDeleteListingNote(deletedListingNoteEnvelope);
        verify(notesRepository, times(1)).remove(any(Notes.class));
    }

    @Test
    public void shouldNotDeleteNote() {
        final DeletedListingNote deletedListingNote = new DeletedListingNote(NOTE_ID);
        when(deletedListingNoteEnvelope.payload()).thenReturn(deletedListingNote);
        listingNoteEventListener.handleDeleteListingNote(deletedListingNoteEnvelope);
        verify(notesRepository, times(0)).remove(any(Notes.class));
    }

    private void createNote() {
        final UUID courtRoomId = randomUUID();
        final LocalDate date = now();
        final UUID id = randomUUID();
        final CreatedListingNote noteCreatedForListing = new CreatedListingNote(courtRoomId, date.toString(), id, "noteDescription");
        getNotes();
        when(notesRepository.findByCourtRoomIdAndDate(courtRoomId, date)).thenReturn(asList(notes));
        when(envelope.payload()).thenReturn(noteCreatedForListing);
    }

    private Notes getNotes() {
        notes.setId(NOTE_ID);
        notes.setCourtRoomId(COURT_ROOM_ID);
        notes.setNote("desc");
        notes.setDate(HEARING_DATE);
        return notes;
    }



    @Test
    public void shouldHandleListingNoteEditedEvent() {

        //Given
        UUID noteId = UUID.randomUUID();
        final ListingNoteEdited noteEditedEventData = ListingNoteEdited
                .listingNoteEdited()
                .withNoteId(noteId)
                .withNoteDescription("Note Edited")
                .build();

        final Envelope<ListingNoteEdited> noteEditedEventRequestedEnvelope =
                envelopeFrom(metadataWithDefaults(), noteEditedEventData);

        Notes optionalNote = new Notes(noteId,
                UUID.randomUUID(), LocalDate.now(),
                "existing description");
        when(notesRepository.findOptionalById(noteId)).thenReturn(optionalNote);

        //When
        listingNoteEventListener.handleListingNoteEditedEvent(noteEditedEventRequestedEnvelope);

        //Then
        verify(notesRepository, times(1)).findOptionalById(noteId);
        verify(notesRepository, times(1)).save(noteArgumentCaptor.capture());

        assertThat(noteArgumentCaptor.getValue().getNote(), is("Note Edited"));

    }
}
