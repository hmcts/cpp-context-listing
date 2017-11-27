package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.hearing-unallocated-for-listing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("squid:S00107")
public class HearingUnallocatedForListing extends HearingEvent {

    private final String type;
    private final LocalDate startDate;
    private final Integer estimateMinutes;
    private final String judgeId;
    private final String courtRoomId;
    private final LocalTime startTime;
    private final boolean notBefore;

    public HearingUnallocatedForListing(@JsonProperty(value = "hearingId") final String hearingId,
                                        @JsonProperty(value = "type") final String type,
                                        @JsonProperty(value = "startDate") final LocalDate startDate,
                                        @JsonProperty(value = "estimateMinutes") final Integer estimateMinutes,
                                        @JsonProperty(value = "judgeId") final String judgeId,
                                        @JsonProperty(value = "courtRoomId") final String courtRoomId,
                                        @JsonProperty(value = "startTime") final LocalTime startTime,
                                        @JsonProperty(value = "notBefore") final boolean notBefore) {
        super(hearingId);
        this.type = type;
        this.startDate = startDate;
        this.estimateMinutes = estimateMinutes;
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.startTime = startTime;
        this.notBefore = notBefore;
    }

    public String getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
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

    public LocalTime getStartTime() {
        return startTime;
    }

    public boolean isNotBefore() {
        return notBefore;
    }
}
