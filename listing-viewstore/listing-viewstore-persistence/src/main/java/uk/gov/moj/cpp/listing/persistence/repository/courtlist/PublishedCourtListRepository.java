package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class PublishedCourtListRepository {

    @PersistenceContext(unitName = "listing-persistence-unit")
    EntityManager entityManager;

    public PublishedCourtList findBy(final PublishedCourtListPrimaryKey primaryKey) {
        return entityManager.find(PublishedCourtList.class, primaryKey);
    }

    public PublishedCourtList save(final PublishedCourtList publishedCourtList) {
        return entityManager.merge(publishedCourtList);
    }

    public Long count() {
        return entityManager.createQuery(
                        "SELECT COUNT(publishedCourtList) FROM PublishedCourtList publishedCourtList", Long.class)
                .getSingleResult();
    }

    public List<PublishedCourtList> findAll() {
        return entityManager.createQuery(
                        "SELECT publishedCourtList FROM PublishedCourtList publishedCourtList", PublishedCourtList.class)
                .getResultList();
    }

    public void remove(final PublishedCourtList publishedCourtList) {
        entityManager.remove(entityManager.contains(publishedCourtList)
                ? publishedCourtList : entityManager.merge(publishedCourtList));
    }

    public void flush() {
        entityManager.flush();
    }
}
