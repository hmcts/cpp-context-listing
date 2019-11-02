package uk.gov.moj.cpp.listing.steps.data;


import java.util.List;
import java.util.UUID;

public class ListedCaseData {

    private final UUID caseId;
    private final String authorityCode;
    private final UUID authorityId;
    private final String caseReference;
    private final List<DefendantData> defendants;
    private final Boolean restrictFromCourtList;
    private final Boolean isEjected;
    private final List<CaseMarkerData> caseMarkers;

    public ListedCaseData(UUID caseId, UUID authorityId, String authorityCode, String caseReference, List<DefendantData> defendantData, final Boolean restrictFromCourtList, final Boolean isEjected, final List<CaseMarkerData> caseMarkers) {
                  this.caseId = caseId;
                  this.authorityCode = authorityCode;
                  this.authorityId = authorityId;
                  this.caseReference = caseReference;
                  this.defendants = defendantData;

        this.restrictFromCourtList = restrictFromCourtList;
        this.isEjected = isEjected;
        this.caseMarkers = caseMarkers;
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

    public Boolean getEjected() {
        return isEjected;
    }

    public List<DefendantData> getDefendants() {
        return defendants;
    }

    public Boolean getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public List<CaseMarkerData> getCaseMarkers() {
        return caseMarkers;
    }
}
