package uk.gov.moj.cpp.listing.steps.data;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;



public class UpdatedHearingData {

    private static final String TYPE = "TRIAL";
    private final UUID hearingId;
    private final UUID judgeId;
    private final UUID courtRoomId;
    private final String type;
    private final String startDate;
    private final String startTime;
    private final Boolean notBefore;
    private final Integer estimateMinutes;

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");




    public static UpdatedHearingData updatedHearingDataWithoutJudgeId(final UUID hearingId) {
        return new UpdatedHearingData(hearingId, null, randomUUID(), TYPE,
                LocalDate.now().toString(), LocalTime.now().format(dtf), FALSE, INTEGER.next());
    }

    public static UpdatedHearingData updatedHearingDataWithoutCourtRoomId(final UUID hearingId) {
        return new UpdatedHearingData(hearingId, randomUUID(), null, TYPE,
                LocalDate.now().toString(), LocalTime.now().format(dtf), FALSE, INTEGER.next());
    }

    public static UpdatedHearingData updatedHearingDataWithoutCourtRoomIdAndStartTime(final UUID hearingId) {
        return new UpdatedHearingData(hearingId, randomUUID(), null, TYPE,
                LocalDate.now().toString(), null, FALSE, INTEGER.next());
    }

    public static UpdatedHearingData updatedHearingDataWithAllFieldsSet(final UUID hearingId) {
        return new UpdatedHearingData(hearingId, randomUUID(), randomUUID(), TYPE,
                LocalDate.now().toString(), LocalTime.now().format(dtf), TRUE, INTEGER.next());
    }

    public static UpdatedHearingData updatedHearingDataWithoutStartTime(final UUID hearingId) {
        return new UpdatedHearingData(hearingId, randomUUID(), randomUUID(), TYPE,
                LocalDate.now().toString(), null, FALSE, INTEGER.next());
    }


    public UpdatedHearingData(final UUID hearingId, final UUID judgeId, final UUID courtRoomId,
                              final String type, final String startDate, final String startTime,
                              final Boolean notBefore, final Integer estimateMinutes) {
        this.hearingId = hearingId;
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.type = type;
        this.startDate = startDate;
        this.startTime = startTime;
        this.notBefore = notBefore;
        this.estimateMinutes = estimateMinutes;
    }

    public UUID getJudgeId() {
        return judgeId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public String getType() {
        return type;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public Boolean getNotBefore() {
        return notBefore;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public UUID getHearingId() {
        return hearingId;
    }
}


