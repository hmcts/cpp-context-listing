package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.entity.Notes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotesRepositoryTest extends AbstractRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsNotesByIdsAndByCourtRoomAndDate() {
        final UUID courtRoomId = randomUUID();
        final Notes first = notesRepository.save(new Notes(randomUUID(), courtRoomId, DATE, "first"));
        final Notes second = notesRepository.save(new Notes(randomUUID(), courtRoomId, DATE.plusDays(1), "second"));
        notesRepository.save(new Notes(randomUUID(), randomUUID(), DATE, "other room"));
        entityManager.flush();
        entityManager.clear();

        assertThat(notesRepository.findNotes(List.of(first.getId(), second.getId())))
                .extracting(Notes::getNote).containsExactlyInAnyOrder("first", "second");
        assertThat(notesRepository.findByCourtRoomIdAndDate(courtRoomId, DATE))
                .extracting(Notes::getNote).containsExactly("first");
        assertThat(notesRepository.findOptionalById(first.getId()).getNote()).isEqualTo("first");
        assertThat(notesRepository.findOptionalById(randomUUID())).isNull();
        assertThat(notesRepository.findBy(second.getId()).getDate()).isEqualTo(DATE.plusDays(1));
    }

    @Test
    void removeDeletesTheNote() {
        final Notes note = notesRepository.save(new Notes(randomUUID(), randomUUID(), DATE, "to remove"));
        entityManager.flush();

        notesRepository.remove(notesRepository.findBy(note.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThat(notesRepository.findBy(note.getId())).isNull();
    }
}
