package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class HearingAdjournedData {

    private final UUID hearingId;

    public HearingAdjournedData(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public UUID getHearingId() {
        return hearingId;
    }
}
