package uk.gov.moj.cpp.listing.persistence.repository;


import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.BaseOffence;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeOffenceId;
import uk.gov.moj.cpp.listing.persistence.entity.OffenceWithDefendantId;

/**
 * Repository for {@link OffenceWithDefendantId}
 */
@Repository
public interface OffenceWithDefendantIdRepository extends EntityRepository<OffenceWithDefendantId, CompositeOffenceId>,
        EntityManagerDelegate<BaseOffence> {

}


