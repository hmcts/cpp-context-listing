package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CacheRefDataCourtroomRepository {

    @PersistenceContext(unitName = "listing-persistence-unit")
    EntityManager entityManager;

    public CacheRefDataCourtroom findBy(final UUID id) {
        return entityManager.find(CacheRefDataCourtroom.class, id);
    }

    public CacheRefDataCourtroom save(final CacheRefDataCourtroom entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CacheRefDataCourtroom entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public int deleteAll() {
        return entityManager.createQuery("delete from CacheRefDataCourtroom").executeUpdate();
    }
}
