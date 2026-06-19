package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.moj.cpp.listing.domain.CaseMarker;

import java.util.List;
import java.util.UUID;

public class UpdateCaseMarkersForHearingCommand {

    private UUID prosecutionCaseId;

    private UUID hearingId;

    private List<CaseMarker> caseMarkers;

    @SuppressWarnings("squid:S2384")
    public UpdateCaseMarkersForHearingCommand(final UUID prosecutionCaseId, final UUID hearingId, final List<CaseMarker> caseMarkers) {
        this.prosecutionCaseId = prosecutionCaseId;
        this.hearingId = hearingId;
        this.caseMarkers = caseMarkers;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    @SuppressWarnings("squid:S2384")
    public List<CaseMarker> getCaseMarkers() {
        return caseMarkers;
    }
}
