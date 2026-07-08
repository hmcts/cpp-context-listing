package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.query.CaseByDefendant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseByDefendantRepository {

    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String HEARING_DATE = "hearingDate";
    private static final String CASE_IDS = "caseIds";

    @PersistenceContext(unitName = "listing-persistence-unit")
    EntityManager entityManager;

    public List<CaseByDefendant> getCasesByDefendantAndHearingDate(
            final List<UUID> caseIds,
            final List<UUID> defendantIds,
            final LocalDate hearingDate) {

        return entityManager.createNamedQuery(
                        CaseByDefendant.FIND_CASE_BY_DEFENDANT_WITH_CASE_ID, CaseByDefendant.class)
                .setParameter(DEFENDANT_IDS, defendantIds)
                .setParameter(HEARING_DATE, hearingDate)
                .setParameter(CASE_IDS, caseIds)
                .getResultList();
    }
}
