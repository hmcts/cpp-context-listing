package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Notes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class NotesRepository {

    @PersistenceContext(unitName = "listing-persistence-unit")
    EntityManager entityManager;

    public Notes save(final Notes notes) {
        return entityManager.merge(notes);
    }

    public Notes findBy(final UUID id) {
        return entityManager.find(Notes.class, id);
    }

    public Notes findOptionalById(final UUID noteId) {
        return entityManager.find(Notes.class, noteId);
    }

    public List<Notes> findAll() {
        return entityManager.createQuery("SELECT notes FROM Notes notes", Notes.class)
                .getResultList();
    }

    public void remove(final Notes notes) {
        entityManager.remove(entityManager.contains(notes) ? notes : entityManager.merge(notes));
    }

    public void flush() {
        entityManager.flush();
    }

    public List<Notes> findNotes(final List<UUID> idList) {
        return entityManager.createQuery(
                        "select notes from Notes notes where notes.id IN (:ids) ", Notes.class)
                .setParameter("ids", idList)
                .getResultList();
    }

    public List<Notes> findByCourtRoomIdAndDate(final UUID courtRoomId, final LocalDate date) {
        return entityManager.createQuery(
                        "SELECT notes FROM Notes notes WHERE notes.courtRoomId = :courtRoomId AND notes.date = :date",
                        Notes.class)
                .setParameter("courtRoomId", courtRoomId)
                .setParameter("date", date)
                .getResultList();
    }
}
