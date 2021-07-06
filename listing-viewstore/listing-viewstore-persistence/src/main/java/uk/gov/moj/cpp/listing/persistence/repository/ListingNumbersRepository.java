package uk.gov.moj.cpp.listing.persistence.repository;


import java.util.UUID;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.ListingNumbers;

@Repository
public interface ListingNumbersRepository extends EntityRepository<ListingNumbers, UUID> {

        @Query(value = "INSERT INTO offence_listing_numbers (offence_id, listing_number) VALUES(:id, 1) " +
                "ON CONFLICT (offence_id)  " +
                "DO  UPDATE SET listing_number = offence_listing_numbers.listing_number + 1 RETURNING *", isNative = true)
        ListingNumbers upset(@QueryParam("id") UUID offenceId);
}