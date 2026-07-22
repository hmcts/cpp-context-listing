package uk.gov.moj.cpp.listing.persistence.repository;

import static com.vladmihalcea.hibernate.type.json.internal.JacksonUtil.toJsonNode;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
 * Manual performance test for the tuned findHearings(masterDefendantIdSet) query
 * (HearingRepository#findHearings(Set, String, Set, Set, Set, String, LocalDate)).
 *
 * Builds a SIT-scale dataset: one master defendant with PAST_HEARING_COUNT historical
 * hearing/listed_cases/defendant rows, then times how long the query takes to find them
 * all via the masterDefendantIdSet UNION branch.
 *
 * Ignored by default so it never slows down the regular IT suite - remove @Ignore locally,
 * point at a real Postgres instance, and run it before/after SQL or index changes to compare
 * timings printed to stdout.
 */
@RunWith(CdiTestRunner.class)
@Ignore("Manual performance test - remove @Ignore locally to run against a real DB with bulk data")
public class HearingFindHearingsPerformanceIT extends BaseTransactionalTest {

    private static final int PAST_HEARING_COUNT = 5000;
    private static final int FLUSH_EVERY = 200;

    @Inject
    public HearingRepository hearingRepository;

    @Before
    public void clear() {
        final List<Hearing> all = hearingRepository.findAll();
        all.forEach(hearingRepository::remove);
        hearingRepository.flush();
    }

    @Test
    public void shouldFindHearingsForDefendantWithManyPastHearings() {
        final UUID masterDefendantId = randomUUID();

        for (int i = 0; i < PAST_HEARING_COUNT; i++) {
            hearingRepository.save(pastHearingFor(masterDefendantId));
            if (i % FLUSH_EVERY == 0) {
                hearingRepository.flush();
            }
        }
        hearingRepository.flush();

        final Set<String> jurisdictionTypes = Set.of(CROWN.toString());
        final Set<String> caseUrnSet = Set.of("");
        final Set<UUID> masterDefendantIdSet = Set.of(masterDefendantId);
        final Set<String> linkedCaseUrn = Set.of("");

        final long start = System.nanoTime();

        final List<Hearing> actualHearings = hearingRepository.findHearings(
                jurisdictionTypes,
                null,
                caseUrnSet,
                masterDefendantIdSet,
                linkedCaseUrn,
                null,
                LocalDate.now());

        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        System.out.println("findHearings(masterDefendantIdSet) against " + PAST_HEARING_COUNT
                + " rows took " + elapsedMillis + " ms and returned " + actualHearings.size() + " hearings");

        assertThat(actualHearings.size(), is(PAST_HEARING_COUNT));
    }

    private Hearing pastHearingFor(final UUID masterDefendantId) {
        final Hearing hearing = Hearing.builder()
                .withId(randomUUID())
                .withProperties(toJsonNode("{}"))
                .withTypeId(randomUUID())
                .withCourtCentreId(randomUUID())
                .withCourtRoomId(randomUUID())
                .withStartDate(LocalDate.now().minusDays(30))
                .withEndDate(null)
                .withIsVacatedTrial(false)
                .withAllocated(true)
                .withJurisdictionType(CROWN.toString())
                .withUnscheduled(false)
                .withIsPossibleDisqualification(false)
                .build();

        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setCaseReference(randomUUID().toString());
        caseIdentifier.setAuthorityId(randomUUID());

        final ListedCases listedCase = new ListedCases(randomUUID(), randomUUID(), caseIdentifier, null, hearing, null, null, null);

        final Defendant defendant = new Defendant(randomUUID(), randomUUID(), masterDefendantId, null);
        listedCase.setDefendants(new HashSet<>(Set.of(defendant)));

        hearing.setListedCases(new HashSet<>(Set.of(listedCase)));

        return hearing;
    }
}
