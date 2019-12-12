package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface PublishedCourtListRepository
        extends EntityRepository<PublishedCourtList, PublishedCourtListPrimaryKey>,
        EntityManagerDelegate<PublishedCourtList> {

}
