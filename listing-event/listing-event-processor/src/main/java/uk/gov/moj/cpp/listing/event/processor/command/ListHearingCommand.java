package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.moj.cpp.listing.domain.Defendant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"squid:S00107"})
public class ListHearingCommand {

    private final String hearingId;
    private final String type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer estimateMinutes;
    private final String caseId;
    private final String urn;
    private final String courtCentreId;
    private final String judgeId;
    private final String courtRoomId;
    private final String startTime;
    private final List<Defendant> defendants;


    public ListHearingCommand(final String hearingId,
                              final String type,
                              final LocalDate startDate,
                              final LocalDate endDate,
                              final Integer estimateMinutes,
                              final String caseId,
                              final String courtCentreId,
                              final String courtRoomId,
                              final String judgeId,
                              final String startTime,
                              final List<Defendant> defendants,
                              final String urn) {
        this.hearingId = hearingId;
        this.type = type;
        this.startDate = startDate;
        this.estimateMinutes = estimateMinutes;
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.judgeId = judgeId;
        this.startTime = startTime;
        this.endDate = endDate;
        this.defendants = defendants;
        this.urn = urn;
    }

    public String getHearingId() {
        return hearingId;
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

    public String getCaseId() {
        return caseId;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public String getJudgeId() {
        return judgeId;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public String getStartTime() {
        return startTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<Defendant> getDefendants() {
        return new ArrayList<>(defendants);
    }

    public String getUrn() {
        return urn;
    }
}
