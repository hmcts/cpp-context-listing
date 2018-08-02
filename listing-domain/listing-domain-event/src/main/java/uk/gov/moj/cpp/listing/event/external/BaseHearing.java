package uk.gov.moj.cpp.listing.event.external;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("squid:S00107")
public class BaseHearing implements Serializable {

    private final String id;
    private final String type;
    private final String courtCentreId;
    private final String courtRoomId;
    private final String judgeId;

    private final List<ZonedDateTime> hearingDays;

    @JsonCreator
    public BaseHearing(@JsonProperty(value = "id") final String id,
                       @JsonProperty(value = "type") final String type,
                       @JsonProperty(value = "courtRoomId") final String courtRoomId,
                       @JsonProperty(value = "judgeId") final String judgeId,
                       @JsonProperty(value = "hearingDays") final List<ZonedDateTime> hearingDays,
                       @JsonProperty(value = "courtCentreId") final String courtCentreId
    ) {
        this.id = id;
        this.type = type;
        this.courtRoomId = courtRoomId;
        this.judgeId = judgeId;
        this.courtCentreId = courtCentreId;
        this.hearingDays = hearingDays;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public String getJudgeId() {
        return judgeId;
    }

    public List<ZonedDateTime> getHearingDays() {
        return hearingDays;
    }

    @Override
    public String toString() {
        return "BaseHearing{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", courtCentreId=" + courtCentreId +
                ", courtRoomId='" + courtRoomId + '\'' +
                ", judgeId='" + judgeId + '\'' +
                ", hearingDays=" + hearingDays +
                '}';
    }
}
