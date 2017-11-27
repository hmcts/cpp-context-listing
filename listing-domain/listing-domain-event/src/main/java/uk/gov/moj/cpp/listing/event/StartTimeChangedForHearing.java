package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.start-time-changed-for-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class StartTimeChangedForHearing extends HearingEvent {

    private final LocalTime startTime;

    public StartTimeChangedForHearing(@JsonProperty(value = "startTime") final LocalTime startTime,
                                      @JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
        this.startTime = startTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

}
