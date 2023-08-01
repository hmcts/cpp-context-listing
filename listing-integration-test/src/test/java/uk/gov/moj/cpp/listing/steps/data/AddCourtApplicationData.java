package uk.gov.moj.cpp.listing.steps.data;

import java.util.List;
import uk.gov.justice.core.courts.CourtApplication;

import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;

public class AddCourtApplicationData {

    private final UUID hearingId;
    private final CourtApplication courtApplication;
    private List<ProsecutionCase> prosecutionCases;

    public AddCourtApplicationData(final UUID hearingId, final CourtApplication courtApplication) {
        this.hearingId = hearingId;
        this.courtApplication = courtApplication;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public CourtApplication getCourtApplication() {
        return courtApplication;
    }

    public List<ProsecutionCase> getProsecutionCases() {
        return prosecutionCases;
    }

    public void setProsecutionCases(final List<ProsecutionCase> prosecutionCases) {
        this.prosecutionCases = prosecutionCases;
    }
}
