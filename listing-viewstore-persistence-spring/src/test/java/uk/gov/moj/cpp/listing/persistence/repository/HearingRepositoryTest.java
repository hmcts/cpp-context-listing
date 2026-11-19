package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.moj.cpp.listing.persistence.repository.ViewstoreFixtures.application;
import static uk.gov.moj.cpp.listing.persistence.repository.ViewstoreFixtures.hearing;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link HearingRepository} queries used by the query API, run against Postgres with the
 * production Liquibase schema (the native SQL is carried over unchanged from DeltaSpike).
 */
class HearingRepositoryTest extends AbstractRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);

    @Autowired
    private HearingRepository hearingRepository;

    @Autowired
    private CourtApplicationRepository courtApplicationRepository;

    @Autowired
    private CacheRefDataCourtroomRepository cacheRefDataCourtroomRepository;

    @Autowired
    private EntityManager entityManager;

    private final UUID courtCentreId = randomUUID();
    private final UUID courtRoomId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID masterDefendantId = randomUUID();
    private Hearing allocated;
    private Hearing unallocated;

    /**
     * New hearings are persisted, not {@code save}d: Spring Data's {@code save} merges entities
     * with an assigned id, and loading a {@link Hearing} by id selects its read-only "virtual"
     * columns (totalCount, hearing_date, ...) that only the native queries return.
     */
    private Hearing persist(final Hearing hearing) {
        entityManager.persist(hearing);
        return hearing;
    }

    @BeforeEach
    void seed() {
        allocated = persist(hearing(courtCentreId, courtRoomId, DATE, true, "URN1", caseId, masterDefendantId));
        unallocated = hearing(courtCentreId, null, DATE, false, "URN2", randomUUID(), randomUUID());
        unallocated.setWeekCommencingStartDate(DATE.minusDays(1));
        unallocated.setWeekCommencingEndDate(DATE.plusDays(5));
        unallocated = persist(unallocated);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByReadsTheHearingWithItsJsonbProperties() {
        final Hearing found = hearingRepository.findBy(allocated.getId());

        assertThat(found).isNotNull();
        assertThat(found.getTotalCount()).isEqualTo(1L);
        assertThat(found.getProperties().get("id").asText()).isEqualTo(allocated.getId().toString());
        assertThat(found.getProperties().get("allocated").asBoolean()).isTrue();
        assertThat(found.getListedCases()).hasSize(1);
        assertThat(found.getHearingDays()).hasSize(1);
        assertThat(hearingRepository.findBy(randomUUID())).isNull();
    }

    @Test
    void findHearingsForSearchDateMatchesTheHearingDayTimeWindow() {
        final List<Hearing> found = hearingRepository.findHearings(true, courtCentreId.toString(), null, null, null, null,
                DATE, ZonedDateTime.of(DATE.atTime(9, 0), ZoneOffset.UTC), ZonedDateTime.of(DATE.atTime(12, 0), ZoneOffset.UTC));
        final List<Hearing> outsideWindow = hearingRepository.findHearings(true, courtCentreId.toString(), null, null, null, null,
                DATE, ZonedDateTime.of(DATE.atTime(12, 0), ZoneOffset.UTC), ZonedDateTime.of(DATE.atTime(13, 0), ZoneOffset.UTC));

        assertThat(found).extracting(Hearing::getId).containsExactly(allocated.getId());
        assertThat(outsideWindow).isEmpty();
    }

    @Test
    void jdbcRangeSearchFiltersOnlyOnTheSuppliedCriteria() {
        final List<Hearing> found = hearingRepository.findHearings(true, courtCentreId, null, null, null, "CROWN",
                DATE.minusDays(1), DATE.plusDays(1), 0, 10);
        final List<Hearing> otherCentre = hearingRepository.findHearings(true, randomUUID(), null, null, null, null,
                DATE.minusDays(1), DATE.plusDays(1), 0, 10);

        assertThat(found).extracting(Hearing::getId).containsExactly(allocated.getId());
        assertThat(found.get(0).getTotalCount()).isEqualTo(1L);
        assertThat(otherCentre).isEmpty();
    }

    @Test
    void rangeSearchWithPossibleDisqualificationBindsNullFilters() {
        final List<Hearing> found = hearingRepository.findHearings("true", courtCentreId.toString(), null, null, null, null,
                DATE.minusDays(1), DATE.plusDays(1), false, 0, 10);

        assertThat(found).extracting(Hearing::getId).containsExactly(allocated.getId());
    }

    @Test
    void weekCommencingSearchesFindTheUnallocatedHearing() {
        final List<Hearing> byRange = hearingRepository.findHearingsByWeekCommencingRange(courtCentreId.toString(), null, null, null, null,
                DATE.minusDays(2), DATE.plusDays(2), 0, 10);
        final List<Hearing> noPaging = hearingRepository.findHearingsByWeekCommencingRangeWithNoPagination(courtCentreId.toString(), null, null, null, null,
                DATE.minusDays(2), DATE.plusDays(2));
        final List<Hearing> jdbc = hearingRepository.findUnallocatedHearingsByWeekCommencingRange(courtCentreId, null, null, null, null,
                DATE.minusDays(2), DATE.plusDays(2), false, 0, 10);
        final List<Hearing> disqualification = hearingRepository.findUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification(
                courtCentreId.toString(), null, null, null, null, DATE.minusDays(2), DATE.plusDays(2), false, false, 0, 10);

        assertThat(byRange).extracting(Hearing::getId).contains(unallocated.getId());
        assertThat(noPaging).extracting(Hearing::getId).contains(unallocated.getId());
        assertThat(jdbc).extracting(Hearing::getId).containsExactly(unallocated.getId());
        assertThat(disqualification).extracting(Hearing::getId).containsExactly(unallocated.getId());
    }

    @Test
    void unscheduledSearchesByCaseUrnAndCourtCentres() {
        final Hearing unscheduled = hearing(courtCentreId, null, DATE, false, "UNSCHED1", randomUUID(), randomUUID());
        unscheduled.setUnscheduled(true);
        persist(unscheduled);
        entityManager.flush();

        assertThat(hearingRepository.findHearings("UNSCHED1", null, 0, 10)).extracting(Hearing::getId).containsExactly(unscheduled.getId());
        assertThat(hearingRepository.findHearings(null, null, Set.of(courtCentreId.toString()), 0, 10)).extracting(Hearing::getId).containsExactly(unscheduled.getId());
        assertThat(hearingRepository.findHearings("OTHER", null, Set.of(courtCentreId.toString()), 0, 10)).isEmpty();
    }

    @Test
    void anyAllocationSearchByCaseUrnOrApplicationReference() {
        assertThat(hearingRepository.findHearingsByCaseUrnAndAnyAllocationState("URN1", DATE.minusDays(1)))
                .extracting(Hearing::getId).containsExactly(allocated.getId());
        assertThat(hearingRepository.findHearingsByCaseUrnAndAnyAllocationState("URN1", DATE.plusDays(1))).isEmpty();
    }

    @Test
    void allocatedAndUnallocatedByCaseAndApplication() {
        final UUID applicationId = randomUUID();
        final Hearing withApplication = hearingRepository.findBy(allocated.getId());
        entityManager.persist(application(withApplication, applicationId, null, "APP1"));
        entityManager.flush();

        assertThat(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(caseId.toString()))
                .extracting(Hearing::getId).containsExactly(allocated.getId());
        assertThat(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(null, applicationId.toString()))
                .extracting(Hearing::getId).containsExactly(allocated.getId());
        assertThat(courtApplicationRepository.findByParentApplicationId(randomUUID())).isEmpty();
    }

    @Test
    void courtListQueriesAggregateTheHearingsOfTheDay() {
        final Hearing publicList = hearingRepository.findHearingsForPublicStandardList(true, courtCentreId.toString(), DATE, DATE, Set.of("none"));
        // Both queries return a synthetic row with the same fixed id; clear the persistence context so
        // the second query is not answered from the first one's managed instance.
        entityManager.clear();
        final List<Hearing> alphabetical = hearingRepository.findHearingsForAlphabeticalList(true, courtCentreId.toString(), DATE, Set.of("none"));

        assertThat(publicList.getProperties().get("hearings").get(0).get("hearingsByCourtCentreId").get(0)
                .get("hearingsByHearingDate").get(0).get("hearing").get("id").asText()).isEqualTo(allocated.getId().toString());
        assertThat(publicList.getProperties().get("judiciary")).hasSize(1);
        assertThat(alphabetical).hasSize(1);
        assertThat(alphabetical.get(0).getProperties().get(0).get("hearingsByHearingDate")).hasSize(1);
    }

    @Test
    void cotrSearchByHearingTypes() {
        final Hearing found = hearingRepository.findBy(allocated.getId());

        assertThat(hearingRepository.findHearingsForCotr(Set.of(found.getTypeId().toString()), courtCentreId.toString(), DATE, DATE))
                .extracting(Hearing::getId).containsExactly(allocated.getId());
    }

    @Test
    void availableSearchByCaseUrnAndMasterDefendant() {
        final Set<String> crownAndMags = Set.of("CROWN", "MAGISTRATES");
        assertThat(hearingRepository.findHearings(true, crownAndMags, null, Set.of("URN1"), Set.of(""), Set.of(""), null, DATE.minusDays(1)))
                .extracting(Hearing::getId).containsExactly(allocated.getId());
        assertThat(hearingRepository.findHearings(crownAndMags, null, Set.of(""), Set.of(masterDefendantId), Set.of(""), null, DATE.minusDays(1)))
                .extracting(Hearing::getId).containsExactly(allocated.getId());
    }

    @Test
    void courtCalendarSearchReturnsOneRowPerHearingDay() {
        cacheRefDataCourtroomRepository.saveAndFlush(new CacheRefDataCourtroom(courtRoomId, "Court 1"));

        final List<Hearing> found = hearingRepository.findAllocatedHearingsForCourtCalendar(courtCentreId, courtRoomId, null, null, null,
                DATE, DATE, null, 0, 10);
        final List<Hearing> exactStart = hearingRepository.findAllocatedHearingsForCourtCalendar(courtCentreId, courtRoomId, null, null, null,
                DATE, DATE, Instant.parse("2026-03-10T10:00:00Z"), 0, 10);

        assertThat(found).extracting(Hearing::getId).containsExactly(allocated.getId());
        assertThat(found.get(0).getHearingDayCount()).isEqualTo(1L);
        assertThat(found.get(0).getHearingDate()).isEqualTo(DATE);
        assertThat(exactStart).hasSize(1);
    }

    @Test
    void findsByIdsAndListedCaseIds() {
        assertThat(hearingRepository.findAllCourtSchedulerHearingByIds(List.of(allocated.getId(), unallocated.getId()))).hasSize(2);
        assertThat(hearingRepository.findHearingsByListedCaseIds(List.of(caseId))).extracting(Hearing::getId).containsExactly(allocated.getId());
    }

    @Test
    void removeDeletesTheManagedHearingAndItsChildRows() {
        final Hearing found = hearingRepository.findBy(allocated.getId());

        hearingRepository.remove(found);
        entityManager.flush();
        entityManager.clear();

        assertThat(hearingRepository.findBy(allocated.getId())).isNull();
        assertThat(hearingRepository.findBy(unallocated.getId())).isNotNull();
        assertThat(entityManager.createNativeQuery("select count(*) from listed_cases where hearing_id = cast(:id as uuid)")
                .setParameter("id", allocated.getId().toString()).getSingleResult()).isEqualTo(0L);
    }
}
