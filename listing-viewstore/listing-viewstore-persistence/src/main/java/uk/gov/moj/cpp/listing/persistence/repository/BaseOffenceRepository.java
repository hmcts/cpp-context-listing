package uk.gov.moj.cpp.listing.persistence.repository;


import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.BaseOffence;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeOffenceId;

/**
 * Repository for {@link BaseOffence}
 */
@Repository
public interface BaseOffenceRepository extends EntityRepository<BaseOffence, CompositeOffenceId>,
        EntityManagerDelegate<BaseOffence> {

    void removeById(CompositeOffenceId id);

}


