package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@SuppressWarnings("squid:S00107")
public class HearingSummary implements Serializable {

    static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");


    private final UUID id;
    private final UUID judgeId;
    private final String type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<String> startTimes;
    private final List<LocalDate> nonSittingDays;
    private Set<DefendantSummary> defendants;
    private final Integer estimateMinutes;
    private final UUID courtCentreId;
    private final UUID courtRoomId;
    private final UUID caseId;

    public HearingSummary(final UUID id, final String type, final UUID judgeId,
                          final Set<DefendantSummary> defendants,
                          final HearingSummaryDetails hearingSummaryDetails){
        List<String> startTimesList = null;
        if(hearingSummaryDetails.getStartTimes()!=null) {
            startTimesList = hearingSummaryDetails.getStartTimes().stream().map(st -> st.format(DATE_TIME_FORMAT)).collect(Collectors.toList());
        }
        this.id = id;
        this.judgeId = judgeId;
        this.endDate = hearingSummaryDetails.getEndDate();
        this.startDate = hearingSummaryDetails.getStartDate();
        this.type = type;
        this.defendants = defendants;
        this.startTimes = startTimesList;
        this.nonSittingDays = hearingSummaryDetails.getNonSittingDays();
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public  List<String> getStartTimes() { return startTimes; }

    public UUID getCourtCentreId() { return courtCentreId; }

    public UUID getCourtRoomId() { return courtRoomId; }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<LocalDate> getNonSittingDays() {
        return nonSittingDays;
    }

    public static class HearingSummaryDetails {

        private final LocalDate startDate;

        private final LocalDate endDate;

        private final Integer estimateMinutes;

        private final List<LocalDate> nonSittingDays;

        private final List<ZonedDateTime> startTimes;

        private final UUID courtCentreId;

        private final UUID courtRoomId;

        private final UUID caseId;


        public HearingSummaryDetails(final LocalDate startDate, final LocalDate endDate,
                                     final List<LocalDate>nonSittingDays, final List<ZonedDateTime> startTimes,
                                     final UUID courtCentreId, final UUID courtRoomId, final
                                     Integer estimateMinutes,final UUID caseId) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.startTimes = startTimes;
            this.nonSittingDays = nonSittingDays;
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

        public  List<ZonedDateTime> getStartTimes() { return startTimes; }

        public UUID getCourtCentreId() { return courtCentreId; }

        public UUID getCourtRoomId() { return courtRoomId; }

        public UUID getCaseId() {
            return caseId;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public List<LocalDate> getNonSittingDays() {
            return nonSittingDays;
        }
    }
}
