package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class CaseAndDefendantData {

    final UUID hearingId;

    final String caseUrnQueryParam;

    final String caseUrn;

    final UUID masterDefendantId;

    final String searchCriteria;

    final String jurisdictionTypeQueryParam;

    final String jurisdictionType;

    public CaseAndDefendantData(UUID hearingId, String caseUrnQueryParam, String caseUrn, UUID masterDefendantId, String searchCriteria, String jurisdictionTypeQueryParam, String jurisdictionType) {
        this.hearingId = hearingId;
        this.caseUrnQueryParam = caseUrnQueryParam;
        this.caseUrn = caseUrn;
        this.masterDefendantId = masterDefendantId;
        this.searchCriteria = searchCriteria;
        this.jurisdictionTypeQueryParam = jurisdictionTypeQueryParam;
        this.jurisdictionType = jurisdictionType;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getCaseUrnQueryParam() {
        return caseUrnQueryParam;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public String getJurisdictionTypeQueryParam() {
        return jurisdictionTypeQueryParam;
    }

    public String getJurisdictionType() {
        return jurisdictionType;
    }
}
