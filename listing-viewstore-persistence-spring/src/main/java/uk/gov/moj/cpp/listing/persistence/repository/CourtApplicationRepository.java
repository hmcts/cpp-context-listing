package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtApplicationRepository extends JpaRepository<CourtApplications, UUID> {

    List<CourtApplications> findByParentApplicationId(UUID id);

    /** DeltaSpike {@code findBy(id)} alias: the entity, or {@code null}. */
    default CourtApplications findBy(final UUID id) {
        return findById(id).orElse(null);
    }
}
