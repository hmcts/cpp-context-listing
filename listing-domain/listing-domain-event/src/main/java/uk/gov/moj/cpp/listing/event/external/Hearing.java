package uk.gov.moj.cpp.listing.event.external;


import uk.gov.moj.cpp.listing.Judge;
import uk.gov.moj.cpp.listing.domain.Defendant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("squid:S00107")
public class Hearing implements Serializable {

    private final String id;
    private final String caseId;
    private final String courtCentreId;
    private final String courtCentreName;
    private final String type;
    private final int estimateMinutes;
    private final List<Defendant> defendants;
    private final String courtRoomId;
    private final String courtRoomName;
    private final Judge judge;
    private final String startDateTime;
    private final boolean notBefore;

    @JsonCreator
    public Hearing(@JsonProperty(value = "id") final String id,
                   @JsonProperty(value = "type") final String type,
                   @JsonProperty(value = "caseId") final String caseId,
                   @JsonProperty(value = "courtCentreId") final String courtCentreId,
                   @JsonProperty(value = "courtCentreName") final String courtCentreName,
                   @JsonProperty(value = "courtRoomId") final String courtRoomId,
                   @JsonProperty(value = "courtRoomName") final String courtRoomName,
                   @JsonProperty(value = "judge") final Judge judge,
                   @JsonProperty(value = "startDateTime") final String startDateTime,
                   @JsonProperty(value = "notBefore") final boolean notBefore,
                   @JsonProperty(value = "estimateMinutes") final int estimateMinutes,
                   @JsonProperty(value = "defendants") final List<Defendant> defendants
    ) {
        this.id = id;
        this.type = type;
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
        this.courtCentreName = courtCentreName;
        this.courtRoomId = courtRoomId;
        this.courtRoomName = courtRoomName;
        this.judge = judge;
        this.startDateTime = startDateTime;
        this.notBefore = notBefore;
        this.estimateMinutes = estimateMinutes;
        this.defendants = new ArrayList(defendants);
    }



    public List<Defendant> getDefendants() {
        return new ArrayList(defendants);
    }

    public String getId() {
        return id;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public String getType() {
        return type;
    }

    public int getEstimateMinutes() {
        return estimateMinutes;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public Judge getJudge() {
        return judge;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public boolean isNotBefore() {
        return notBefore;
    }

    @Override
    public String toString() {
        return "Hearing{" +
                "id='" + id + '\'' +
                ", caseId='" + caseId + '\'' +
                ", courtCentreId='" + courtCentreId + '\'' +
                ", type='" + type + '\'' +
                ", estimateMinutes=" + estimateMinutes +
                ", defendants=" + defendants +
                ", courtCentreName='" + courtCentreName + '\'' +
                ", courtRoomId='" + courtRoomId + '\'' +
                ", courtRoomName='" + courtRoomName + '\'' +
                ", judge=" + judge +
                ", startDateTime=" + startDateTime +
                ", notBefore=" + notBefore +
                '}';
    }
}
