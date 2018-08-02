package uk.gov.moj.cpp.listing.event.external;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("squid:S00107")
public class Hearing extends BaseHearing implements Serializable {

    private final String caseId;

    private final List<Defendant> defendants;

    @JsonCreator
    public Hearing(@JsonProperty(value = "id") final String id,
                   @JsonProperty(value = "type") final String type,
                   @JsonProperty(value = "caseId") final String caseId,
                   @JsonProperty(value = "courtCentreId") final String courtCentreId,
                   @JsonProperty(value = "courtRoomId") final String courtRoomId,
                   @JsonProperty(value = "judgeId") final String judgeId,
                   @JsonProperty(value = "hearingDays") final List<ZonedDateTime> hearingDays,
                   @JsonProperty(value = "defendants") final List<Defendant> defendants
    ) {
        super(id, type, courtRoomId, judgeId, hearingDays,
                courtCentreId);
        this.caseId = caseId;
        this.defendants = new ArrayList(defendants);
    }

    public String getCaseId() {
        return caseId;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    @Override
    public String toString() {
        return "Hearing{" +
                "id='" + this.getId() + '\'' +
                ", caseId='" + caseId + '\'' +
                ", type='" + getType() + '\'' +
                ", defendants=" + defendants +
                ", hearingDays=" + getHearingDays() + '\'' +
                ", courtRoomId='" + getCourtRoomId() + '\'' +
                ", judgeId='" + getJudgeId() + '\'' +
                '}';
    }
}
