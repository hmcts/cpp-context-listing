package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CaseData {

    private final UUID caseId;
    private final String urn;
    private final LocalDate sendingCommittalDate;
    private final List<HearingData> hearingData;

    public CaseData(final UUID caseId, final String urn,
                    final LocalDate sendingCommittalDate, final List<HearingData> hearingData) {

        this.caseId = caseId;
        this.urn = urn;
        this.sendingCommittalDate = sendingCommittalDate;
        this.hearingData = hearingData;
    }

    public UUID getCaseId() { return caseId; }

    public String getUrn() { return urn; }

    public LocalDate getSendingCommittalDate() { return sendingCommittalDate; }

    public List<HearingData> getHearingData() { return hearingData; }
}
