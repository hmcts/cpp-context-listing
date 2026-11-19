package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The {@link HearingRepository} methods that the DeltaSpike repository implemented by hand
 * (delegating to {@link uk.gov.moj.cpp.listing.persistence.repository.courtlist.HearingJdbcRepository}
 * because their WHERE clause depends on which filters are set). Their {@code @Query} annotations
 * in the DeltaSpike class were never used (DeltaSpike only proxies abstract methods) and are not
 * carried over.
 */
public interface HearingRepositoryCustom {

    /**
     * DeltaSpike {@code remove(entity)}: {@code EntityManager.remove} of a managed hearing (the JEE
     * event listener's hearing deletes, ported in P2.3.2). Not Spring Data's {@code delete}, which
     * first re-reads the entity by id: a by-id load selects the read-only columns only the native
     * queries return.
     */
    void remove(Hearing hearing);

    /**
     * DeltaSpike {@code save(entity)} as the JEE event listener used it (P2.3.2): a managed hearing
     * is kept as it is (its changes are flushed with the transaction), a new one is persisted. Not
     * Spring Data's {@code save}, whose merge of an entity with an assigned id first re-reads it by
     * id, and a by-id load selects the read-only columns only the native queries return.
     */
    <S extends Hearing> S save(S hearing);

    List<Hearing> findHearings(boolean allocated,
                               UUID courtCentreId,
                               UUID courtRoomId,
                               UUID authorityCode,
                               UUID hearingTypeId,
                               String jurisdictionType,
                               LocalDate startDate,
                               LocalDate endDate,
                               Integer offSet,
                               Integer pageSize);

    List<Hearing> findUnallocatedHearingsByWeekCommencingRange(UUID courtCentreId,
                                                               UUID courtRoomId,
                                                               UUID authorityCode,
                                                               UUID hearingTypeId,
                                                               String jurisdictionType,
                                                               LocalDate weekCommencingStartDate,
                                                               LocalDate weekCommencingEndDate,
                                                               boolean allocated,
                                                               Integer offSet,
                                                               Integer pageSize);

    List<Hearing> findAllocatedHearingsForCourtCalendar(UUID courtCentreId,
                                                        UUID courtRoomId,
                                                        UUID authorityCode,
                                                        UUID hearingTypeId,
                                                        String jurisdictionType,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Instant exactHearingStartDateTime,
                                                        Integer offSet,
                                                        Integer pageSize);
}
