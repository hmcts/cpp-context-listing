package uk.gov.moj.cpp.listing.persistence.repository;


import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
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
    List<Hearing> findByAllocatedAndCourtCentreId(final Boolean allocated, final UUID courtCentreId);

    /**
     * Update {@link Hearing} with type
     *
     * @param type property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query(value = "update Hearing as h SET h.type = ?1 WHERE h.id = ?2")
    int updateType(String type, UUID hearingId);

    /**
     * Update {@link Hearing} with courtRoomId
     *
     * @param courtRoomId property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query(value = "update Hearing as h SET h.courtRoomId = ?1 WHERE h.id = ?2")
    int updateCourtRoomId(UUID courtRoomId , UUID hearingId);


    /**
     * Update {@link Hearing} with judgeId
     *
     * @param judgeId property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query(value = "update Hearing as h SET h.judgeId = ?1 WHERE h.id = ?2")
    int updateJudgeId(UUID judgeId , UUID hearingId);

    /**
     * Update {@link Hearing} with estimateMinutes
     *
     * @param estimateMinutes property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query(value = "update Hearing as h SET h.estimateMinutes = ?1 WHERE h.id = ?2")
    int updateEstimateMinutes(Integer estimateMinutes, UUID hearingId);

    /**
     * Update {@link Hearing} with startDate
     *
     * @param startDate property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query(value = "update Hearing as h SET h.startDate = ?1 WHERE h.id = ?2")
    int updateStartDate(LocalDate startDate , UUID hearingId);

    /**
     * Update {@link Hearing} with startTime
     *
     * @param startTime property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query(value = "update Hearing as h SET h.startTime = ?1 WHERE h.id = ?2")
    int updateStartTime(LocalTime startTime , UUID hearingId);

    /**
     * Update {@link Hearing} with allocated
     *
     * @param allocated property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query( value = "update Hearing as h SET h.allocated = ?1 WHERE h.id = ?2")
    int updateAllocated(boolean allocated , UUID hearingId);

    /**
     * Update {@link Hearing} with notBefore
     *
     * @param notBefore property of the hearing to update.
     * @param hearingId property of the hearing to update.
     * @return number of rows updated.
     */
    @Modifying
    @Query( value = "update Hearing as h SET h.notBefore = ?1 WHERE h.id = ?2")
    int updateNotBefore(boolean notBefore , UUID hearingId);
}
