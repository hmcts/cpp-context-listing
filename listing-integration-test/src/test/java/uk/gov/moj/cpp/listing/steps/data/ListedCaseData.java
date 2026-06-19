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
    private final String caseStatus;
    private final UUID groupId;
    private final Boolean isCivil;
    private final Boolean isGroupMember;
    private final Boolean isGroupMaster;

    public ListedCaseData(UUID caseId, UUID authorityId, String authorityCode, String caseReference, List<DefendantData> defendantData, final Boolean restrictFromCourtList, final Boolean isEjected, final List<CaseMarkerData> caseMarkers, final String caseStatus,
                          final UUID groupId, final Boolean isCivil, final Boolean isGroupMember, final Boolean isGroupMaster) {
        this.caseId = caseId;
        this.authorityCode = authorityCode;
        this.authorityId = authorityId;
        this.caseReference = caseReference;
        this.defendants = defendantData;

        this.restrictFromCourtList = restrictFromCourtList;
        this.isEjected = isEjected;
        this.caseMarkers = caseMarkers;
        this.caseStatus = caseStatus;

        this.groupId = groupId;
        this.isCivil = isCivil;
        this.isGroupMaster = isGroupMaster;
        this.isGroupMember = isGroupMember;
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

    public String getCaseStatus() {
        return caseStatus;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public Boolean getCivil() {
        return isCivil;
    }

    public Boolean getGroupMember() {
        return isGroupMember;
    }

    public Boolean getGroupMaster() {
        return isGroupMaster;
    }
}
