package uk.gov.moj.cpp.listing.domain.transformation.judicialresult.domain;

import java.util.Arrays;

public enum EventToTransform {

    CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED("listing.events.case-resulted-defendant-proceedings-updated"),
    CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED("listing.events.case-update-defendant-proceedings-updated");

    private final String eventName;

    EventToTransform(final String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public static boolean isEventToTransform(final String eventName) {
        return Arrays.stream(values()).anyMatch(event -> event.eventName.equals(eventName));
    }
}
