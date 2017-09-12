package uk.gov.moj.cpp.listing.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.CourtRoom;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourtRoomRepository extends EntityRepository<CourtRoom, UUID> {
    public List<CourtRoom> findByCourtCentre(final String courtCentre);
}
