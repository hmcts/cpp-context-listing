package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.hearing-updated-for-listing")
@JsonInclude(value = Include.NON_NULL)
public class HearingUpdatedForListing {

    private final String hearingId;
    private final String judgeId;
    private final String courtRoomId;
    private final String type;
    private final HearingPeriod hearingPeriod;
    private final Integer estimateMinutes;

    public HearingUpdatedForListing(@JsonProperty(value = "hearingId") final String hearingId,
                                    @JsonProperty(value = "judgeId") final String judgeId,
                                    @JsonProperty(value = "courtRoomId") final String courtRoomId,
                                    @JsonProperty(value = "type") final String type,
                                    @JsonProperty(value = "hearingPeriod") final HearingPeriod hearingPeriod,
                                    @JsonProperty(value = "estimateMinutes") final Integer estimateMinutes
                                   ) {
        this.hearingId = hearingId;
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.type = type;
        this.hearingPeriod = hearingPeriod;
        this.estimateMinutes = estimateMinutes;
    }

    public String getHearingId() {
        return hearingId;
    }

    public String getJudgeId() {
        return judgeId;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public String getType() {
        return type;
    }

    public HearingPeriod getHearingPeriod() {
        return hearingPeriod;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }
}
