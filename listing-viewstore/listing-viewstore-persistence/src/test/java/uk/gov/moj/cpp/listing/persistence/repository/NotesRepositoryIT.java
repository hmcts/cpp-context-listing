package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import uk.gov.moj.cpp.listing.persistence.entity.Notes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-Postgres repository IT (docker Postgres, throwaway DB). No mocking, no embedded container.
 */
class NotesRepositoryIT {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("listing-test-persistence-unit");

    private NotesRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new NotesRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndRetrieveANoteById() {
        final UUID id = randomUUID();
        repository.save(new Notes(id, randomUUID(), LocalDate.of(2026, 1, 15), "a note"));

        final Notes found = repository.findBy(id);

        assertThat(found, is(notNullValue()));
        assertThat(found.getId(), is(id));
        assertThat(found.getNote(), is("a note"));
    }

    @Test
    void shouldReturnNullFromFindOptionalByIdWhenNoNoteExists() {
        assertThat(repository.findOptionalById(randomUUID()), is(nullValue()));
    }

    @Test
    void shouldFindNotesByIds() {
        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        repository.save(new Notes(id1, randomUUID(), LocalDate.of(2026, 1, 15), "note1"));
        repository.save(new Notes(id2, randomUUID(), LocalDate.of(2026, 1, 16), "note2"));
        repository.save(new Notes(randomUUID(), randomUUID(), LocalDate.of(2026, 1, 17), "note3"));

        final List<Notes> found = repository.findNotes(of(id1, id2));

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(Notes::getId).toList(), containsInAnyOrder(id1, id2));
    }

    @Test
    void shouldFindNotesByCourtRoomIdAndDate() {
        final UUID courtRoomId = randomUUID();
        final LocalDate date = LocalDate.of(2026, 2, 1);
        // notes has a unique constraint (pk_notes_range) of one note per court-room + date
        repository.save(new Notes(randomUUID(), courtRoomId, date, "match"));
        repository.save(new Notes(randomUUID(), courtRoomId, date.plusDays(1), "wrong date"));
        repository.save(new Notes(randomUUID(), randomUUID(), date, "wrong courtroom"));

        final List<Notes> found = repository.findByCourtRoomIdAndDate(courtRoomId, date);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getNote(), is("match"));
    }

    @Test
    void shouldFindAllNotes() {
        repository.save(new Notes(randomUUID(), randomUUID(), LocalDate.of(2026, 1, 15), "n1"));
        repository.save(new Notes(randomUUID(), randomUUID(), LocalDate.of(2026, 1, 16), "n2"));

        assertThat(repository.findAll(), hasSize(2));
    }

    @Test
    void shouldRemoveANote() {
        final UUID id = randomUUID();
        repository.save(new Notes(id, randomUUID(), LocalDate.of(2026, 1, 15), "to remove"));

        repository.remove(repository.findBy(id));

        assertThat(repository.findOptionalById(id), is(nullValue()));
    }

    @Test
    void shouldReturnEmptyFromFindAllWhenNoNotesExist() {
        assertThat(repository.findAll(), is(empty()));
    }
}
