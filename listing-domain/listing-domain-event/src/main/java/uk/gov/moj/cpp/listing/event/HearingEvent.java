package uk.gov.moj.cpp.listing.event;

public class HearingEvent {

    private final String hearingId;

    public HearingEvent(final String hearingId) {
        this.hearingId = hearingId;
    }

    public String getHearingId() {
        return hearingId;
    }
}
