package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CacheRefDataCourtroomRepository extends JpaRepository<CacheRefDataCourtroom, UUID> {

    /** Bulk JPQL delete, as the DeltaSpike repository did (not Spring Data's load-then-delete). */
    @Override
    @Transactional
    @Modifying
    @Query("delete from CacheRefDataCourtroom")
    void deleteAll();

    /** DeltaSpike {@code findBy(id)} alias: the entity, or {@code null}. */
    default CacheRefDataCourtroom findBy(final UUID id) {
        return findById(id).orElse(null);
    }
}
