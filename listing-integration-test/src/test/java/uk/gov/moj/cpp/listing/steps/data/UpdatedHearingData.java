package uk.gov.moj.cpp.listing.steps.data;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import uk.gov.justice.listing.events.Defendants;
import uk.gov.justice.listing.events.Offences;
import uk.gov.justice.listing.events.ProsecutionCases;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UpdatedHearingData {

    protected static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final HearingTypeData SENTENCE_HEARING_TYPE = new HearingTypeData(randomUUID(), "Sentence", "welshSentence");
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
    private final String publicListNote;
    private final String panel = "ADULT";
    private  String priority;
    private  String bookingType;
    private List<String> specialRequirements;
    private Boolean sendNotificationToParties;
    private List<ProsecutionCases> prosecutionCases;
    private List<Defendants> defendants;
    private List<Offences> offences;
    private String splitHearing;



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
                              final String publicListNote,
                              final String priority,
                              final String bookingType,
                              final List<String> specialRequirements,
                              final Boolean sendNotificationToParties,
                              final List<ProsecutionCases> prosecutionCases) {
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
        this.publicListNote = publicListNote;
        this.priority = priority;
        this.bookingType = bookingType;
        this.specialRequirements = specialRequirements;
        this.sendNotificationToParties = sendNotificationToParties;
        this.prosecutionCases = prosecutionCases;
    }

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
                              final String publicListNote,
                              final Boolean sendNotificationToParties,
                              final List<ProsecutionCases> prosecutionCases,
                              final String splitHearing) {
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
        this.publicListNote = publicListNote;
        this.sendNotificationToParties = sendNotificationToParties;
        this.prosecutionCases = prosecutionCases;
        this.splitHearing = splitHearing;

    }

    public static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId) {
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), UUID.randomUUID(), new JudicialRoleTypeData(Optional.of(randomUUID()), "MAGISTRATE")));
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithJurisdictionType(final UUID hearingId, final String jurisdictionType) {
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), UUID.randomUUID(), new JudicialRoleTypeData(Optional.of(randomUUID()), jurisdictionType)));
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithDefendant(final UUID hearingId, final HearingsData hearingData) {
        return updatedHearingDataForAllocationForDefendant(hearingId, hearingData);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithNonDefaultDays(final UUID hearingId) {
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), UUID.randomUUID(), new JudicialRoleTypeData(Optional.of(randomUUID()), "MAGISTRATE")));
        return updatedHearingDataForAllocationWithNonDefaultDays(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithNonDefaultDaysWithAdditionalFields(final UUID hearingId) {
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), UUID.randomUUID(), new JudicialRoleTypeData(Optional.of(randomUUID()), "MAGISTRATE")));
        return updatedHearingDataForAllocationWithNonDefaultDaysWithAdditionalFields(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithoutJudiciary(final UUID hearingId) {
        final List<JudicialRoleData> judiciary = Collections.emptyList();
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    private static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId, final List<JudicialRoleData> judiciary) {

        final UUID courtCentreId = randomUUID();
        final UUID roomId = randomUUID();

        final LocalDate startDate = nextOrSameWorkingDay(LocalDate.now());
        final ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, DEFAULT_START_TIME, UTC);

        final List<String> nonSittingDays = Collections.singletonList(startDate.plusDays(1).toString());

        final NonDefaultDayData firstNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION), of(COURT_SCHEDULE_ID), of(1), of(OUCODE), of(SESSION), of(courtCentreId).map(UUID::toString), of(roomId).map(UUID::toString));
        final NonDefaultDayData secondNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.plusDays(2).format(DATE_TIME_FORMAT), of(DURATION), of(randomUUID().toString()), of(2), of("BAHOO2"), of("PM"), of(courtCentreId).map(UUID::toString), of(roomId).map(UUID::toString));

        final List<NonDefaultDayData> nonDefaultDays = asList(firstNonDefaultDayData, secondNonDefaultDayData);

        final String endDate = startDate.plusDays(2).toString();
        final Boolean hasVideoLink = true;
        final String publicListNote = "publicListNote";

        return new UpdatedHearingData(hearingId, courtCentreId, RandomGenerator.STRING.next(), roomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES, null, null, null, hasVideoLink, publicListNote, false, null, null);
    }

    private static UpdatedHearingData updatedHearingDataForAllocationForDefendant(final UUID hearingId, final HearingsData hearingsData) {

        final UUID courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId();
        final UUID roomId = randomUUID();

        final LocalDate startDate = hearingsData.getHearingData().get(0).getHearingStartDate();
        final ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, DEFAULT_START_TIME, UTC);

        final NonDefaultDayData firstNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION), of(COURT_SCHEDULE_ID), of(1), of(OUCODE), of(SESSION), of(courtCentreId).map(UUID::toString), of(roomId).map(UUID::toString));
        final NonDefaultDayData secondNonDefaultDayData = new NonDefaultDayData(startTimeWithZone.plusDays(2).format(DATE_TIME_FORMAT), of(DURATION), of(randomUUID().toString()), of(2), of("BAHOO2"), of("PM"), of(courtCentreId).map(UUID::toString), of(roomId).map(UUID::toString));

        final List<NonDefaultDayData> nonDefaultDays = asList(firstNonDefaultDayData);

        final String endDate = startDate.plusDays(2).toString();
        final Optional<String>  splitHearing = Optional.of("unallocated");
        final HearingTypeData hearingTypeData = new HearingTypeData(hearingsData.getHearingData().get(0).getHearingTypeData().getTypeId(), hearingsData.getHearingData().get(0).getHearingTypeData().getTypeDescription(), hearingsData.getHearingData().get(0).getHearingTypeData().getWelshDescription());

        final ProsecutionCases.Builder builder = ProsecutionCases.prosecutionCases().withCaseId(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId())
                .withDefendants(asList(Defendants.defendants()
                        .withDefendantId(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getDefendantId())
                        .withOffences(asList(Offences.offences()
                                .withOffenceId(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId()).build())).build()));

        return new UpdatedHearingData(hearingId, courtCentreId, RandomGenerator.STRING.next(), roomId, hearingTypeData,
                startDate.toString(), endDate, nonDefaultDays,
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, Collections.emptyList(), JURISDICTION_TYPE_MAGISTRATES, null, null, null, null, null, false, asList(builder.build()), splitHearing.get());
    }

    public static UpdatedHearingData updatedHearingDataForPublicListNote(final HearingData hearingData, final Boolean hasVideoLink, final String publicListNote) {

        final LocalDate startDate = nextOrSameWorkingDay(LocalDate.now());

        final List<String> nonSittingDays = Collections.singletonList(startDate.plusDays(1).toString());

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), RandomGenerator.STRING.next(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(), Collections.emptyList(),
                nonSittingDays, HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), JURISDICTION_TYPE_CROWN, null, null, hearingData.getWeekCommencingDuration(), hasVideoLink, publicListNote, false, null, null);
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
        final List<String> nonSittingDays = Collections.singletonList(startDate.plusDays(1).toString());
        final UUID courtRoomId = randomUUID();
        final UUID courtCentreId = randomUUID();


        final List<NonDefaultDayData> nonDefaultDays = Collections.singletonList(new NonDefaultDayData(startTimeWithZone.toString(), of(15), of(courtCentreId).map(UUID::toString), of(courtRoomId).map(UUID::toString)));

        return new UpdatedHearingData(hearingId, courtCentreId, "Carmarthen Magistrates Court", courtRoomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES, null, null, null, null, null, false, null, null);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithNonDefaultDays(final UUID hearingId, final List<JudicialRoleData> judiciary, UUID courtCentreId, UUID courtRoomId) {
        final String endDate = "2020-04-23";
        final LocalDate startDate = LocalDate.parse(endDate);
        final ZonedDateTime startTimeWithZone = ZonedDateTime.parse("2020-04-23T11:32:41.587Z");
        final List<String> nonSittingDays = Collections.singletonList(startDate.plusDays(1).toString());


        final List<NonDefaultDayData> nonDefaultDays = Collections.singletonList(new NonDefaultDayData(startTimeWithZone.toString(), of(15), of(courtCentreId).map(UUID::toString), of(courtRoomId).map(UUID::toString)));

        return new UpdatedHearingData(hearingId, courtCentreId, "Carmarthen Magistrates Court", courtRoomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES, null, null, null, null, null, true, null, null);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithNonDefaultDaysWithoutCourtRoomSelection(final UUID hearingId, final UUID courtCentreId) {
        final String endDate = LocalDate.now().toString();
        final LocalDate startDate = LocalDate.parse(endDate);
        final ZonedDateTime startTimeWithZone = ZonedDateTime.parse("2020-04-23T11:32:41.587Z");

        final List<NonDefaultDayData> nonDefaultDays = Collections.singletonList(new NonDefaultDayData(startTimeWithZone.toString(), of(15), of(courtCentreId).map(UUID::toString), null));

        return new UpdatedHearingData(hearingId, courtCentreId, "Worcester Crown Court", null, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                null, HEARING_LANGUAGE_ENGLISH, null, JURISDICTION_TYPE_CROWN, null, null, null, null, null, false, null, null);
    }

    private static UpdatedHearingData updatedHearingDataForAllocationWithNonDefaultDaysWithAdditionalFields(final UUID hearingId, final List<JudicialRoleData> judiciary) {
        final String endDate = "2020-04-23";
        final LocalDate startDate = LocalDate.parse(endDate);
        final ZonedDateTime startTimeWithZone = ZonedDateTime.parse("2020-04-23T11:32:41.587Z");
        final List<String> nonSittingDays = Collections.singletonList(startDate.plusDays(1).toString());
        final UUID courtRoomId = randomUUID();
        final UUID courtCentreId = randomUUID();


        final List<NonDefaultDayData> nonDefaultDays = Collections.singletonList(new NonDefaultDayData(startTimeWithZone.toString(), of(15), of(courtCentreId).map(UUID::toString), of(courtRoomId).map(UUID::toString)));

        return new UpdatedHearingData(hearingId, courtCentreId, "Carmarthen Magistrates Court", courtRoomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES, null, null, null, null, null, "High", "Video", asList("RVC", "GSN"), false, null);
    }

    public static UpdatedHearingData updatedHearingData(final HearingData hearingData) {

        //changed values
        final LocalDate startDate = LocalDate.now().plusDays(21);
        final LocalTime startTime = LocalTime.of(10, 0);
        final String endDate = startDate.toString();
        final UUID courtRoomId = randomUUID();
        final UUID courtCentreId = hearingData.getCourtCentreId();
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(false), UUID.randomUUID(), UUID.randomUUID(), new JudicialRoleTypeData(Optional.of(randomUUID()), "CIRCUIT_JUDGE")));

        final ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, startTime, UTC);
        final List<NonDefaultDayData> nonDefaultDays = Collections.singletonList(new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION), of(courtCentreId).map(UUID::toString), of(courtRoomId).map(UUID::toString)));
        return new UpdatedHearingData(hearingData.getId(), courtCentreId, hearingData.getName(), courtRoomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, judiciary, hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getPublicListNote(), false, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithVideoLink(final UpdatedHearingData hearingData) {

        //changed values

        final String videoLinkDetails = "videoLinkChanged";
        return new UpdatedHearingData(hearingData.getHearingId(), hearingData.getCourtCentreId(), hearingData.getName(), hearingData.getCourtRoomId(), SENTENCE_HEARING_TYPE,
                hearingData.getStartDate(), hearingData.getEndDate(), hearingData.getNonDefaultDays(),
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), videoLinkDetails, false, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithoutVideoLink(final UpdatedHearingData hearingData) {

        //changed values
        final Boolean hasVideoLink = null;
        final String videoLinkDetails = "";
        return new UpdatedHearingData(hearingData.getHearingId(), hearingData.getCourtCentreId(), hearingData.getName(), hearingData.getCourtRoomId(), SENTENCE_HEARING_TYPE,
                hearingData.getStartDate(), hearingData.getEndDate(), hearingData.getNonDefaultDays(),
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hasVideoLink, videoLinkDetails, false, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithNoCourtRoom(final HearingData hearingData) {

        //changed values
        final UUID courtRoomId = null;
        final UUID courtCentreId = hearingData.getCourtCentreId();

        return new UpdatedHearingData(hearingData.getId(), courtCentreId, hearingData.getName(), courtRoomId, hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(),
                Collections.singletonList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(DURATION), of(courtCentreId).map(UUID::toString), ofNullable(courtRoomId).map(UUID::toString))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getPublicListNote(), false, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithNoEndDate(final HearingData hearingData) {

        //changed values
        final String endDate = null;

        final UUID courtRoomId = hearingData.getCourtRoomId();
        final UUID courtCentreId = hearingData.getCourtCentreId();

        return new UpdatedHearingData(hearingData.getId(), courtCentreId, hearingData.getName(), courtRoomId, hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), endDate,
                Collections.singletonList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(DURATION), of(courtCentreId).map(UUID::toString), of(courtRoomId).map(UUID::toString))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getPublicListNote(), false, null, null);

    }

    public static UpdatedHearingData updatedHearingDataDifferentJudiciary(final HearingData hearingData) {

        //changed values
        final List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), UUID.randomUUID(), new JudicialRoleTypeData(Optional.of(randomUUID()), "MAGISTRATE")));

        final UUID courtRoomId = hearingData.getCourtRoomId();
        final UUID courtCentreId = hearingData.getCourtCentreId();


        return new UpdatedHearingData(hearingData.getId(), courtCentreId, hearingData.getName(), courtRoomId, hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), ofNullable(hearingData.getHearingEndDate()).map(LocalDate::toString).orElse(null),
                Collections.singletonList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(courtCentreId).map(UUID::toString), ofNullable(courtRoomId).map(UUID::toString))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, judiciary, hearingData.getJurisdictionType(), null, null, null, hearingData.getHasVideoLink(), hearingData.getPublicListNote(), false, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithWeekCommencingDate(final HearingData hearingData, final String weekCommencingStartDate, final String weekCommencingEndDate, final int weekCommencingDurationInWeeks) {
        final UUID courtRoomId = randomUUID();
        final UUID courtCentreId = hearingData.getCourtCentreId();

        return new UpdatedHearingData(hearingData.getId(), courtCentreId, hearingData.getName(), courtRoomId, hearingData.getHearingTypeData(),
                null, null,
                Collections.singletonList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(1), of(courtCentreId).map(UUID::toString), ofNullable(courtRoomId).map(UUID::toString))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingData.getHasVideoLink(), hearingData.getPublicListNote(), false, null, null);

    }

    public static UpdatedHearingData updatedHearingDataWithWeekCommencingDate(final HearingData hearingData, final LocalDate weekCommencingStartDate, final LocalDate weekCommencingEndDate, final int weekCommencingDurationInWeeks) {

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getName(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                null, null,
                Collections.singletonList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT), of(1), of(hearingData.getCourtCentreId()).map(UUID::toString), ofNullable(hearingData.getCourtRoomId()).map(UUID::toString))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType(), weekCommencingStartDate.toString(), weekCommencingEndDate.toString(), weekCommencingDurationInWeeks, hearingData.getHasVideoLink(), hearingData.getPublicListNote(), false, null, null);

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

    public String getPublicListNote() {
        return publicListNote;
    }

    public String getPanel() {
        return panel;
    }

    public String getPriority() {
        return priority;
    }

    public String getBookingType() {
        return bookingType;
    }

    public List<String> getSpecialRequirements() {
        return specialRequirements;
    }

    public Boolean isSendNotificationToParties() {
        return sendNotificationToParties;
    }

    public String getSplitHearing() {
        return splitHearing;
    }
}
