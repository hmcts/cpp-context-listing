package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.entity.query.CaseByDefendant;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-Postgres repository IT — exercises the named NATIVE query
 * (CaseByDefendant.findCaseByDefendantWithCaseId) against docker Postgres. No mocking.
 */
class CaseByDefendantRepositoryIT {

    private static final LocalDate HEARING_DATE = LocalDate.of(2026, 3, 10);

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("listing-test-persistence-unit");

    private CaseByDefendantRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new CaseByDefendantRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldFindCasesByDefendantAndHearingDate() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        persistHearingGraph(entityManager, caseId, "URN-1", defendantId, HEARING_DATE);

        final List<CaseByDefendant> found =
                repository.getCasesByDefendantAndHearingDate(of(caseId), of(defendantId), HEARING_DATE);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getCaseId(), is(caseId));
        assertThat(found.get(0).getUrn(), is("URN-1"));
    }

    @Test
    void shouldReturnEmptyWhenNoCasesMatchDefendantAndHearingDate() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        persistHearingGraph(entityManager, caseId, "URN-2", defendantId, HEARING_DATE);

        // query with a different hearing date -> no match
        final List<CaseByDefendant> found =
                repository.getCasesByDefendantAndHearingDate(of(caseId), of(defendantId), HEARING_DATE.plusDays(7));

        assertThat(found, is(empty()));
    }

    private void persistHearingGraph(final EntityManager entityManager,
                                     final UUID caseId,
                                     final String urn,
                                     final UUID defendantId,
                                     final LocalDate hearingDate) {
        final Hearing hearing = new Hearing(randomUUID(), JsonNodeFactory.instance.objectNode());

        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setCaseReference(urn);

        final ListedCases listedCase = new ListedCases(randomUUID(), caseId, caseIdentifier, null,
                hearing, new HashSet<>(), new HashSet<>(), Boolean.FALSE);
        final Defendant defendant = new Defendant(randomUUID(), defendantId, null, listedCase);
        listedCase.setDefendants(Set.of(defendant));

        final HearingDays hearingDay = new HearingDays(randomUUID(), 1, null, null, randomUUID(),
                hearingDate, randomUUID(), 60, hearing);

        hearing.setListedCases(Set.of(listedCase));
        hearing.setHearingDays(Set.of(hearingDay));

        entityManager.persist(hearing);
    }
}
