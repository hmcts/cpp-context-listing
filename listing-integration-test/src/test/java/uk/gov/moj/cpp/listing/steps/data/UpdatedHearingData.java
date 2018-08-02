package uk.gov.moj.cpp.listing.steps.data;

import static java.util.UUID.randomUUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class UpdatedHearingData {

    private static final String SENTENCE_HEARING_TYPE = "Sentence";
    private static final ZoneId UTC = ZoneId.of("UTC");

    private final UUID hearingId;
    private final UUID judgeId;
    private final UUID courtRoomId;
    private final String type;
    private final String startDate;
    private final String endDate;
    private final List<String> startTimes;
    private final List<String> nonSittingDays;
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");


    public static UpdatedHearingData updatedHearingDataWithoutJudgeId(final UUID hearingId) {
        final LocalDate startDate = LocalDate.now();
        LocalTime startTime = LocalTime.now();
        ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, LocalTime.parse(startTime.format(dtf)), UTC);
        return new UpdatedHearingData(hearingId, null, randomUUID(), SENTENCE_HEARING_TYPE,
                startDate.toString(), Arrays.asList(startTimeWithZone.format(DATE_TIME_FORMAT)),
                Arrays.asList(startDate.plusDays(1).toString()), startDate.plusDays(2).toString());
    }

    public static UpdatedHearingData updatedHearingDataWithoutCourtRoomId(final UUID hearingId) {
        final LocalDate startDate = LocalDate.now();
        ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, LocalTime.now(), UTC);

        return new UpdatedHearingData(hearingId, randomUUID(), null, SENTENCE_HEARING_TYPE,
                startDate.toString(), Arrays.asList(startTimeWithZone.format(DATE_TIME_FORMAT)),
                Arrays.asList( startDate.plusDays(1).toString(), startDate.plusDays(2).toString()),
                startDate.plusDays(3).toString());
    }

    public static UpdatedHearingData updatedHearingDataWithAllFieldsSet(final UUID hearingId) {
        final LocalDate startDate = LocalDate.now();
        ZonedDateTime startTime = ZonedDateTime.of(startDate, LocalTime.now(), UTC);

        return new UpdatedHearingData(hearingId, randomUUID(), randomUUID(), SENTENCE_HEARING_TYPE,
               startDate.toString(),Arrays.asList(startTime.format(DATE_TIME_FORMAT)),
                Arrays.asList(startDate.plusDays(1).toString()), startDate.plusDays(2).toString());
    }


    public UpdatedHearingData(final UUID hearingId, final UUID judgeId, final UUID courtRoomId,
                              final String type, final String startDate, final List<String> startTimes,
                              final List<String> nonSittingDays, final String endDate) {
        this.hearingId = hearingId;
        this.judgeId = judgeId;
        this.courtRoomId = courtRoomId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTimes = startTimes;
        this.nonSittingDays = nonSittingDays;
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

    public List<String> getStartTimes() {
        return startTimes;
    }

    public String getEndDate() {
        return endDate;
    }

    public List<String> getNonSittingDays() {
        return nonSittingDays;
    }

    public UUID getHearingId() {
        return hearingId;
    }
}


