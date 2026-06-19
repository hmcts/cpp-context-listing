package uk.gov.moj.cpp.listing.persistence.entity.query;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

@Entity(name = "CaseByDefendant")
@NamedNativeQueries({
        @NamedNativeQuery(
                name = CaseByDefendant.FIND_CASE_BY_DEFENDANT_WITH_CASE_ID,
                query = CaseByDefendant.QUERY,
                resultClass = CaseByDefendant.class
        )
})

public class CaseByDefendant implements Serializable {

    public static final String FIND_CASE_BY_DEFENDANT_WITH_CASE_ID = "findCaseByDefendantWithCaseId";

    public static final String QUERY = "SELECT DISTINCT lc.case_id AS caseId, lc.case_reference AS urn "+
            "FROM listed_cases lc, hearing h, defendant d , hearing_days hd "+
            "WHERE lc.hearing_id = h.id "+
            "AND d.listed_case_id = lc.id "+
            "AND hd.hearing_id = h.id "+
            "AND d.defendant_id in (:defendantIds) "+
            "AND lc.case_id in (:caseIds) "+
            "AND hd.hearing_date = :hearingDate "+
            "UNION "+
            "SELECT DISTINCT lc.case_id as caseId, lc.case_reference as urn "+
            "FROM listed_cases lc, hearing h, defendant d "+
            "WHERE lc.hearing_id = h.id "+
            "AND d.listed_case_id = lc.id "+
            "AND d.defendant_id in (:defendantIds) "+
            "AND lc.case_id in (:caseIds) "+
            "AND (:hearingDate BETWEEN h.week_commencing_start_date AND h.week_commencing_end_date AND allocated = FALSE)" ;


    @Id
    @Column(name = "caseId", nullable = false)
    private UUID caseId;

    @Column(name = "urn")
    private String urn;


    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(final String urn) {
        this.urn = urn;
    }

    public static Builder caseByDefendant() {
        return new Builder();
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static final class Builder {

        private UUID caseId;
        private String urn;

        public Builder withCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withUrn(String urn) {
            this.urn = urn;
            return this;
        }

        public CaseByDefendant build() {
            final CaseByDefendant caseByDefendant = new CaseByDefendant();
            caseByDefendant.setCaseId(caseId);
            caseByDefendant.setUrn(urn);
            return caseByDefendant;
        }
    }
}
