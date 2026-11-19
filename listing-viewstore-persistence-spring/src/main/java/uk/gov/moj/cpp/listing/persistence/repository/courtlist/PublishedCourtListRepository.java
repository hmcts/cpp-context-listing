package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublishedCourtListRepository extends JpaRepository<PublishedCourtList, PublishedCourtListPrimaryKey> {

    /** DeltaSpike {@code findBy(id)} alias: the entity, or {@code null}. */
    default PublishedCourtList findBy(final PublishedCourtListPrimaryKey key) {
        return findById(key).orElse(null);
    }
}
