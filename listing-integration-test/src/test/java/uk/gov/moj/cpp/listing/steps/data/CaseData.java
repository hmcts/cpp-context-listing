package uk.gov.moj.cpp.listing.steps.data;

import java.util.List;
import java.util.UUID;

public class CaseData {

    private final UUID caseId;
    private final String urn;
    private final List<HearingData> hearingData;

    public CaseData(final UUID caseId, final String urn,
                    final List<HearingData> hearingData) {

        this.caseId = caseId;
        this.urn = urn;
        this.hearingData = hearingData;
    }

    public UUID getCaseId() { return caseId; }

    public String getUrn() { return urn; }

    public List<HearingData> getHearingData() { return hearingData; }
}
