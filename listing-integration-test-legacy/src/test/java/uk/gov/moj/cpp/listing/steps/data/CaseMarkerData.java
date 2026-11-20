package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class CaseMarkerData {

    private UUID id;
    private UUID caseMarkerTypeId;
    private String caseMarkerCode;
    private String caseMarkerDescription;

    public CaseMarkerData(final UUID id, final UUID caseMarkerTypeId, final String caseMarkerCode, final String caseMarkerDescription) {
        this.id = id;
        this.caseMarkerTypeId = caseMarkerTypeId;
        this.caseMarkerCode = caseMarkerCode;
        this.caseMarkerDescription = caseMarkerDescription;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseMarkerTypeId() {
        return caseMarkerTypeId;
    }

    public String getCaseMarkerCode() {
        return caseMarkerCode;
    }

    public String getCaseMarkerDescription() {
        return caseMarkerDescription;
    }
}
