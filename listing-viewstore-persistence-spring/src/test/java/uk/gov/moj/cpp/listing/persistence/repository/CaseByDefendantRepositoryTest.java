package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.moj.cpp.listing.persistence.repository.ViewstoreFixtures.hearing;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.entity.query.CaseByDefendant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CaseByDefendantRepositoryTest extends AbstractRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);

    @Autowired
    private CaseByDefendantRepository caseByDefendantRepository;

    @Autowired
    private EntityManager entityManager;

    private static UUID defendantIdOf(final Hearing hearing) {
        final ListedCases listedCase = hearing.getListedCases().iterator().next();
        return listedCase.getDefendants().iterator().next().getDefendantId();
    }

    @Test
    void findsTheCaseByHearingDayDate() {
        final UUID caseId = randomUUID();
        final Hearing allocated = hearing(randomUUID(), randomUUID(), DATE, true, "URNDAY", caseId, randomUUID());
        entityManager.persist(allocated);
        entityManager.flush();

        final List<CaseByDefendant> found = caseByDefendantRepository.getCasesByDefendantAndHearingDate(
                List.of(caseId), List.of(defendantIdOf(allocated)), DATE);

        assertThat(found).extracting(CaseByDefendant::getCaseId).containsExactly(caseId);
        assertThat(found).extracting(CaseByDefendant::getUrn).containsExactly("URNDAY");
        assertThat(caseByDefendantRepository.getCasesByDefendantAndHearingDate(
                List.of(caseId), List.of(defendantIdOf(allocated)), DATE.plusDays(1))).isEmpty();
    }

    @Test
    void findsTheCaseOfAnUnallocatedHearingByWeekCommencing() {
        final UUID caseId = randomUUID();
        final Hearing unallocated = hearing(randomUUID(), null, DATE.minusDays(30), false, "URNWC", caseId, randomUUID());
        unallocated.getHearingDays().clear();
        unallocated.setWeekCommencingStartDate(DATE.minusDays(1));
        unallocated.setWeekCommencingEndDate(DATE.plusDays(5));
        entityManager.persist(unallocated);
        entityManager.flush();

        final List<CaseByDefendant> found = caseByDefendantRepository.getCasesByDefendantAndHearingDate(
                List.of(caseId), List.of(defendantIdOf(unallocated)), DATE);

        assertThat(found).extracting(CaseByDefendant::getCaseId).containsExactly(caseId);
        assertThat(caseByDefendantRepository.getCasesByDefendantAndHearingDate(
                List.of(caseId), List.of(randomUUID()), DATE)).isEmpty();
    }
}
