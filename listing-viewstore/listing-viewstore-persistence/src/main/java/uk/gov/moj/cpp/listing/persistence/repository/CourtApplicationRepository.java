package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CourtApplicationRepository {

    @PersistenceContext(unitName = "listing-persistence-unit")
    EntityManager entityManager;

    public List<CourtApplications> findByParentApplicationId(final UUID id) {
        return entityManager.createQuery(
                        "SELECT courtApplications FROM CourtApplications courtApplications WHERE courtApplications.parentApplicationId = :id",
                        CourtApplications.class)
                .setParameter("id", id)
                .getResultList();
    }
}
