package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.judge-assigned-to-hearing")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class JudgeAssignedToHearing extends HearingEvent {

    private final String judgeId;

    public JudgeAssignedToHearing(@JsonProperty(value = "judgeId") final String judgeId,
                                  @JsonProperty(value = "hearingId") final String hearingId) {
        super(hearingId);
        this.judgeId = judgeId;
    }

    public String getJudgeId() {
        return judgeId;
    }
}
