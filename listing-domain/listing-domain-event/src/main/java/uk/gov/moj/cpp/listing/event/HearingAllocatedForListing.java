package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.hearing-allocated-for-listing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HearingAllocatedForListing extends HearingEvent {

    private final String type;
    private final Integer estimateMinutes;
    private final String judgeId;
    private final String courtRoomId;
    private final HearingDate hearingDate;


    public HearingAllocatedForListing(@JsonProperty(value = "hearingId") final String hearingId,
                                      @JsonProperty(value = "type") final String type,
                                      @JsonProperty(value = "estimateMinutes") final Integer estimateMinutes,
                                      @JsonProperty(value = "judgeId") final String judgeId,
                                      @JsonProperty(value = "courtRoomId") final String courtRoomId,
                                      @JsonProperty(value = "hearingDate") final HearingDate hearingDate) {
        super(hearingId);
        this.type = type;
        this.estimateMinutes = estimateMinutes;
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.hearingDate = hearingDate;
    }

    public String getType() {
        return type;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public String getJudgeId() {
        return judgeId;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public HearingDate getHearingDate() {
        return hearingDate;
    }
}
