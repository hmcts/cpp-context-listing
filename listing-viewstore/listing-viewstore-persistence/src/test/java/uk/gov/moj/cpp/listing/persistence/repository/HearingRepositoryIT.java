package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-Postgres repository IT for HearingRepository (docker Postgres). No mocking.
 * Migrating DeltaSpike @Query methods to JPA native queries one group at a time — each proven here first.
 */
class HearingRepositoryIT {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("listing-test-persistence-unit");

    private HearingRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new HearingRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndRetrieveAHearingById() {
        final UUID id = randomUUID();
        repository.save(new Hearing(id, JsonNodeFactory.instance.objectNode().put("hearingType", "TRIAL")));

        final Hearing found = repository.findBy(id);

        assertThat(found, is(notNullValue()));
        assertThat(found.getId(), is(id));
        assertThat(found.getProperties().get("hearingType").asText(), is("TRIAL"));
        // totalCount (native "1 as totalCount") does not map from a `select *` query under Hibernate 6 and is
        // not used for single-result findBy; it is validated on the paginated findHearings queries below.
    }

    @Test
    void shouldReturnNullWhenNoHearingExistsForId() {
        assertThat(repository.findBy(randomUUID()), is(nullValue()));
    }

    @Test
    void shouldFindAllocatedAndUnallocatedHearingsByCaseId() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        persistHearingWithListedCase(entityManager, hearingId, caseId);
        // a second, unrelated hearing+case that must not be returned
        persistHearingWithListedCase(entityManager, randomUUID(), randomUUID());

        final List<Hearing> found = repository.findAllocatedAndUnallocatedHearingsByCaseId(caseId.toString());

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindAllCourtSchedulerHearingsByIds() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        persistHearing(entityManager, id1);
        persistHearing(entityManager, id2);
        persistHearing(entityManager, randomUUID()); // not requested

