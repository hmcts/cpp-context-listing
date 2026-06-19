package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.query.CaseByDefendant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.QueryParam;

public abstract class CaseByDefendantRepository extends AbstractEntityRepository<CaseByDefendant, UUID> {

    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String HEARING_DATE = "hearingDate";
    private static final String CASE_IDS = "caseIds";

    public List<CaseByDefendant> getCasesByDefendantAndHearingDate(
            @QueryParam(CASE_IDS) final List<UUID> caseIds,
            @QueryParam(DEFENDANT_IDS) final List<UUID> defendantIds,
            @QueryParam(HEARING_DATE) final LocalDate hearingDate) {

            return this.entityManager().createNamedQuery(
                    CaseByDefendant.FIND_CASE_BY_DEFENDANT_WITH_CASE_ID, CaseByDefendant.class)
                    .setParameter(DEFENDANT_IDS, defendantIds)
                    .setParameter(HEARING_DATE, hearingDate)
                    .setParameter(CASE_IDS, caseIds)
                    .getResultList();
    }
}