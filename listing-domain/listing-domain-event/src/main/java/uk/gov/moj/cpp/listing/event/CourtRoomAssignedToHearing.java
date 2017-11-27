package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.court-room-assigned-to-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class CourtRoomAssignedToHearing extends HearingEvent {

    private final String courtRoomId;

    public CourtRoomAssignedToHearing(@JsonProperty(value = "courtRoomId") final String courtRoomId,
                                      @JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
        this.courtRoomId = courtRoomId;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }
}

