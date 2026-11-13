package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CacheRefDataCourtroomRepository extends EntityRepository<CacheRefDataCourtroom, UUID>,
        EntityManagerDelegate<CacheRefDataCourtroom> {

    @Modifying
    @Query("delete from CacheRefDataCourtroom")
    void deleteAll();
}
