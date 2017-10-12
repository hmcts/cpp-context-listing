package uk.gov.moj.cpp.listing.persistence.repository;


import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link Hearing}
 */
@Repository
public interface HearingRepository extends EntityRepository<Hearing, UUID>,
        EntityManagerDelegate<Hearing> {

    /**
     * Find {@link Hearing} by whether hearing has been Allocated
     *
     * @param allocated property of the hearing to retrieve.
     * @return Hearings.
     */
    public abstract List<Hearing> findByAllocatedAndCourtCentreId(final Boolean allocated, final String courtCentreId);


}
