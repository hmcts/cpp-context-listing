package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.judge-removed-from-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class JudgeRemovedFromHearing extends HearingEvent {

    public JudgeRemovedFromHearing(@JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
    }

}
