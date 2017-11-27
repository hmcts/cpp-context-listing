package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.court-room-removed-from-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class CourtRoomRemovedFromHearing extends HearingEvent {

    public CourtRoomRemovedFromHearing(@JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
    }

}
