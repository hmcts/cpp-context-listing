package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefdataCourtroom;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CacheRefdataCourtroomRepository extends EntityRepository<CacheRefdataCourtroom, UUID> {

    @Modifying
    @Query("delete from CacheRefdataCourtroom")
    int deleteAll();
}
