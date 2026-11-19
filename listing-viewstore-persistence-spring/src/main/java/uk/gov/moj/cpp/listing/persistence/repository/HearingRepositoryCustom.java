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
