package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.listing.events.DeletedListingNote;
import uk.gov.justice.listing.events.ListingNoteEdited;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingNoteTest {

    @InjectMocks
    private ListingNote note;

    private UUID id = UUID.randomUUID();

    @Test
    public void shouldRaiseListingNoteCreatedEvent() {
        //given
        final UUID id = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final String date = LocalDate.now().toString();
        final String noteDescription = "noteDescription";
        final CreatedListingNote noteCreatedForListing = new CreatedListingNote(courtRoomId, date, id, noteDescription);

        //when
        final Stream<Object> objectStream = this.note.createNote(id, courtRoomId, date, noteDescription);

        //then
        final List<Object> events = objectStream.collect(toList());
        assertThat(events.size(), is(1));
        assertThat(events, hasItem(noteCreatedForListing));
    }


    @Test
    public void shouldRaiseListingNoteEditedEvent() {

        //given
        final UUID id = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final String date = LocalDate.now().toString();
        final String noteDescription = "noteDescription";
        this.note.createNote(id, courtRoomId, date, noteDescription);
        String editedNoteDescription = "edited note description";
        ListingNoteEdited listingNoteEdited = new ListingNoteEdited(editedNoteDescription, id);

        //when
        Stream<Object> objectStream = this.note.editNote(id, editedNoteDescription);

        //then
        final List<Object> events = objectStream.collect(toList());
        assertThat(events.size(), is(1));
        assertThat(events, hasItem(listingNoteEdited));
    }

    @Test
    public void shouldRaiseListingNoteDeletedEvent() {
        shouldRaiseListingNoteEditedEvent();
        final Stream<Object> objectStream = this.note.deleteNote(id);
        final List<Object> events = objectStream.collect(toList());
        assertThat(events.size(), is(1));
        final DeletedListingNote deletedListingNote = new DeletedListingNote(id);
        assertThat(events, hasItem(deletedListingNote));
    }
}