package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CaseData {

    private final UUID caseProgressionId;
    private final String urn;
    private final List<HearingData> hearingData;

    public CaseData(final UUID caseProgressionId, final String urn,
                    final List<HearingData> hearingData) {

        this.caseProgressionId = caseProgressionId;
        this.urn = urn;
        this.hearingData = hearingData;
    }

    public UUID getCaseProgressionId() { return caseProgressionId; }

    public String getUrn() { return urn; }

    public List<HearingData> getHearingData() { return hearingData; }
}
