package uk.gov.moj.cpp.listing.persistence.repository;


import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ListingCase}
 */
@Repository
public interface ListingCaseRepository extends EntityRepository<ListingCase, UUID>,
        EntityManagerDelegate<ListingCase> {

}
