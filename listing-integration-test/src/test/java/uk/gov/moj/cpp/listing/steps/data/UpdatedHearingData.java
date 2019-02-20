package uk.gov.moj.cpp.listing.steps.data;

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


    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final String jurisdictionType;


    public static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId) {
        List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(),"MAGISTRATE")));
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    public static UpdatedHearingData updatedHearingDataForAllocationWithoutJudiciary(final UUID hearingId) {
        List<JudicialRoleData> judiciary = Collections.emptyList();
        return updatedHearingDataForAllocation(hearingId, judiciary);
    }

    private static UpdatedHearingData updatedHearingDataForAllocation(final UUID hearingId, final List<JudicialRoleData> judiciary) {

        final LocalDate startDate = LocalDate.now();
        LocalTime startTime = LocalTime.now();
        ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, LocalTime.parse(startTime.format(dtf)), UTC);

        List<String> nonSittingDays = Arrays.asList(startDate.plusDays(1).toString());
        List<NonDefaultDayData> nonDefaultDays = Arrays.asList(new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION)));
        String endDate = startDate.plusDays(2).toString();


        return new UpdatedHearingData(hearingId, randomUUID(), randomUUID(), SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                nonSittingDays, HEARING_LANGUAGE_WELSH, judiciary, JURISDICTION_TYPE_MAGISTRATES);
    }


    public static UpdatedHearingData updatedHearingData(HearingData hearingData) {

        //changed values
        final LocalDate startDate = LocalDate.now().plusDays(21);
        LocalTime startTime = LocalTime.of(10, 0);
        String endDate = startDate.toString();
        UUID courtRoomId = randomUUID();
        List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(false), UUID.randomUUID(),  new JudicialRoleTypeData(Optional.empty(), "CIRCUIT_JUDGE")));

        ZonedDateTime startTimeWithZone = ZonedDateTime.of(startDate, startTime, UTC);
        List<NonDefaultDayData> nonDefaultDays = Arrays.asList(new NonDefaultDayData(startTimeWithZone.format(DATE_TIME_FORMAT), of(DURATION)));

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), courtRoomId, SENTENCE_HEARING_TYPE,
                startDate.toString(), endDate, nonDefaultDays,
                Collections.emptyList(), HEARING_LANGUAGE_WELSH, judiciary, hearingData.getJurisdictionType());

    }

    public static UpdatedHearingData updatedHearingDataWithNoCourtRoom(HearingData hearingData) {

        //changed values
        UUID courtRoomId = null;

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), courtRoomId, hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(),
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType());

    }

    public static UpdatedHearingData updatedHearingDataWithNoEndDate(HearingData hearingData) {

        //changed values
        String endDate = null;

        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), endDate,
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, hearingData.getJudiciary(), hearingData.getJurisdictionType());

    }

    public static UpdatedHearingData updatedHearingDataDifferentJudiciary(HearingData hearingData) {

        //changed values
        List<JudicialRoleData> judiciary = Collections.singletonList(new JudicialRoleData(of(true), of(true), UUID.randomUUID(), new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE")));


        return new UpdatedHearingData(hearingData.getId(), hearingData.getCourtCentreId(), hearingData.getCourtRoomId(), hearingData.getHearingTypeData(),
                hearingData.getHearingStartDate().toString(), hearingData.getHearingEndDate().toString(),
                Arrays.asList(new NonDefaultDayData(hearingData.getHearingStartTime().format(DATE_TIME_FORMAT))),
                Collections.emptyList(), HEARING_LANGUAGE_ENGLISH, judiciary, hearingData.getJurisdictionType());

    }

    public UpdatedHearingData(UUID hearingId, UUID courtCentreId, UUID courtRoomId, HearingTypeData type, String startDate,
                              String endDate, List<NonDefaultDayData> nonDefaultDays, List<String> nonSittingDays,
                              String hearingLanguage, List<JudicialRoleData> judiciary, String jurisdictionType) {
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

}


