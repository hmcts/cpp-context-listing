package uk.gov.moj.cpp.listing.persistence.repository;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;


@RunWith(CdiTestRunner.class)
public class NotesRepositoryTest extends BaseTransactionalTest {

    @Inject
    NotesRepository notesRepository;


    @Test
    public void shouldFindNotesId() {

        List<Notes> expectedNotes = IntStream.range(0, 2).mapToObj(i -> new Notes(randomUUID(), randomUUID(), LocalDate.now(), STRING.next())).
                peek(note -> notesRepository.save(note)).
                collect(Collectors.toList());

        List<Notes> actualNotes = notesRepository.findNotes(expectedNotes.stream().map(note -> note.getId()).collect(Collectors.toList()));

        assertTrue(EqualsBuilder.reflectionEquals(expectedNotes, actualNotes));

    }

    @Test
    public void shouldFindNoteByCourtRoomAndDate() {
        final Notes notes = new Notes();
        final LocalDate date = LocalDate.now();
        notes.setDate(date);
        notes.setNote("Note description");
        notes.setId(randomUUID());
        final UUID courtRoomId = randomUUID();
        notes.setCourtRoomId(courtRoomId);
        notesRepository.save(notes);

        final List<Notes> byCourtRoomCourtCentreAndDate = notesRepository.findByCourtRoomIdAndDate(courtRoomId, date);
        assertThat(byCourtRoomCourtCentreAndDate.size(), is(1));
    }

    @Test
    public void shouldNotFindNoteByCourtRoomCourtCentreAndDate() {
        final List<Notes> byCourtRoomCourtCentreAndDate = notesRepository.findByCourtRoomIdAndDate(randomUUID(), LocalDate.now());
        assertThat(byCourtRoomCourtCentreAndDate.size(), is(0));
    }


    @Test
    public void shouldFindNoteById() {

        //Given
        UUID noteId = randomUUID();
        String noteDescription = "random note description";
        Notes note = createNoteObject(noteId, LocalDate.now(), randomUUID(), noteDescription);
        notesRepository.save(note);

        //When
        Notes optionalById = notesRepository.findOptionalById(noteId);

        //Then
        assertThat(optionalById.getId(), is(noteId));
        assertThat(optionalById.getNote(), is("random note description"));
    }

    @Test
    public void shouldlUpdateNoteDescription() {

        //Given
        UUID noteId = randomUUID();
        String noteDescription = "random note description";
        Notes note = createNoteObject(noteId, LocalDate.now(), randomUUID(), noteDescription);
        notesRepository.save(note);

        //When
        Notes optionalById = notesRepository.findOptionalById(noteId);
        optionalById.setNote("edited note description");
        notesRepository.save(note);
        Notes noteAfterChangingDescription = notesRepository.findOptionalById(noteId);

        //Then
        assertThat(noteAfterChangingDescription.getNote(), is("edited note description"));
        List<Notes> allNotes = notesRepository.findAll();
        assertThat(allNotes.size(), is(1));

    }

    private Notes createNoteObject(UUID noteId, LocalDate now, UUID courtCentreId,
                                   String noteDescription) {
        return new Notes(noteId, courtCentreId, now, noteDescription);
    }


}


