package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.start-date-changed-for-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class StartDateChangedForHearing extends HearingEvent {

    private final LocalDate startDate;

    public StartDateChangedForHearing(@JsonProperty(value = "startDate") final LocalDate startDate,
                                      @JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
        this.startDate = startDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
