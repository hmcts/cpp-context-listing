package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HearingSummary implements Serializable {


    private final UUID id;

    private final UUID judgeId;

    private final boolean notBefore;

    private final String type;

    private Set<DefendantSummary> defendants;

    private final LocalDate startDate;

    private final LocalTime startTime;

    private final Integer estimateMinutes;

    private final UUID courtCentreId;

    private final UUID courtRoomId;

    private final UUID caseId;

    public HearingSummary(final UUID id, final String type, final UUID judgeId,
                          final boolean notBefore, final Set<DefendantSummary> defendants,
                          final HearingSummaryDetails hearingSummaryDetails){
        this.id = id;
        this.judgeId = judgeId;
        this.notBefore = notBefore;
        this.type = type;
        this.defendants = defendants;
        this.startDate = hearingSummaryDetails.getStartDate();
        this.startTime = hearingSummaryDetails.getStartTime();
        this.courtCentreId = hearingSummaryDetails.getCourtCentreId();
        this.courtRoomId = hearingSummaryDetails.getCourtRoomId();
        this.estimateMinutes = hearingSummaryDetails.getEstimateMinutes();
        this.caseId = hearingSummaryDetails.getCaseId();
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Set<DefendantSummary> getDefendants() {
        return defendants;
    }

    public UUID getJudgeId() { return judgeId; }

    public boolean getNotBefore() { return notBefore; }

    public LocalDate getStartDate() {
        return startDate;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public LocalTime getStartTime() { return startTime; }

    public UUID getCourtCentreId() { return courtCentreId; }

    public UUID getCourtRoomId() { return courtRoomId; }

    public UUID getCaseId() {
        return caseId;
    }

    public static class HearingSummaryDetails {

        private final LocalDate startDate;

        private final LocalTime startTime;

        private final Integer estimateMinutes;

        private final UUID courtCentreId;

        private final UUID courtRoomId;

        private final UUID caseId;

        public HearingSummaryDetails(final LocalDate startDate, final LocalTime startTime,
                                     final UUID courtCentreId, final UUID courtRoomId, final
                                     Integer estimateMinutes,final UUID caseId) {
            this.startDate = startDate;
            this.startTime = startTime;
            this.courtCentreId = courtCentreId;
            this.courtRoomId = courtRoomId;
            this.estimateMinutes = estimateMinutes;
            this.caseId = caseId;

        }
        public LocalDate getStartDate() {
            return startDate;
        }

        public Integer getEstimateMinutes() {
            return estimateMinutes;
        }

        public LocalTime getStartTime() { return startTime; }

        public UUID getCourtCentreId() { return courtCentreId; }

        public UUID getCourtRoomId() { return courtRoomId; }

        public UUID getCaseId() {
            return caseId;
        }
    }
}
