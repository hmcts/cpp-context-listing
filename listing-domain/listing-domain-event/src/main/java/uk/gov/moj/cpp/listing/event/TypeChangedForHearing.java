package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.type-changed-for-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class TypeChangedForHearing extends HearingEvent {

    private final String type;

    public TypeChangedForHearing(@JsonProperty(value = "type") final String type,
                                 @JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
