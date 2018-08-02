package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class Hearing implements Serializable {

    private final String id;
    private final String caseId;
    private final String courtCentreId;
    private final String type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int estimateMinutes;
    private final String courtRoomId;
    private final String judgeId;
    private final String startDateTime;
    private final List<Defendant> defendants;
    private final boolean allocated;


    @JsonCreator
    public Hearing(@JsonProperty(value = "id") final String id,
                   @JsonProperty(value = "caseId") final String caseId,
                   @JsonProperty(value = "courtCentreId") final String courtCentreId,
                   @JsonProperty(value = "type") final String type,
                   @JsonProperty(value = "startDate") final LocalDate startDate,
                   @JsonProperty(value = "endDate") final LocalDate endDate,
                   @JsonProperty(value = "estimateMinutes") final int estimateMinutes,
                   @JsonProperty(value = "courtRoomId") final String courtRoomId,
                   @JsonProperty(value = "judgeId") final String judgeId,
                   @JsonProperty(value = "startDateTime") final String startDateTime,
                   @JsonProperty(value = "defendants") final List<Defendant> defendants,
                   @JsonProperty(value = "allocated") final boolean allocated
                   ) {
        this.id = id;
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.estimateMinutes = estimateMinutes;
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.startDateTime = startDateTime;
        this.allocated = allocated;
        this.defendants = new ArrayList<>(defendants);
    }

    public boolean isAllocated() {
        return allocated;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getId() {
        return id;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public String getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getEstimateMinutes() {
        return estimateMinutes;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public String getJudgeId() {
        return judgeId;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public List<Defendant> getDefendants() {
        return new ArrayList<>(defendants);
    }
}
