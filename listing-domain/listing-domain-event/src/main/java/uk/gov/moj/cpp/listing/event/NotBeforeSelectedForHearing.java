package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.not-before-selected-for-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NotBeforeSelectedForHearing extends HearingEvent {

    public NotBeforeSelectedForHearing(@JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
    }

}
