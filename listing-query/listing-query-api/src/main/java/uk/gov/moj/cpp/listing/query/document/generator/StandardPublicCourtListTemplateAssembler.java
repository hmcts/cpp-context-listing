package uk.gov.moj.cpp.listing.query.document.generator;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.ORGANISATION;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON;
import static uk.gov.moj.cpp.listing.domain.CourtListType.BENCH;
import static uk.gov.moj.cpp.listing.domain.CourtListType.STANDARD;
import static uk.gov.moj.cpp.listing.domain.utils.JsonUtils.getString;
import static uk.gov.moj.cpp.listing.domain.utils.ZonedDateTimeFormatter.adjustDateTime;
import static uk.gov.moj.cpp.listing.query.document.generator.courtlist.Offence.offence;
import static uk.gov.moj.cpp.listing.query.document.generator.courtlist.ReportingRestriction.reportingRestriction;
import static uk.gov.moj.cpp.listing.query.document.generator.courtlist.Timeslot.timeslot;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.WelshMonth;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Address;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Counsel;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.CourtRoom;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Defendant;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Hearing;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.HearingDate;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Offence;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.ReportingRestriction;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.StandardCourtList;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Timeslot;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.validation.constraints.NotNull;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00107", "squid:S1132", "squid:S1602", "squid:S1067", "pmd:NullAssignment","squid:S3358","squid:MethodCyclomaticComplexity"})
public class StandardPublicCourtListTemplateAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardPublicCourtListTemplateAssembler.class);

    private static final String HEARING_DATE = "hearingDate";
    private static final String START_TIME = "startTime";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String PROSECUTOR = "prosecutor";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String HEARINGS_BY_HEARING_DATE = "hearingsByHearingDate";
    private static final String HEARINGS_BY_COURT_CENTRE_ID = "hearingsByCourtCentreId";
    private static final String DEFENDANTS = "defendants";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String HEARINGS = "hearings";
    private static final String JUDICIARY = "judiciary";
    private static final String JUDICIAL_ID = "judicialId";
    private static final String ID = "id";
    private static final String JUDICIARIES = "judiciaries";
    private static final String COLON = ":";
    private static final String SPACE = " ";
    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DecimalFormat START_TIME_FORMAT = new DecimalFormat("00");
    private static final String TITLE_PREFIX_WELSH = "titlePrefixWelsh";
    private static final String SURNAME = "surname";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String NATIONALITY_DESCRIPTION = "nationalityDescription";
    private static final String AUTHORITY_CODE = "authorityCode";
    private static final String PROSECUTOR_CODE = "prosecutorCode";
    private static final String OFFENCE_WORDING = "offenceWording";
    private static final String TITLE = "title";
    private static final String STATEMENT_OF_OFFENCE = "statementOfOffence";
    private static final String OFFENCES = "offences";
    private static final String LISTED_CASES = "listedCases";
    private static final String SEQUENCE = "sequence";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String MIDDLE_NAME = "middleName";
    private static final String BLANK_STRING = "";
    private static final String HEARING = "hearing";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String TYPE = "type";
    private static final String DESCRIPTION = "description";
    private static final String TITLE_JUDICIARY_PREFIX_WELSH = "titleJudiciaryPrefixWelsh";
    private static final String TITLE_SUFFIX_WELSH = "titleSuffixWelsh";
    private static final String WELSH_TITLE = "welshTitle";
    private static final String REPORTING_RESTRICTION_REASON = "reportingRestrictionReason";
    private static final String WELSH_REPORTING_RESTRICTION_REASON = "welshReportingRestrictionReason";
    private static final String RESTRICT_FROM_COURT_LIST = "restrictFromCourtList";
    private static final String DEFENDANT = "Defendant";
    private static final String HEARING_STRING = "Hearing";
    private static final String ADJOURNED_HEARING_DATE = "adjournedFromDate";
    private static final String ADDRESS = "address";
    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String APPLICATION_REFERENCE = "applicationReference";
    private static final String APPLICANT = "applicant";
    private static final String COURT_APPLICATION_PARTY_TYPE = "courtApplicationPartyType";
    private static final String RESTRICT_COURT_APPLICATION_TYPE = "restrictCourtApplicationType";
    private static final String APPLICATION_TYPE = "applicationType";
    private static final String RESPONDENTS = "respondents";
    private static final String APPLICATION_PARTICULARS = "applicationParticulars";
    private static final String SHADOWLISTED ="shadowListed";
    private static final String PROSECUTION_COUNSELS = "prosecutionCounsels";
    private static final String DEFENCE_COUNSELS = "defenceCounsels";
    private static final String REPORTING_RESTRICTIONS = "reportingRestrictions";
    private static final String LABEL = "label";

    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JudiciaryNameMapper judiciaryNameMapper;


    public Optional<JsonObject> assemble(JsonEnvelope envelope, final String courtCentreId, final String courtRoomId, final CourtListType courtListType, final boolean restricted) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} CourtList courtCentreId:{}, payload:{}", courtListType, courtCentreId, envelope.toObfuscatedDebugString());
        }

        if (!payload.getJsonArray(HEARINGS).isEmpty()) {
            LOGGER.info(" found hearings {}", payload);

            final CourtCentreDetails courtCentreDetails = courtCentreFactory.getCourtCentre(fromString(courtCentreId), envelope);

            final boolean restrictedListRequired = STANDARD.equals(courtListType) || BENCH.equals(courtListType) ? restricted : TRUE;
            final String listType = courtListType.toString().toLowerCase();
            final Map<String, String> hearingTypesIdWelshDescriptionMap = referenceDataService.getHearingTypesIdWelshDescriptionMap(envelope);
            final Comparator<HearingDate> hearingDateComparator = comparing(h -> parse(h.getHearingDate()));

            final Optional<JsonObject> courtListTemplateData = payload.getJsonArray(HEARINGS).getValuesAs(JsonObject.class).stream()
                    .filter(hearingsByCourtCentre -> nonNull(hearingsByCourtCentre.getJsonArray(HEARINGS_BY_COURT_CENTRE_ID)))
                    .map(hearingsByCourtCentre -> hearingsByCourtCentre.getJsonArray(HEARINGS_BY_COURT_CENTRE_ID).getValuesAs(JsonObject.class).stream()
                            .map(hearingByCourtCentreId -> {
                                final LocalDate hearingDate = LocalDates.from(hearingByCourtCentreId.getString(HEARING_DATE));
                                final Optional<JsonObject> referenceDataJudiciariesJsonObject = retrieveReferenceDataForJudiciary(hearingsByCourtCentre, envelope);

                                final List<CourtRoom> courtRooms = createCourtRoomsList(courtCentreDetails, courtRoomId, hearingByCourtCentreId, referenceDataJudiciariesJsonObject.orElse(null), restrictedListRequired, courtListType,  hearingTypesIdWelshDescriptionMap);
                                return courtRooms.isEmpty() ? null : HearingDate.hearingDate()
                                        .withHearingDate(hearingDate.toString())
                                        .withHearingDateWelsh(createWelshHearingDate(hearingDate))
                                        .withCourtRooms(courtRooms)
                                        .build();
                            })
                            .filter(Objects::nonNull)
                            .sorted(hearingDateComparator)
                            .collect(toList()))
                    .map(hearingDates -> objectToJsonObjectConverter.convert(createStandardCourtList(courtCentreDetails, hearingDates, listType)))
                    .findFirst();


            LOGGER.info(" assembled courtListTemplateData: {}", courtListTemplateData);
            return courtListTemplateData;
        }
        return empty();
    }

    private String createWelshHearingDate(LocalDate hearingDate) {
        String welshDate = BLANK_STRING;
        final Optional<WelshMonth> welshMonth = WelshMonth.valueFor(hearingDate.getMonth());
        if (welshMonth.isPresent()) {
            welshDate = hearingDate.getDayOfMonth() + SPACE
                    + capitalize(lowerCase(welshMonth.get().name())) + SPACE
                    + hearingDate.getYear();
        }
        return welshDate;

    }

    private Optional<JsonObject> retrieveReferenceDataForJudiciary(JsonObject hearingsByCourtCentre, JsonEnvelope envelope) {
        final List<UUID> judiciaryIdList = hearingsByCourtCentre.getJsonArray(JUDICIARY).getValuesAs(JsonObject.class).stream()
                .map(judiciary -> fromString(judiciary.getString(JUDICIAL_ID)))
                .collect(toList());

        if (!judiciaryIdList.isEmpty()) {
            final JsonEnvelope referenceDataJudiciaries = referenceDataService.getJudiciariesByIdList(judiciaryIdList, envelope);
            return of(referenceDataJudiciaries.payloadAsJsonObject());
        }
        return empty();
    }


    private List<CourtRoom> createCourtRoomsList(final CourtCentreDetails courtCentre, final String selectedCourtRoomId, final JsonObject hearingByCourtCentreId,
                                                 @Nullable final JsonObject referenceDataJudiciariesJo, final boolean restrictedListRequired, final CourtListType courtListType, final Map<String, String> hearingTypesIdWelshDescriptionMap) {

        final LocalDate hearingDate = LocalDates.from(hearingByCourtCentreId.getString(HEARING_DATE));

        final Map<String, List<JsonObject>> hearingsByCourtRoomIdMap = new HashMap<>();

        hearingByCourtCentreId.getJsonArray(HEARINGS_BY_HEARING_DATE).getValuesAs(JsonObject.class).stream()
                .filter(hearingByDate -> nonNull(hearingByDate) && hearingByDate.size() > 0)
                .map(hearingByDate -> hearingByDate.getJsonObject(HEARING))
                .filter(hearing -> (hearing.containsKey(LISTED_CASES) && !hearing.getJsonArray(LISTED_CASES).isEmpty()) || !hearing.getJsonArray(COURT_APPLICATIONS).isEmpty())
                .forEach(hearing -> {
                    final Set<String> courtRoomIds = hearing.getJsonArray(HEARING_DAYS).getValuesAs(JsonObject.class).stream()
                            .filter(hearingDay -> isBlank(getString(hearingDay, COURT_CENTRE_ID)) || courtCentre.getId().equals(fromString(hearingDay.getString(COURT_CENTRE_ID))))
                            .filter(hearingDay -> isNotBlank(getString(hearingDay, COURT_ROOM_ID)))
                            .map(hearingDay -> hearingDay.getString(COURT_ROOM_ID))
                            .collect(Collectors.toSet());
                    courtRoomIds.add(hearing.getString(COURT_ROOM_ID));
                    courtRoomIds.stream()
                            .filter(courtRoomId -> ofNullable(courtCentre.getCourtRooms().get(fromString(courtRoomId))).isPresent())
                            .forEach(courtRoomId -> hearingsByCourtRoomIdMap.computeIfAbsent(courtRoomId, k -> new ArrayList<>()).add(hearing));
                });

        return hearingsByCourtRoomIdMap.keySet().stream()
                .filter(courtRoomId -> selectedCourtRoomId == null || selectedCourtRoomId.equals(courtRoomId))
                .map(courtRoomId -> createCourtRoom(hearingsByCourtRoomIdMap.get(courtRoomId), courtCentre.getCourtRooms().get(fromString(courtRoomId)), referenceDataJudiciariesJo, hearingDate, restrictedListRequired, courtListType, hearingTypesIdWelshDescriptionMap))
                .sorted(comparing(CourtRoom::getCourtRoomName))
                .collect(toList());
    }

    private CourtRoom createCourtRoom(final List<JsonObject> hearingsByCourtRoom, final CourtRoomDetails courtRoomDetails, @Nullable final JsonObject referenceDataJudiciariesJo,
                                      final LocalDate hearingDate, final boolean restrictedListRequired,final CourtListType courtListType,  final Map<String, String> hearingTypesIdWelshDescriptionMap) {
        final Map<LocalDateTime, List<Hearing>> unsortedListMultimap = new HashMap<>();

        final String judiciaryNamesWithCommas = nonNull(referenceDataJudiciariesJo) ? createJudiciaryNames(hearingsByCourtRoom, referenceDataJudiciariesJo) : BLANK_STRING;

        final String judiciaryNamesWelshWithCommas = nonNull(referenceDataJudiciariesJo) ? createJudiciaryNamesWelsh(hearingsByCourtRoom, referenceDataJudiciariesJo) : BLANK_STRING;

        hearingsByCourtRoom.forEach(hearingJson -> {
            final List<JsonObject> hearingDays = hearingJson.getJsonArray(HEARING_DAYS).getValuesAs(JsonObject.class).stream()
                    .filter(hd -> hearingDate.equals(ZonedDateTime.parse(hd.getString(START_TIME)).toLocalDate()))
                    .filter(hd -> isBlank(getString(hd, COURT_ROOM_ID)) || courtRoomDetails.getId().equals(fromString(hd.getString(COURT_ROOM_ID))))
                    .collect(toList());

            hearingDays.stream().forEach(hearingDay -> {
                final ZonedDateTime startTimestamp = adjustDateTime(ZonedDateTimes.fromString(hearingDay.getString(START_TIME)));
                final Integer sequence = hearingDay.getInt(SEQUENCE);

                final String hearingStartTime = START_TIME_FORMAT.format(startTimestamp.getHour()) + COLON + START_TIME_FORMAT.format(startTimestamp.getMinute());

                arrangeHearingsByStartTime(unsortedListMultimap, hearingJson, startTimestamp, hearingStartTime, sequence, restrictedListRequired, courtListType, hearingTypesIdWelshDescriptionMap);
            });
        });

        final Map<LocalDateTime, List<Hearing>> sortedListMultimap = new TreeMap<>();

        unsortedListMultimap.forEach((key, value) -> {
            final List<Hearing> hearingsRow = value.stream()
                    .sorted(comparing(Hearing::getSequence))
                    .collect(toList());
            sortedListMultimap.put(key, hearingsRow);
        });

        final List<Timeslot> timeslots = sortedListMultimap.keySet().stream().map(key -> timeslot()
                .withHearings(sortedListMultimap.get(key))
                .build())
                .filter(timeslot -> isNotEmpty(timeslot.getHearings()))
                .collect(toList());

        return CourtRoom.courtRoom()
                .withCourtRoomName(courtRoomDetails.getCourtRoomName())
                .withWelshCourtRoomName(courtRoomDetails.getWelshCourtRoomName())
                .withJudiciaryNames(judiciaryNamesWithCommas)
                .withWelshJudiciaryNames(judiciaryNamesWelshWithCommas)
                .withTimeslots(timeslots)
                .build();
    }

    private void arrangeHearingsByStartTime(final Map<LocalDateTime, List<Hearing>> unsortedListMultimap, final JsonObject hearingJson, final ZonedDateTime startTimestamp, final String hearingStartTime, final Integer sequence, final boolean restrictedListRequired, final CourtListType courtListType,  final Map<String, String> hearingTypesIdWelshDescriptionMap) {
        if (hearingJson.containsKey(LISTED_CASES)) {
            final List<Hearing> hearings = hearingJson.getJsonArray(LISTED_CASES).getValuesAs(JsonObject.class).stream()
                    .map(listedCase -> createHearingFromListedCase(hearingJson, hearingStartTime, sequence, listedCase, restrictedListRequired, courtListType, hearingTypesIdWelshDescriptionMap))
                    .filter(hearing -> isNotEmpty(hearing.getDefendants()))
                    .collect(toList());
            unsortedListMultimap.computeIfAbsent(startTimestamp.toLocalDateTime(), k -> new ArrayList<>()).addAll(hearings);
        }
        if (hearingJson.containsKey(COURT_APPLICATIONS) && !hearingJson.getJsonArray(COURT_APPLICATIONS).isEmpty()) {
            final List<Hearing> hearings = hearingJson.getJsonArray(COURT_APPLICATIONS).getValuesAs(JsonObject.class).stream()
                    .map(courtApplication -> createHearingFromCourtApplication(hearingJson, hearingStartTime, sequence, courtApplication, restrictedListRequired, hearingTypesIdWelshDescriptionMap))
                    .collect(toList());
            unsortedListMultimap.computeIfAbsent(startTimestamp.toLocalDateTime(), k -> new ArrayList<>()).addAll(hearings);
        }
    }

    private String createJudiciaryNamesWelsh(final List<JsonObject> hearingsByCourtRoom, final JsonObject judiciariesJsonObject) {
        final List<String> courtRoomJudiciaryIds = getJudicialIds(hearingsByCourtRoom);
        final List<String> judiciaryNames = judiciariesJsonObject.getJsonArray(JUDICIARIES).getValuesAs(JsonObject.class).stream()
                .filter(j -> courtRoomJudiciaryIds.contains(j.getString(ID)))
                .sorted(comparing(jo -> jo.getString(SURNAME)))
                .map(j -> j.getString(TITLE_PREFIX_WELSH, BLANK_STRING) + SPACE + j.getString(TITLE_JUDICIARY_PREFIX_WELSH, BLANK_STRING) + SPACE + j.getString(SURNAME, BLANK_STRING) + (j.getString(TITLE_SUFFIX_WELSH, BLANK_STRING).equals(BLANK_STRING) ? BLANK_STRING : SPACE + j.getString(TITLE_SUFFIX_WELSH)))
                .collect(toList());
        return String.join(", ", judiciaryNames);
    }

    private String createJudiciaryNames(final List<JsonObject> hearingsByCourtRoom, final JsonObject judiciariesJsonObject) {
        final List<String> courtRoomJudiciaryIds = getJudicialIds(hearingsByCourtRoom);
        final List<String> judiciaryNames = judiciariesJsonObject.getJsonArray(JUDICIARIES).getValuesAs(JsonObject.class).stream()
                .filter(j -> courtRoomJudiciaryIds.contains(j.getString(ID)))
                .sorted(comparing(jo -> jo.getString(SURNAME)))
                .map(j -> judiciaryNameMapper.getName(j))
                .collect(toList());
        return String.join(", ", judiciaryNames);
    }

    private List<String> getJudicialIds(List<JsonObject> hearingsByCourtRoom) {
        return hearingsByCourtRoom.stream()
                .map(hearingByCourtRoom -> hearingByCourtRoom.getJsonArray(JUDICIARY).getValuesAs(JsonObject.class).stream()
                        .map(j -> j.getString(JUDICIAL_ID))
                        .collect(toList()))
                .flatMap(List::stream)
                .collect(toList());
    }

    private StandardCourtList createStandardCourtList(CourtCentreDetails courtCentre, List<HearingDate> hearingDates, String courtListType) {

        return StandardCourtList.standardCourtList()
                .withListType(courtListType)
                .withCourtCentreName(courtCentre.getCourtCentreName())
                .withWelshCourtCentreName(blankStringIfNull(courtCentre.getWelshCourtCentreName()))
                .withCourtCentreAddress1(blankStringIfNull(courtCentre.getAddress1()) + SPACE + blankStringIfNull(courtCentre.getAddress2()))
                .withWelshCourtCentreAddress1(blankStringIfNull(courtCentre.getWelshAddress1()) + SPACE + blankStringIfNull(courtCentre.getWelshAddress2()))
                .withCourtCentreAddress2(blankStringIfNull(courtCentre.getAddress3()) + SPACE + blankStringIfNull(courtCentre.getAddress4()) + SPACE + blankStringIfNull(courtCentre.getAddress5()) + SPACE + blankStringIfNull(courtCentre.getPostcode()))
                .withWelshCourtCentreAddress2(blankStringIfNull(courtCentre.getWelshAddress3()) + SPACE + blankStringIfNull(courtCentre.getWelshAddress4()) + SPACE + blankStringIfNull(courtCentre.getWelshAddress5()) + SPACE + blankStringIfNull(courtCentre.getPostcode()))
                .withHearingDates(hearingDates)
                .build();
    }

    private String blankStringIfNull(String stringToCheck) {
        return nonNull(stringToCheck) ? stringToCheck : BLANK_STRING;
    }

    private Hearing createHearingFromListedCase(final JsonObject hearingJson, final String hearingStartTime, final Integer sequence,
                                                final JsonObject listedCase, final boolean restrictedListRequired, final CourtListType courtListType, final Map<String, String> hearingTypesIdWelshDescriptionMap) {
        final String hearingType = hearingJson.getJsonObject(TYPE).getString(DESCRIPTION);
        final String hearingWelshType =  hearingTypesIdWelshDescriptionMap.get(hearingJson.getJsonObject(TYPE).getString(ID));
        final String reportingRestrictionReason = hearingJson.getString(REPORTING_RESTRICTION_REASON, BLANK_STRING);
        final String welshReportingRestrictionReason = hearingJson.getString(WELSH_REPORTING_RESTRICTION_REASON, BLANK_STRING);
        final boolean restrictedByCase = listedCase.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE);
        final boolean caseRestricted = restrictedByCase && isRestricted(restrictedListRequired, courtListType, listedCase);
        final String adjournedHearingDate = hearingJson.getString(ADJOURNED_HEARING_DATE, BLANK_STRING);
        return Hearing.hearing()
                .withCaseNumber(caseRestricted ? EMPTY : listedCase.getJsonObject(CASE_IDENTIFIER).getString(CASE_REFERENCE))
                .withCaseId(caseRestricted ? null : fromString(listedCase.getString(ID)))
                .withHearingType(caseRestricted ? HEARING_STRING : hearingType)
                .withWelshHearingType(caseRestricted ? HEARING_STRING : (StringUtils.isEmpty(hearingWelshType) ? hearingType : hearingWelshType))
                .withProsecutorType(caseRestricted ? EMPTY : getProsecutorType(listedCase))
                .withSequence(sequence)
                .withReportingRestrictionReason(caseRestricted ? EMPTY : reportingRestrictionReason)
                .withWelshReportingRestrictionReason(caseRestricted ? EMPTY : welshReportingRestrictionReason)
                .withDefendants(caseRestricted ? emptyList() : createDefendantsFromListedCase(hearingJson, listedCase, restrictedListRequired, courtListType))
                .withStartTime(hearingStartTime)
                .withAdjournedHearingDate(adjournedHearingDate)
                .withId(fromString(hearingJson.getString(ID)))
                .withPanel(hearingJson.getString("panel", null))
                .build();
    }

    private String getProsecutorType(final JsonObject listedCase) {
        if(listedCase.containsKey(PROSECUTOR)){
            return listedCase.getJsonObject(PROSECUTOR).getString(PROSECUTOR_CODE);
        }
        return listedCase.getJsonObject(CASE_IDENTIFIER).getString(AUTHORITY_CODE);
    }

    private Hearing createHearingFromCourtApplication(final JsonObject hearingJson, final String hearingStartTime, final Integer sequence,
                                                      final JsonObject courtApplication, final boolean restrictedListRequired, final Map<String, String> hearingTypesIdWelshDescriptionMap) {
        final String hearingType = hearingJson.getJsonObject(TYPE).getString(DESCRIPTION);
        final String hearingWelshType = hearingTypesIdWelshDescriptionMap.get(hearingJson.getJsonObject(TYPE).getString(ID));
        final String reportingRestrictionReason = hearingJson.getString(REPORTING_RESTRICTION_REASON, BLANK_STRING);
        final String welshReportingRestrictionReason = hearingJson.getString(WELSH_REPORTING_RESTRICTION_REASON, BLANK_STRING);
        final boolean restrictedByCase = courtApplication.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE);
        final boolean caseRestricted = restrictedByCase && restrictedListRequired;
        final String adjournedHearingDate = hearingJson.getString(ADJOURNED_HEARING_DATE, BLANK_STRING);
        return Hearing.hearing()
                .withCaseNumber(caseRestricted ? EMPTY : courtApplication.getString(APPLICATION_REFERENCE))
                .withHearingType(caseRestricted ? HEARING_STRING : hearingType)
                .withWelshHearingType(caseRestricted ? HEARING_STRING : (StringUtils.isEmpty(hearingWelshType) ? hearingType : hearingWelshType))
                .withProsecutorType(caseRestricted ? EMPTY : createProsecutorNameFromCourtApplication(courtApplication))
                .withSequence(sequence)
                .withReportingRestrictionReason(caseRestricted ? EMPTY : reportingRestrictionReason)
                .withWelshReportingRestrictionReason(caseRestricted ? EMPTY : welshReportingRestrictionReason)
                .withDefendants(caseRestricted ? emptyList() : createDefendantsEquivalentFromCourtApplication(courtApplication, restrictedListRequired))
                .withStartTime(hearingStartTime)
                .withAdjournedHearingDate(adjournedHearingDate)
                .withId(fromString(hearingJson.getString(ID)))
                .withPanel(hearingJson.getString("panel", null))
                .withCourtApplicationId(fromString(courtApplication.getString("id")))
                .withApplicationOffences(ofNullable(courtApplication.getJsonArray(OFFENCES))
                        .map(offences ->  offences.getValuesAs(JsonObject.class).stream()
                                .map(offence -> Offence.offence()
                                        .withId(fromString(offence.getString(ID))).build())
                                .collect(toList()))
                        .orElse(null))
                .build();
    }

    private List<Defendant> createDefendantsFromListedCase(final JsonObject hearingJson, final JsonObject listedCase, final boolean restrictedListRequired, final CourtListType courtListType) {
        final AtomicInteger namePositionOnRestricted = new AtomicInteger(1);
        final long restrictedDefendantCount = listedCase.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                .filter(defendant -> isRestricted(restrictedListRequired, courtListType, defendant))
                .count();

        return listedCase.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                .map(defendant -> {
                    final String dateOfBirth = defendant.getString(DATE_OF_BIRTH, null);
                    final boolean defendantRestricted = isRestricted(restrictedListRequired, courtListType, defendant);
                    return createDefendant(hearingJson, defendant, dateOfBirth, defendantRestricted,
                            defendantRestricted ? getNameSuffixForRestrictedCase(restrictedDefendantCount, namePositionOnRestricted) : EMPTY, restrictedListRequired, courtListType, listedCase.getString(ID));
                })
                .filter(defendant -> restrictedListRequired && (STANDARD.equals(courtListType) || BENCH.equals(courtListType)) ? isNotEmpty(defendant.getOffences()) : TRUE)
                .collect(toList());
    }

    private String getNameSuffixForRestrictedCase(final long restrictedCount, final AtomicInteger namePositionOnRestricted) {
        return restrictedCount > 1 ? valueOf(namePositionOnRestricted.getAndIncrement()) : EMPTY;
    }

    private List<Defendant> createDefendantsEquivalentFromCourtApplication(final JsonObject courtApplication, final boolean restrictedListRequired) {
        if (courtApplication.containsKey(RESPONDENTS) && !courtApplication.getJsonArray(RESPONDENTS).isEmpty() &&
                courtApplication.getJsonArray(RESPONDENTS).getValuesAs(JsonObject.class).stream()
                        .anyMatch(respondent -> CourtApplicationPartyType.valueOf(respondent.getString(COURT_APPLICATION_PARTY_TYPE)) == PERSON)) {
            final AtomicInteger namePositionOnRestricted = new AtomicInteger(1);
            final long restrictedRespondentCount = courtApplication.getJsonArray(RESPONDENTS).getValuesAs(JsonObject.class).stream()
                    .filter(respondent -> respondent.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE)).count();

            return courtApplication.getJsonArray(RESPONDENTS).getValuesAs(JsonObject.class).stream()
                    .map(respondent -> {
                        final boolean respondentRestricted = respondent.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE) && restrictedListRequired;
                        return createDefendantEquivalentFromCourtApplication(courtApplication, respondent, respondentRestricted,
                                respondentRestricted ? getNameSuffixForRestrictedCase(restrictedRespondentCount, namePositionOnRestricted) : EMPTY, restrictedListRequired);
                    })
                    .collect(toList());
        }
        final JsonObject applicant = courtApplication.getJsonObject(APPLICANT);
        final boolean applicantRestricted = applicant.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE) && restrictedListRequired;
        return ImmutableList.of(createDefendantEquivalentFromCourtApplication(courtApplication, applicant, applicantRestricted, EMPTY, restrictedListRequired));
    }

    private boolean isShadowListed(final CourtListType courtListType, final JsonObject listedCase) {
        return  (BENCH.equals(courtListType) || STANDARD.equals(courtListType)) && listedCase.getBoolean(SHADOWLISTED, FALSE);
    }

    private boolean isPublicAndRestricted(final CourtListType courtListType, final JsonObject jsonObject) {
        return CourtListType.PUBLIC.equals(courtListType) && jsonObject.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE);
    }

    private Defendant createDefendant(final JsonObject hearingJson, final JsonObject defendant, final String dateOfBirth, final boolean defendantRestricted,
                                      final String defendantSuffix, final boolean restrictedListRequired, final CourtListType courtListType, final String caseId) {
        final Defendant.Builder builder = Defendant.defendant();
        builder.withId(fromString(defendant.getString(ID)));
        final Set<ReportingRestriction> reportingRestrictions = new HashSet<>();
        final String legalEntityDefendant = defendant.getString(ORGANISATION_NAME, BLANK_STRING);
        if (defendantRestricted) {
            if (!StringUtils.isBlank(legalEntityDefendant)) {
                builder.withOrganisationName((DEFENDANT + SPACE + defendantSuffix).trim());
            } else {
                builder.withFirstName(EMPTY)
                        .withSurname((DEFENDANT + SPACE + defendantSuffix).trim());
                builder.withDateOfBirth(EMPTY);
                builder.withAge(EMPTY);
            }
        } else {
            builder.withFirstName(defendant.getString(FIRST_NAME, BLANK_STRING))
                    .withSurname(defendant.getString(LAST_NAME, BLANK_STRING))
                    .withOrganisationName(defendant.getString(ORGANISATION_NAME, BLANK_STRING));
            if (nonNull(dateOfBirth)) {
                builder.withDateOfBirth(parse(dateOfBirth).format(DOB_FORMATTER));
                builder.withAge(valueOf(Period.between(parse(dateOfBirth), LocalDate.now()).getYears()));
            }
            builder.withNationality(defendant.getString(NATIONALITY_DESCRIPTION, BLANK_STRING));
            if (nonNull(defendant.getJsonObject(ADDRESS))) {
                builder.withAddress(buildAddress(defendant.getJsonObject(ADDRESS)));
            }
        }
        final List<Offence> offenceList = new ArrayList<>();
        defendant.getJsonArray(OFFENCES).getValuesAs(JsonObject.class)
                .forEach(offences -> {
                    reportingRestrictions.addAll(getReportingRestriction(offences)
                            .stream()
                            .filter(Objects::nonNull)
                            .map(reporting -> reportingRestriction()
                                    .withLabel(reporting.getLabel())
                                    .build())
                            .collect(Collectors.toList()));
                    final boolean offenceRestricted = isRestricted(restrictedListRequired, courtListType, offences);
                    if(! offenceRestricted) {
                        offenceList.add(createOffence(offences, offenceRestricted));
                    }
                });
        if(!offenceList.isEmpty()) {
            builder.withOffences(offenceList);
        }

        builder.withReportingRestrictions(reportingRestrictions);

        if (BENCH.equals(courtListType)) {
            addDefenceAndProsecutionCounsels(hearingJson, defendant, caseId, builder);
        }

        return  builder.build();
    }

    private void addDefenceAndProsecutionCounsels(final JsonObject hearingJson, final JsonObject defendant, final String caseId, final Defendant.Builder builder) {
        if(hearingJson.containsKey(DEFENCE_COUNSELS)) {
            builder.withDefenceCounsels(extractCounsels(hearingJson.getJsonArray(DEFENCE_COUNSELS), defendant.getString(ID), DEFENDANTS));
        }
        if(hearingJson.containsKey(PROSECUTION_COUNSELS)) {
            builder.withProsecutionCounsels(extractCounsels(hearingJson.getJsonArray(PROSECUTION_COUNSELS), caseId, PROSECUTION_CASES));
        }
    }


    private Offence createOffence(final JsonObject offence, final boolean offenceRestricted) {
        final Offence.Builder builder = Offence.offence();
        if (!offenceRestricted) {
            builder.withOffenceTitle(offence.getJsonObject(STATEMENT_OF_OFFENCE).getString(TITLE));
            builder.withWelshOffenceTitle(offence.getJsonObject(STATEMENT_OF_OFFENCE).getString(WELSH_TITLE, BLANK_STRING));
            builder.withOffenceWording(offence.getString(OFFENCE_WORDING, BLANK_STRING));
            builder.withId(fromString(offence.getString(ID)));
        }

        return builder.build();
    }

    private List<ReportingRestriction> getReportingRestriction(final JsonObject offences){
        final Set<ReportingRestriction> reportingRestrictions = new HashSet<>();
        if(offences.containsKey(REPORTING_RESTRICTIONS)){
            for (final JsonObject reportingRestriction : offences.getJsonArray(REPORTING_RESTRICTIONS).getValuesAs(JsonObject.class)) {
                reportingRestrictions.add(reportingRestriction().withLabel(reportingRestriction.getString(LABEL)).build());
            }
        }
        return isNotEmpty(reportingRestrictions) ? reportingRestrictions.stream().filter(Objects::nonNull).collect(Collectors.toList()) : emptyList();
    }

    private List<Counsel> extractCounsels(final JsonArray requestCounsels, final String id, final String type) {
        final List<Counsel> counsels = new ArrayList<>();
        for(final JsonObject requestCounsel : requestCounsels.getValuesAs(JsonObject.class)) {
            if(requestCounsel.getJsonArray(type).getValuesAs(JsonValue.class).stream().anyMatch(c -> id.equals(((JsonString)c).getString()))) {
                counsels.add(Counsel.counsel()
                        .withFirstName(requestCounsel.containsKey(FIRST_NAME) ? requestCounsel.getString(FIRST_NAME) : null)
                        .withMiddleName(requestCounsel.containsKey(MIDDLE_NAME) ? requestCounsel.getString(MIDDLE_NAME) : null)
                        .withLastName(requestCounsel.containsKey(LAST_NAME) ? requestCounsel.getString(LAST_NAME) : null)
                        .withTitle(requestCounsel.containsKey(TITLE) ? requestCounsel.getString(TITLE) : null)
                        .build());
            }
        }
        return counsels;
    }


    private boolean isRestricted(final boolean restrictedListRequired, final CourtListType courtListType, final JsonObject jsonObject) {
        return restrictedListRequired ? isPublicAndRestricted(courtListType, jsonObject) || isShadowListed(courtListType, jsonObject) : FALSE;
    }

    private Defendant createDefendantEquivalentFromCourtApplication(final JsonObject courtApplication, @NotNull final JsonObject defendantEquivalent, final boolean nameRestricted, final String namePositionOnRestricted, final boolean restrictedListRequired) {
        final Defendant.Builder builder = Defendant.defendant();
        final boolean applicationTypeRestricted = courtApplication.getBoolean(RESTRICT_COURT_APPLICATION_TYPE, FALSE) && restrictedListRequired;

        populateWithNameAndAddressFromCourtApplication(builder, defendantEquivalent, namePositionOnRestricted, nameRestricted);

        builder.withOffences(applicationTypeRestricted ? emptyList() :
                ImmutableList.of(offence()
                        .withOffenceTitle(courtApplication.getString(APPLICATION_TYPE))
                        .withOffenceWording(courtApplication.getString(APPLICATION_PARTICULARS, EMPTY))
                        .build()));

        return builder.build();
    }

    private String createProsecutorNameFromCourtApplication(final JsonObject courtApplication) {
        if (courtApplication.containsKey(RESPONDENTS) && !courtApplication.getJsonArray(RESPONDENTS).isEmpty()) {
            if (courtApplication.getJsonArray(RESPONDENTS).getValuesAs(JsonObject.class).stream()
                    .anyMatch(respondent -> CourtApplicationPartyType.valueOf(respondent.getString(COURT_APPLICATION_PARTY_TYPE)) == PERSON)) {
                final JsonObject applicant = courtApplication.getJsonObject(APPLICANT);
                return createProsecutorName(applicant);
            }
            final JsonObject respondent = courtApplication.getJsonArray(RESPONDENTS).getValuesAs(JsonObject.class).stream().findFirst().orElse(null);
            if (nonNull(respondent)) {
                return createProsecutorName(respondent);
            }
        }
        return EMPTY;
    }

    private String createProsecutorName(final JsonObject defendantEquivalent) {
        if (CourtApplicationPartyType.valueOf(defendantEquivalent.getString(COURT_APPLICATION_PARTY_TYPE)) == PERSON) {
            return format("%s, %s", upperCase(defendantEquivalent.getString(LAST_NAME)), defendantEquivalent.getString(FIRST_NAME));
        }
        return defendantEquivalent.getString(LAST_NAME);
    }

    private void populateWithNameAndAddressFromCourtApplication(final Defendant.Builder builder, @NotNull final JsonObject defendantEquivalent, final String nameSuffix, final boolean nameRestricted) {
        if (CourtApplicationPartyType.valueOf(defendantEquivalent.getString(COURT_APPLICATION_PARTY_TYPE)) == PERSON) {
            builder.withFirstName(nameRestricted ? EMPTY : defendantEquivalent.getString(FIRST_NAME, EMPTY))
                    .withSurname(nameRestricted ? (DEFENDANT + SPACE + nameSuffix).trim() : defendantEquivalent.getString(LAST_NAME));
        } else if (CourtApplicationPartyType.valueOf(defendantEquivalent.getString(COURT_APPLICATION_PARTY_TYPE)) == ORGANISATION) {
            builder.withOrganisationName(nameRestricted ? (DEFENDANT + SPACE + nameSuffix).trim() : defendantEquivalent.getString(LAST_NAME));
        }
        if (!nameRestricted && defendantEquivalent.containsKey(ADDRESS)) {
            builder.withAddress(buildAddress(defendantEquivalent.getJsonObject(ADDRESS)));
        }
        builder.withId(fromString(defendantEquivalent.getString(ID)));
    }


    private Address buildAddress(final JsonObject address) {
        return Address.address()
                .withAddress1(address.getString("address1"))
                .withAddress2(address.containsKey("address2") ? of(address.getString("address2")) : empty())
                .withAddress3(address.containsKey("address3") ? of(address.getString("address3")) : empty())
                .withAddress4(address.containsKey("address4") ? of(address.getString("address4")) : empty())
                .withAddress5(address.containsKey("address5") ? of(address.getString("address5")) : empty())
                .withPostcode(address.containsKey("postcode") ? of(address.getString("postcode")) : empty())
                .build();

    }
}
