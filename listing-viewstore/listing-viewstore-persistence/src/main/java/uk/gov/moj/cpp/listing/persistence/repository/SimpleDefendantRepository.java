package uk.gov.moj.cpp.listing.persistence.repository;


import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeDefendantId;
import uk.gov.moj.cpp.listing.persistence.entity.SimpleDefendant;

/**
 * Repository for {@link SimpleDefendant}
 */
@Repository
public interface SimpleDefendantRepository extends EntityRepository<SimpleDefendant, CompositeDefendantId>,
        EntityManagerDelegate<SimpleDefendant> {

}
