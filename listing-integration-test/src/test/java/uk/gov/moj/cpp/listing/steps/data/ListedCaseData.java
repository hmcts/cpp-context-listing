package uk.gov.moj.cpp.listing.steps.data;


import java.util.List;
import java.util.UUID;

public class ListedCaseData {

    private final UUID caseId;
    private final String authorityCode;
    private final UUID authorityId;
    private final String caseReference;
    private final List<DefendantData> defendants;

    public ListedCaseData(UUID caseId, UUID authorityId, String authorityCode, String caseReference, List<DefendantData> defendantData) {
                  this.caseId = caseId;
                  this.authorityCode = authorityCode;
                  this.authorityId = authorityId;
                  this.caseReference = caseReference;
                  this.defendants = defendantData;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getAuthorityCode() {
        return authorityCode;
    }

    public UUID getAuthorityId() {
        return authorityId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public List<DefendantData> getDefendants() {
        return defendants;
    }
}
