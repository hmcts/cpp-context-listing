package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Notes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotesRepository extends JpaRepository<Notes, UUID> {

    @Query(value = "select notes from Notes notes where notes.id IN (:ids) ")
    List<Notes> findNotes(@Param("ids") final List<UUID> idList);

    List<Notes> findByCourtRoomIdAndDate(final UUID courtRoomId, final LocalDate date);

    /** DeltaSpike {@code findOptionalBy...}: the note, or {@code null}. */
    default Notes findOptionalById(final UUID noteId) {
        return findById(noteId).orElse(null);
    }

    /** DeltaSpike {@code findBy(id)} alias: the entity, or {@code null}. */
    default Notes findBy(final UUID id) {
        return findById(id).orElse(null);
    }

    /** DeltaSpike {@code remove(entity)} alias. */
    @Transactional
    default void remove(final Notes notes) {
        delete(notes);
    }
}