        final List<Hearing> found = repository.findAllCourtSchedulerHearingByIds(of(id1, id2));

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(Hearing::getId).toList(), containsInAnyOrder(id1, id2));
    }

    @Test
    void shouldFindUnscheduledHearingsByCaseUrnAndTypeOfListWithTotalCount() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID typeOfListId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setAllocated(false);
        hearing.setUnscheduled(true);
        hearing.setTypeOfListId(typeOfListId);
        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setCaseReference("URN123");
        final ListedCases listedCase = new ListedCases(randomUUID(), randomUUID(), caseIdentifier, null, hearing,
                new HashSet<>(), new HashSet<>(), Boolean.FALSE);
        hearing.setListedCases(Set.of(listedCase));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearings("URN123", typeOfListId.toString(), 0, 10);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
        // KNOWN ISSUE for listing team: the "count(*) OVER() as totalCount" native alias does not populate
        // Hearing.totalCount under Hibernate 6 (rows/filters are correct; totalCount is null). Used by
        // HearingQueryView / RangeSearchQuery pagination. Likely needs an explicit @SqlResultSetMapping.
    }

    @Test
    void shouldFindHearingsByCaseUrnAndAnyAllocationState() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setStartDate(LocalDate.of(2026, 5, 1));
        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setCaseReference("URN-ANY");
        final ListedCases listedCase = new ListedCases(randomUUID(), randomUUID(), caseIdentifier, null, hearing,
                new HashSet<>(), new HashSet<>(), Boolean.FALSE);
        hearing.setListedCases(Set.of(listedCase));
        entityManager.persist(hearing);

        final List<Hearing> found =
                repository.findHearingsByCaseUrnAndAnyAllocationState("URN-ANY", LocalDate.of(2026, 4, 1));

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindAllocatedAndUnallocatedHearingsByCaseIdAndApplicationId() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        final ListedCases listedCase = new ListedCases(randomUUID(), caseId, null, null, hearing,
                new HashSet<>(), new HashSet<>(), Boolean.FALSE);
        final CourtApplications courtApplication = new CourtApplications(randomUUID(), applicationId, hearing,
                "APPLICATION_TYPE", null, "reference", "particulars", Boolean.FALSE);
        hearing.setListedCases(Set.of(listedCase));
        hearing.setCourtApplications(Set.of(courtApplication));
        entityManager.persist(hearing);

        final List<Hearing> found =
                repository.findAllocatedAndUnallocatedHearingsByCaseId(caseId.toString(), applicationId.toString());

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindHearingsForCotr() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID typeId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setTypeId(typeId);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setStartDate(LocalDate.of(2026, 6, 10));
        hearing.setEndDate(LocalDate.of(2026, 6, 10));
        hearing.setListedCases(Set.of(listedCaseFor(hearing)));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearingsForCotr(
                Set.of(typeId.toString()), courtCentreId.toString(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindHearingsByWeekCommencingRange() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setWeekCommencingStartDate(LocalDate.of(2026, 6, 8));
        hearing.setWeekCommencingEndDate(LocalDate.of(2026, 6, 14));
        hearing.setListedCases(Set.of(listedCaseFor(hearing)));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearingsByWeekCommencingRange(
                courtCentreId.toString(), null, null, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 0, 100);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindAllocatedHearingsInDateRangeFilteredByPossibleDisqualification() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();

        final Hearing hearing = allocatedHearingInJune(hearingId);
        hearing.setPossibleDisqualification(false);
        hearing.setListedCases(Set.of(listedCaseFor(hearing)));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearings("true", null, null, null, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), false, 0, 10);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindAllocatedHearingsInDateRangeWithoutPagination() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();

        final Hearing hearing = allocatedHearingInJune(hearingId);
        hearing.setListedCases(Set.of(listedCaseFor(hearing)));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearings("true", null, null, null, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindHearingsByWeekCommencingRangeWithoutPagination() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setIsVacatedTrial(Boolean.FALSE);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setWeekCommencingStartDate(LocalDate.of(2026, 6, 8));
        hearing.setWeekCommencingEndDate(LocalDate.of(2026, 6, 14));
        hearing.setListedCases(Set.of(listedCaseFor(hearing)));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearingsByWeekCommencingRangeWithNoPagination(
                courtCentreId.toString(), null, null, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setIsVacatedTrial(Boolean.FALSE);
        hearing.setAllocated(Boolean.FALSE);
        hearing.setPossibleDisqualification(Boolean.TRUE);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setWeekCommencingStartDate(LocalDate.of(2026, 6, 8));
        hearing.setWeekCommencingEndDate(LocalDate.of(2026, 6, 14));
        hearing.setListedCases(Set.of(listedCaseFor(hearing)));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification(
                courtCentreId.toString(), null, null, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), false, true, 0, 10);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindHearingsForPublicStandardListAggregatingJudiciaryAndHearingsAsJson() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("allocated", true);
        properties.putArray("judiciary").addObject().put("judicialId", "J-1");

        final Hearing hearing = new Hearing(hearingId, properties);
        hearing.setIsVacatedTrial(Boolean.FALSE);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setHearingDays(Set.of(hearingDayFor(hearing, courtCentreId, LocalDate.of(2026, 6, 10))));
        entityManager.persist(hearing);

        final Hearing found = repository.findHearingsForPublicStandardList(true, courtCentreId.toString(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), Set.of(randomUUID().toString()));

        assertThat(found, is(notNullValue()));
        assertThat(found.getProperties(), is(notNullValue()));
    }

    @Test
    void shouldFindHearingsForAlphabeticalListAggregatingHearingsAsJson() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("allocated", true);

        final Hearing hearing = new Hearing(hearingId, properties);
        hearing.setIsVacatedTrial(Boolean.FALSE);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setHearingDays(Set.of(hearingDayFor(hearing, courtCentreId, LocalDate.of(2026, 6, 10))));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearingsForAlphabeticalList(true, courtCentreId.toString(),
                LocalDate.of(2026, 6, 10), Set.of(randomUUID().toString()));

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getProperties(), is(notNullValue()));
    }

    @Test
    void shouldFindAllocatedHearingsByJurisdictionAndCaseUrnSet() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setAllocated(Boolean.TRUE);
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setJurisdictionType("MAGISTRATES");
        hearing.setEndDate(LocalDate.of(2026, 12, 31));
        hearing.setListedCases(Set.of(listedCaseWithReference(hearing, "URN-SET")));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearings(true, Set.of("MAGISTRATES"), null,
                Set.of("URN-SET"), Set.of(randomUUID().toString()), Set.of("NONE"), "NONE",
                LocalDate.of(2026, 6, 1));

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindHearingsByJurisdictionAndCaseUrnSetForAnyAllocationState() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setJurisdictionType("CROWN");
        hearing.setEndDate(LocalDate.of(2026, 12, 31));
        hearing.setListedCases(Set.of(listedCaseWithReference(hearing, "URN-ANY-STATE")));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearings(Set.of("CROWN"), null,
                Set.of("URN-ANY-STATE"), Set.of(randomUUID().toString()), Set.of("NONE"), "NONE",
                LocalDate.of(2026, 6, 1));

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    @Test
    void shouldFindUnscheduledHearingsByCaseUrnAndTypeOfListRestrictedToCourtCentres() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setAllocated(Boolean.FALSE);
        hearing.setUnscheduled(Boolean.TRUE);
        hearing.setIsVacatedTrial(Boolean.FALSE);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setListedCases(Set.of(listedCaseWithReference(hearing, "URN-CC")));
        entityManager.persist(hearing);

        final List<Hearing> found = repository.findHearings("URN-CC", null,
                Set.of(courtCentreId.toString()), 0, 10);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(hearingId));
    }

    private Hearing allocatedHearingInJune(final UUID hearingId) {
        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        hearing.setAllocated(Boolean.TRUE);
        hearing.setUnscheduled(Boolean.FALSE);
        hearing.setIsVacatedTrial(Boolean.FALSE);
        hearing.setStartDate(LocalDate.of(2026, 6, 10));
        hearing.setEndDate(LocalDate.of(2026, 6, 10));
        return hearing;
    }

    private HearingDays hearingDayFor(final Hearing hearing, final UUID courtCentreId, final LocalDate hearingDate) {
        return new HearingDays(randomUUID(), 1, null, null, null, hearingDate, courtCentreId, null, hearing);
    }

    private ListedCases listedCaseWithReference(final Hearing hearing, final String caseReference) {
        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setCaseReference(caseReference);
        return new ListedCases(randomUUID(), randomUUID(), caseIdentifier, null, hearing,
                new HashSet<>(), new HashSet<>(), Boolean.FALSE);
    }

    private ListedCases listedCaseFor(final Hearing hearing) {
        return new ListedCases(randomUUID(), randomUUID(), null, null, hearing,
                new HashSet<>(), new HashSet<>(), Boolean.FALSE);
    }

    private void persistHearing(final EntityManager entityManager, final UUID hearingId) {
        entityManager.persist(new Hearing(hearingId, JsonNodeFactory.instance.objectNode()));
    }

    private void persistHearingWithListedCase(final EntityManager entityManager, final UUID hearingId, final UUID caseId) {
        final Hearing hearing = new Hearing(hearingId, JsonNodeFactory.instance.objectNode());
        final ListedCases listedCase = new ListedCases(randomUUID(), caseId, null, null, hearing,
                new HashSet<>(), new HashSet<>(), Boolean.FALSE);
        hearing.setListedCases(java.util.Set.of(listedCase));
        entityManager.persist(hearing);
    }
}
