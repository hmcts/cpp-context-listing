package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HearingCaseSummary implements Serializable {


    private final UUID id;

    private final UUID judgeId;

    private final String type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<String> startTimes;
    private final List<LocalDate> nonSittingDays;
    private final Integer estimateMinutes;
    private final UUID courtCentreId;
    private final UUID courtRoomId;
    private Set<DefendantSummary> defendants;
    private final String urn;

    public HearingCaseSummary(final HearingSummary hearingSummary,
                              final String urn) {
        this.id = hearingSummary.getId();
        this.judgeId = hearingSummary.getJudgeId();
        this.type = hearingSummary.getType();
        this.defendants = hearingSummary.getDefendants();
        this.startDate = hearingSummary.getStartDate();
        this.endDate = hearingSummary.getEndDate();
        this.startTimes = hearingSummary.getStartTimes();
        this.nonSittingDays = hearingSummary.getNonSittingDays();
        this.courtCentreId = hearingSummary.getCourtCentreId();
        this.courtRoomId = hearingSummary.getCourtRoomId();
        this.estimateMinutes = hearingSummary.getEstimateMinutes();
        this.urn = urn;
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

    public UUID getJudgeId() {
        return judgeId;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<String> getStartTimes() {
        return startTimes;
    }

    public List<LocalDate> getNonSittingDays() {
        return nonSittingDays;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }


    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public String getUrn() {
        return urn;
    }
}
