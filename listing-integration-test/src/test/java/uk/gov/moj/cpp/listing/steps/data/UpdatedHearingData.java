package uk.gov.moj.cpp.listing.steps.data;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class UpdatedHearingData {

    private static final HearingTypeData SENTENCE_HEARING_TYPE = new HearingTypeData(randomUUID(), "Sentence");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final String HEARING_LANGUAGE_WELSH = "WELSH";
    private static final String HEARING_LANGUAGE_ENGLISH = "ENGLISH";
    private static final String JURISDICTION_TYPE_MAGISTRATES = "MAGISTRATES";
    private static final String COURT_SCHEDULE_ID = randomUUID().toString();
    private static final String OUCODE = "BKROOL";
    private static final String SESSION = "AM";
    private static final int DURATION = 120;

    private final UUID hearingId;
    private final UUID courtCentreId;
    private final UUID courtRoomId;
    private final HearingTypeData hearingTypData;
    private final String startDate;
    private final String endDate;
    private final String hearingLanguage;
    private final List<NonDefaultDayData> nonDefaultDays;
    private final List<String> nonSittingDays;
    private final List<JudicialRoleData> judiciary;
    private final String weekCommencingStartDate;
    private final String weekCommencingEndDate;
    private final Integer weekCommencingDurationInWeeks;


    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final String jurisdictionType;


    public static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId) {
        List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE")));
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithoutJudiciary(final UUID hearingId) {
        List<JudicialRoleData> judiciary = Collections.emptyList();
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    private static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId, final List<JudicialRoleData> judiciary) {

        final LocalDate startDate = LocalDate.now();
        final LocalTime startTime = LocalTime.now();
        final ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, LocalTime.parse(startTime.format(dtf)), UTC);

        final List<String> nonSittingDays = asList(startDate.plusDays(1).toString());

        final NonDefaultDayData firstNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION), of(COURT_SCHEDULE_ID), of(1), of(OUCODE), of(SESSION));
        final NonDefaultDayData secondNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.plusDays(1).format(DATE_TIME_FORMAT), of(DURATION), of(randomUUID().toString()), of(2), of("BAHOO2"), of("PM"));

        final List<NonDefaultDayData> nonDefaultDays = asList(firstNonDefaultDayData, secondNonDefaultDayData);

        String endDate = startDate.plusDays(2).toString();

        return new UpdatedHearingData(hearingId, randomUUID(), randomUUID(), SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES, null, null, null);
    }


    public static UpdatedHearingData updatedHearingData(HearingData hearingData) {

        //changed values
        final LocalDate startDate = LocalDate.now().plusDays(21);
        LocalTime startTime = LocalTime.of(10, 0);
        String endDate = startDate.toString();
        UUID courtRoomId = randomUUID();
        List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(false), UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(), "CIRCUIT_JUDGE")));

        ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, startTime, UTC);
        List<NonDefaultDayData> nonDefaultDays = asList(new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION)));

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), courtRoomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, judiciary, hearingData.getJurisdictionType(), null, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithNoCourtRoom(HearingData hearingData) {

        //changed values
        UUID courtRoomId = null;

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), courtRoomId, hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(),
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(DURATION))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithNoEndDate(HearingData hearingData) {

        //changed values
        String endDate = null;

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), endDate,
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(DURATION))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null);

    }

    public static UpdatedHearingData updatedHearingDataDifferentJudiciary(HearingData hearingData) {

        //changed values
        List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE")));


        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(),
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, judiciary, hearingData.getJurisdictionType(), null, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithWeekCommencingDate(final HearingData hearingData, final String weekCommencingStartDate, final String weekCommencingEndDate, final int weekCommencingDurationInWeeks) {
        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), null, hearingData.getHearingTypeData(),
                null, null,
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(DURATION))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks);

    }

    public UpdatedHearingData(final UUID hearingId,
                              final UUID courtCentreId,
                              final UUID courtRoomId,
                              final HearingTypeData type,
                              final String startDate,
                              final String endDate,
                              final List<NonDefaultDayData> nonDefaultDays, final List<String> nonSittingDays,
                              final String hearingLanguage,
                              final List<JudicialRoleData> judiciary, final String jurisdictionType,
                              final String weekCommencingStartDate,
                              final String weekCommencingEndDate,
                              final Integer weekCommencingDurationInWeeks) {
        this.hearingId = hearingId;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.hearingTypData = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.hearingLanguage = hearingLanguage;
        this.nonSittingDays = nonSittingDays;
        this.nonDefaultDays = nonDefaultDays;
        this.judiciary = judiciary;
        this.jurisdictionType = jurisdictionType;
        this.weekCommencingStartDate = weekCommencingStartDate;
        this.weekCommencingEndDate = weekCommencingEndDate;
        this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;
    }


    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public HearingTypeData getHearingTypData() {
        return hearingTypData;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getHearingLanguage() {
        return hearingLanguage;
    }

    public List<NonDefaultDayData> getNonDefaultDays() {
        return nonDefaultDays;
    }

    public List<String> getNonSittingDays() {
        return nonSittingDays;
    }

    public List<JudicialRoleData> getJudiciary() {
        return judiciary;
    }

    public String getJurisdictionType() {
        return jurisdictionType;
    }

    public String getWeekCommencingStartDate() {
        return weekCommencingStartDate;
    }

    public String getWeekCommencingEndDate() {
        return weekCommencingEndDate;
    }

    public Integer getWeekCommencingDurationInWeeks() {
        return weekCommencingDurationInWeeks;
    }
}


