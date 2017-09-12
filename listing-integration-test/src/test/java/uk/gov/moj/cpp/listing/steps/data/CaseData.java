package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CaseData {

    private final UUID caseId;
    private final String urn;
    private final List<DefendantData> defendants;
    private final LocalDate sendingCommittalDate;
    private final HearingData hearingData;

    public CaseData(final UUID caseId, final String urn, final List<DefendantData> defendants,
                    final LocalDate sendingCommittalDate, final HearingData hearingData) {

        this.caseId = caseId;
        this.defendants = defendants;
        this.urn = urn;
        this.sendingCommittalDate = sendingCommittalDate;
        this.hearingData = hearingData;
    }

    public UUID getCaseId() { return caseId; }

    public String getUrn() { return urn; }

    public List<DefendantData> getDefendants() { return defendants; }

    public LocalDate getSendingCommittalDate() { return sendingCommittalDate; }

    public HearingData getHearingData() { return hearingData; }
}
