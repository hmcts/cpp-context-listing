package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;


@Repository
public interface CourtApplicationRepository extends EntityRepository<CourtApplications, UUID> {

    List<CourtApplications> findByParentApplicationId(UUID id);

}
