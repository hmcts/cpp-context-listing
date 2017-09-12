package uk.gov.moj.cpp.listing.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.Judge;

import java.util.UUID;

@Repository
public interface JudgeRepository extends EntityRepository<Judge, UUID> {

}
