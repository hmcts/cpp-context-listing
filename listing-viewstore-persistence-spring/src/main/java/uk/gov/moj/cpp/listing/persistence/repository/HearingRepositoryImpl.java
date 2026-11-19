package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.HearingJdbcRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data fragment for {@link HearingRepositoryCustom}. Read-only by default (§11 pitfall 1:
 * custom fragments are not transactional on their own, and read-write transactions on reads are
 * what slowed courtscheduler's ITs down).
 */
@Transactional(readOnly = true)
public class HearingRepositoryImpl implements HearingRepositoryCustom {

    private final HearingJdbcRepository hearingJdbcRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public HearingRepositoryImpl(final HearingJdbcRepository hearingJdbcRepository) {
        this.hearingJdbcRepository = hearingJdbcRepository;
    }

    @Override
    @Transactional
    public <S extends Hearing> S save(final S hearing) {
        if (!entityManager.contains(hearing)) {
            entityManager.persist(hearing);
        }
        return hearing;
    }

    @Transactional
    public void remove(final Hearing hearing) {
        entityManager.remove(hearing);
    }

    @Override
    public List<Hearing> findHearings(final boolean allocated, final UUID courtCentreId, final UUID courtRoomId,
                                      final UUID authorityCode, final UUID hearingTypeId, final String jurisdictionType,
                                      final LocalDate startDate, final LocalDate endDate,
                                      final Integer offSet, final Integer pageSize) {
        return hearingJdbcRepository.findHearings(allocated, courtCentreId, courtRoomId,
                authorityCode, hearingTypeId, jurisdictionType, startDate, endDate, offSet, pageSize);
    }

    @Override
    public List<Hearing> findUnallocatedHearingsByWeekCommencingRange(final UUID courtCentreId, final UUID courtRoomId,
                                                                      final UUID authorityCode, final UUID hearingTypeId,
                                                                      final String jurisdictionType,
                                                                      final LocalDate weekCommencingStartDate,
                                                                      final LocalDate weekCommencingEndDate,
                                                                      final boolean allocated,
                                                                      final Integer offSet, final Integer pageSize) {
        return hearingJdbcRepository.findUnallocatedHearingsByWeekCommencingRange(courtCentreId, courtRoomId, authorityCode,
                hearingTypeId, jurisdictionType, weekCommencingStartDate, weekCommencingEndDate, allocated, offSet, pageSize);
    }

    @Override
    public List<Hearing> findAllocatedHearingsForCourtCalendar(final UUID courtCentreId, final UUID courtRoomId,
                                                               final UUID authorityCode, final UUID hearingTypeId,
                                                               final String jurisdictionType,
                                                               final LocalDate startDate, final LocalDate endDate,
                                                               final Instant exactHearingStartDateTime,
                                                               final Integer offSet, final Integer pageSize) {
        return hearingJdbcRepository.findAllocatedHearingsForCourtCalendar(courtCentreId, courtRoomId,
                authorityCode, hearingTypeId, jurisdictionType, startDate, endDate, exactHearingStartDateTime, offSet, pageSize);
    }
}
