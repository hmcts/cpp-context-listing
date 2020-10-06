package uk.gov.moj.cpp.listing.steps.data;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.DayOfWeek;
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

    protected static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 30);
    protected static final LocalTime DEFAULT_START_TIME_1 = LocalTime.of(17, 00);
    private static final HearingTypeData SENTENCE_HEARING_TYPE = new HearingTypeData(randomUUID(), "Sentence");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final String HEARING_LANGUAGE_WELSH = "WELSH";
    private static final String HEARING_LANGUAGE_ENGLISH = "ENGLISH";
    private static final String JURISDICTION_TYPE_MAGISTRATES = "MAGISTRATES";
    private static final String JURISDICTION_TYPE_CROWN = "CROWN";
    private static final String COURT_SCHEDULE_ID = randomUUID().toString();
    private static final String OUCODE = "BKROOL";
    private static final String SESSION = "AM";
    private static final int DURATION = 120;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String NO_DEFAULT_DAYS_DATE = "2020-04-23";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
    private final UUID hearingId;
    private final UUID courtCentreId;
    private final String name;
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
    private final String jurisdictionType;
    private final Boolean hasVideoLink;
    private final String videoLinkDetails;

    public UpdatedHearingData(final UUID hearingId,
                              final UUID courtCentreId,
                              final String name,
                              final UUID courtRoomId,
                              final HearingTypeData type,
                              final String startDate,
                              final String endDate,
                              final List<NonDefaultDayData> nonDefaultDays, final List<String> nonSittingDays,
                              final String hearingLanguage,
                              final List<JudicialRoleData> judiciary, final String jurisdictionType,
                              final String weekCommencingStartDate,
                              final String weekCommencingEndDate,
                              final Integer weekCommencingDurationInWeeks,
                              final Boolean hasVideoLink,
                              final String videoLinkDetails) {
        this.hearingId = hearingId;
        this.courtCentreId = courtCentreId;
        this.name = name;
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
        this.hasVideoLink = hasVideoLink;
        this.videoLinkDetails = videoLinkDetails;
    }

    public static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId) {
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), UUID.randomUUID(),new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE")));
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithNonDefaultDays(final UUID hearingId) {
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(),UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE")));
        return updatedHearingDataForAllocationWithNonDefaultDays(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithoutJudiciary(final UUID hearingId) {
        final List<JudicialRoleData> judiciary = Collections.emptyList();
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    private static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId, final List<JudicialRoleData> judiciary) {

        final LocalDate startDate = nextOrSameWorkingDay(LocalDate.now());
        final LocalTime startTime = DEFAULT_START_TIME;
        final ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, LocalTime.parse(startTime.format(dtf)), UTC);

        final List<String> nonSittingDays = asList(startDate.plusDays(1).toString());

        final NonDefaultDayData firstNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION), of(COURT_SCHEDULE_ID), of(1), of(OUCODE), of(SESSION));
        final NonDefaultDayData secondNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.plusDays(2).format(DATE_TIME_FORMAT), of(DURATION), of(randomUUID().toString()), of(2), of("BAHOO2"), of("PM"));

        final List<NonDefaultDayData> nonDefaultDays = asList(firstNonDefaultDayData, secondNonDefaultDayData);

        final String endDate = startDate.plusDays(2).toString();
        final Boolean hasVideoLink = true;
        final String videoLinkDetails = "videoLinkDetails";

        return new UpdatedHearingData(hearingId, randomUUID(), RandomGenerator.STRING.next(), randomUUID(), SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES, null, null, null, hasVideoLink, videoLinkDetails);
    }

    public static UpdatedHearingData updatedHearingDataForVideoLink(final HearingData hearingData,final Boolean hasVideoLink, final String videoLinkDetails) {

        final LocalDate startDate = nextOrSameWorkingDay(LocalDate.now());

        final List<String> nonSittingDays = asList(startDate.plusDays(1).toString());

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), RandomGenerator.STRING.next(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(), Collections.emptyList(),
                nonSittingDays, HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), JURISDICTION_TYPE_CROWN, null, null, hearingData.getWeekCommencingDuration(), hasVideoLink, videoLinkDetails);
    }

    private static LocalDate nextOrSameWorkingDay(LocalDate date) {
        return isWeekEnd(date) ? nextWorkingDay(date) : date;
    }

    private static LocalDate nextWorkingDay(LocalDate date) {
        do {
            date = date.plusDays(1);
        } while (isWeekEnd(date));
        return date;
    }

    private static boolean isWeekEnd(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }


    private static UpdatedHearingData updatedHearingDataForAllocationWithNonDefaultDays(final UUID hearingId, final List<JudicialRoleData> judiciary) {
        final String endDate = "2020-04-23";
        final LocalDate startDate = LocalDate.parse(endDate);
        final ZonedDateTime startTimeWithZone = ZonedDateTime.parse("2020-04-23T11:32:41.587Z");
        final List<String> nonSittingDays = asList(startDate.plusDays(1).toString());


        final List<NonDefaultDayData> nonDefaultDays = asList(new NonDefaultDayData(startTimeWithZone.toString(), of(15)));

        return new UpdatedHearingData(hearingId, randomUUID(), "Carmarthen Magistrates Court", randomUUID(), SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES, null, null, null, null, null);
    }

    public static UpdatedHearingData updatedHearingData(final HearingData hearingData) {

        //changed values
        final LocalDate startDate = LocalDate.now().plusDays(21);
        final LocalTime startTime = LocalTime.of(10, 0);
        final String endDate = startDate.toString();
        final UUID courtRoomId = randomUUID();
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(false), UUID.randomUUID(),UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(), "CIRCUIT_JUDGE")));

        final ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, startTime, UTC);
        final List<NonDefaultDayData> nonDefaultDays = asList(new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION)));
        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getName(), courtRoomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, judiciary, hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getVideoLinkDetails());

    }
    public static UpdatedHearingData updatedHearingDataWithVideoLink(final UpdatedHearingData hearingData) {

        //changed values

        final String videoLinkDetails = "videoLinkChanged";
        return new UpdatedHearingData(hearingData.getHearingId(), hearingData.getCourtCentreId(), hearingData.getName(), hearingData.getCourtRoomId(), SENTENCE_HEARING_TYPE,
                hearingData.getStartDate(), hearingData.getEndDate(), hearingData.getNonDefaultDays(),
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), videoLinkDetails);

    }

    public static UpdatedHearingData updatedHearingDataWithoutVideoLink(final UpdatedHearingData hearingData) {

        //changed values
        final Boolean hasVideoLink = null;
        final String videoLinkDetails = null;
        return new UpdatedHearingData(hearingData.getHearingId(), hearingData.getCourtCentreId(), hearingData.getName(), hearingData.getCourtRoomId(), SENTENCE_HEARING_TYPE,
                hearingData.getStartDate(), hearingData.getEndDate(), hearingData.getNonDefaultDays(),
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hasVideoLink, videoLinkDetails);

    }

    public static UpdatedHearingData updatedHearingDataWithNoCourtRoom(final HearingData hearingData) {

        //changed values
        final UUID courtRoomId = null;

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getName(), courtRoomId, hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(),
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(DURATION))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getVideoLinkDetails());

    }

    public static UpdatedHearingData updatedHearingDataWithNoEndDate(final HearingData hearingData) {

        //changed values
        final String endDate = null;

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getName(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), endDate,
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(DURATION))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getVideoLinkDetails());

    }

    public static UpdatedHearingData updatedHearingDataDifferentJudiciary(final HearingData hearingData) {

        //changed values
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(),UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE")));


        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getName(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(),
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, judiciary, hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getVideoLinkDetails());

    }

    public static UpdatedHearingData updatedHearingDataWithWeekCommencingDate(final HearingData hearingData, final String weekCommencingStartDate, final String weekCommencingEndDate, final int weekCommencingDurationInWeeks) {
        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getName(), null, hearingData.getHearingTypeData(),
                null, null,
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(1))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingData.getHasVideoLink(), hearingData.getVideoLinkDetails());

    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public String getName() {
        return name;
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

    public Boolean getHasVideoLink() {
        return hasVideoLink;
    }

    public String getVideoLinkDetails() {
        return videoLinkDetails;
    }
}


