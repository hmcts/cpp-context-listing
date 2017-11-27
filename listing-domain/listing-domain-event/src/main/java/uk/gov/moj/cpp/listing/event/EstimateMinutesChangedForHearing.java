package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.estimate-minutes-changed-for-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class EstimateMinutesChangedForHearing extends HearingEvent {

    private final Integer estimateMinutes;

    public EstimateMinutesChangedForHearing(@JsonProperty(value = "estimateMinutes") final Integer estimateMinutes,
                                            @JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
        this.estimateMinutes = estimateMinutes;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }
}
