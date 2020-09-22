package uk.gov.moj.cpp.listing.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotesRepository extends EntityRepository<Notes, UUID> {

    @Query(value = "select * from listing_notes where id IN :ids ", isNative = true)
    List<Notes> findNotes(@QueryParam("ids") final List<UUID> idList);

    List<Notes> findByCourtRoomIdAndDate(final UUID courtRoomId, final LocalDate date);

    Notes findOptionalById(final UUID noteId);

}
