package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.CourtApplication;

import java.util.UUID;

public class AddCourtApplicationData {

    private final UUID hearingId;
    private final CourtApplication courtApplication;

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
}
