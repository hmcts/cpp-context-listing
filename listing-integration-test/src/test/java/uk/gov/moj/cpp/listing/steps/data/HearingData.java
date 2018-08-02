package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class HearingData {

    private final UUID id;
    private final String courtCentreId;
    private final String hearingType;
    private final LocalDate hearingStartDate;
    private final LocalDate hearingEndDate;
    private final int hearingEstimateMinutes;
    private final List<DefendantData> defendants;
    private final UUID judgeId;
    private final UUID courtRoomId;
    private final String hearingStartTime;

    public HearingData(final UUID id, final String courtCentreId, final String hearingType,
                       final LocalDate hearingStartDate,  final LocalDate hearingEndDate,
                       final int hearingEstimateMinutes,final List<DefendantData> defendants,
                       final UUID courtRoomId, final UUID judgeId, final String hearingStartTime) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.hearingEstimateMinutes = hearingEstimateMinutes;
        this.hearingStartDate = hearingStartDate;
        this.hearingEndDate = hearingEndDate;
        this.hearingType = hearingType;
        this.defendants = defendants;
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.hearingStartTime = hearingStartTime;

    }

    public UUID getId() { return id; }

    public String getCourtCentreId() { return courtCentreId; }

    public String getHearingType() { return hearingType; }

    public LocalDate getHearingStartDate() { return hearingStartDate; }

    public int getHearingEstimateMinutes() { return hearingEstimateMinutes; }

    public List<DefendantData> getDefendants() {
        return defendants;
    }

    public UUID getJudgeId() {
        return judgeId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public LocalDate getHearingEndDate() {
        return hearingEndDate;
    }

    public String getHearingStartTime() {
        return hearingStartTime;
    }
}
