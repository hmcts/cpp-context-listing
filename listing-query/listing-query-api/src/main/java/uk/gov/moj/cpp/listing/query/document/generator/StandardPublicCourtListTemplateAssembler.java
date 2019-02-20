package uk.gov.moj.cpp.listing.query.document.generator;

import static java.time.LocalDate.parse;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.WelshMonth;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.CourtRoom;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Defendant;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Hearing;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.HearingDate;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Offence;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.StandardCourtList;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Timeslot;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00107", "squid:S1132", "squid:S1602"})
public class StandardPublicCourtListTemplateAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardPublicCourtListTemplateAssembler.class);

    private static final String HEARING_DATE = "hearingDate";
    private static final String START_TIME = "startTime";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String HEARINGS_BY_HEARING_DATE = "hearingsByHearingDate";
    private static final String HEARINGS_BY_COURT_CENTRE_ID = "hearingsByCourtCentreId";
    private static final String DEFENDANTS = "defendants";
    private static final String HEARINGS = "hearings";
    private static final String JUDICIARY = "judiciary";
    private static final String JUDICIAL_ID = "judicialId";
    private static final String ID = "id";
    private static final String JUDICIARIES = "judiciaries";
    private static final String COLON = ":";
    private static final String SPACE = " ";
    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DecimalFormat START_TIME_FORMAT = new DecimalFormat("00");
    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String TITLE_PREFIX_WELSH = "titlePrefixWelsh";
    private static final String SURNAME = "surname";
    private static final String TITLE_JUDICIARY_PREFIX = "titleJudiciaryPrefix";
    private static final String TITLE_SUFFIX = "titleSuffix";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String AUTHORITY_CODE = "authorityCode";
    private static final String OFFENCE_WORDING = "offenceWording";
    private static final String TITLE = "title";
    private static final String STATEMENT_OF_OFFENCE = "statementOfOffence";
    private static final String OFFENCES = "offences";
    private static final String LISTED_CASES = "listedCases";
    private static final String SEQUENCE = "sequence";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String BLANK_STRING = "";
    private static final String HEARING = "hearing";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String TYPE = "type";
    private static final String DESCRIPTION = "description";
    private static final String TITLE_JUDICIARY_PREFIX_WELSH = "titleJudiciaryPrefixWelsh";
    private static final String TITLE_SUFFIX_WELSH = "titleSuffixWelsh";
    private static final String WELSH_TITLE = "welshTitle";
    private static final String WELSH = "Welsh";
    private static final String REPORTING_RESTRICTION_REASON = "reportingRestrictionReason";
    private static final String WELSH_REPORTING_RESTRICTION_REASON = "welshReportingRestrictionReason";


    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    public Optional<JsonObject> assemble(JsonEnvelope envelope, final String courtCentreId, final String courtRoomId, final CourtListType courtListType) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("Standard CourtList courtCentreId:{}, payload:{}", courtCentreId, envelope.toObfuscatedDebugString());
        }

        if (!payload.getJsonArray(HEARINGS).isEmpty()) {
            LOGGER.info(" found hearings {}", payload);


            final CourtCentreDetails courtCentreDetails = courtCentreFactory.getCourtCentre(UUID.fromString(courtCentreId), envelope);
            final Boolean isWelsh = courtCentreDetails.isWelsh();
            final String listType = getListType(courtListType, isWelsh);

            Comparator<HearingDate> hearingDateComparator = (h1, h2) -> LocalDate.parse(h1.getHearingDate()).compareTo(LocalDate.parse(h2.getHearingDate()));

            Optional<JsonObject> courtListTemplateData = payload.getJsonArray(HEARINGS).getValuesAs(JsonObject.class).stream()
                    .filter(hearingsByCourtCentre ->  hearingsByCourtCentre.getJsonArray(HEARINGS_BY_COURT_CENTRE_ID)!=null)
                    .map(hearingsByCourtCentre -> {
                        return hearingsByCourtCentre.getJsonArray(HEARINGS_BY_COURT_CENTRE_ID).getValuesAs(JsonObject.class).stream()
                                .map(hearingByCourtCentreId -> {
                                    final LocalDate hearingDate = LocalDates.from(hearingByCourtCentreId.getString(HEARING_DATE));
                                    final Optional<JsonObject> referenceDataJudiciariesJsonObject = retrieveReferenceDataForJudiciary(hearingsByCourtCentre, envelope);
                                    return HearingDate.hearingDate()
                                            .withHearingDate(hearingDate.toString())
                                            .withHearingDateWelsh(createWelshHearingDate(hearingDate))
                                            .withCourtRooms(createCourtRoomsList(courtCentreDetails, courtRoomId, hearingByCourtCentreId, referenceDataJudiciariesJsonObject))
                                            .build();
                                })
                                .sorted(hearingDateComparator)
                                .collect(toList());
                    })
                    .map(hearingDates -> objectToJsonObjectConverter.convert(createStandardCourtList(courtCentreDetails, hearingDates, listType)))
                    .findFirst();


            LOGGER.info(" assembled courtListTemplateData: {}", courtListTemplateData);
            return courtListTemplateData;
        }
        return Optional.empty();
    }

    private String createWelshHearingDate(LocalDate hearingDate) {
        String welshDate = BLANK_STRING;
        final Optional<WelshMonth> welshMonth = WelshMonth.valueFor(hearingDate.getMonth());
        if(welshMonth.isPresent()){
           welshDate =  hearingDate.getDayOfMonth() + SPACE
                    + capitalize(lowerCase(welshMonth.get().name())) + SPACE
                    + hearingDate.getYear() ;
        }
         return welshDate;
       
    }

    private String getListType(CourtListType courtListType, boolean isWelsh) {
        String listType = courtListType.toString().toLowerCase();
        if(courtListType.equals(CourtListType.PUBLIC) && isWelsh){
            return listType.concat(WELSH);
        }
        return listType;
    }

    private Optional<JsonObject> retrieveReferenceDataForJudiciary(JsonObject hearingsByCourtCentre, JsonEnvelope envelope) {
        List<UUID> judiciaryIdList = hearingsByCourtCentre.getJsonArray(JUDICIARY).getValuesAs(JsonObject.class).stream()
                .map(judciary -> UUID.fromString(judciary.getString(JUDICIAL_ID)))
                .collect(toList());

        if (!judiciaryIdList.isEmpty()) {
            final JsonEnvelope referenceDataJudiciaries = referenceDataService.getJudiciariesByIdList(judiciaryIdList, envelope);
            return Optional.of(referenceDataJudiciaries.payloadAsJsonObject());
        }
        return Optional.empty();
    }



    private List<CourtRoom>  createCourtRoomsList(final CourtCentreDetails courtCentre, final String selectedCourtRoomId, final JsonObject hearingByCourtCentreId, final Optional<JsonObject> referenceDataJudiciariesJo) {

        final LocalDate hearingDate = LocalDates.from(hearingByCourtCentreId.getString(HEARING_DATE));

        final Map<String, List<JsonObject>> hearingsByCourtRoomIdMap = new HashMap();

        hearingByCourtCentreId.getJsonArray(HEARINGS_BY_HEARING_DATE).getValuesAs(JsonObject.class).stream()
                .filter(hearingByDate -> hearingByDate != null && hearingByDate.size() > 0)
                .map(hearingByDate -> hearingByDate.getJsonObject(HEARING))
                .forEach(hearing ->
                {
                    hearingsByCourtRoomIdMap.computeIfAbsent(hearing.getString(COURT_ROOM_ID), k -> new ArrayList<>()).add(hearing);
                });


        final List<CourtRoom> courtRooms = hearingsByCourtRoomIdMap.keySet().stream()
                .filter(courtRoomId -> selectedCourtRoomId == null || selectedCourtRoomId.equals(courtRoomId) )
                .map(courtRoomId -> createCourtRoom(hearingsByCourtRoomIdMap.get(courtRoomId), courtCentre.getCourtRooms().get(UUID.fromString(courtRoomId)), referenceDataJudiciariesJo, hearingDate))
                .sorted(Comparator.comparing(CourtRoom::getCourtRoomName))
                .collect(toList());
        return courtRooms;
    }

    private CourtRoom createCourtRoom(List<JsonObject> hearingsByCourtRoom, CourtRoomDetails courtRoomDetails, Optional<JsonObject> referenceDataJudiciariesJoOpt, LocalDate hearingDate) {
        final Map<LocalTime, List<Hearing>> unsortedListMultimap = new HashMap();


        final String judiciaryNamesWithCommas = referenceDataJudiciariesJoOpt.isPresent()
                ? createJudiciaryNames(hearingsByCourtRoom, referenceDataJudiciariesJoOpt.get())
                : BLANK_STRING;

        final String judiciaryNamesWelshWithCommas = referenceDataJudiciariesJoOpt.isPresent()
                ? createJudiciaryNamesWelsh(hearingsByCourtRoom, referenceDataJudiciariesJoOpt.get())
                : BLANK_STRING;


        hearingsByCourtRoom.stream()
                .forEach(hearingJson ->
                {

                    final JsonObject hearingDay = hearingJson.getJsonArray(HEARING_DAYS).getValuesAs(JsonObject.class).stream()
                            .filter(hd -> hearingDate.equals(ZonedDateTime.parse(hd.getString(START_TIME)).toLocalDate()))
                            .findFirst()
                            .orElseThrow(IllegalArgumentException::new);

                    final ZonedDateTime startTime = ZonedDateTimes.fromString(hearingDay.getString(START_TIME));
                    final Integer sequence = hearingDay.getInt(SEQUENCE);

                    final String hearingStartTime = START_TIME_FORMAT.format(startTime.getHour()) + COLON + START_TIME_FORMAT.format(startTime.getMinute());



                    List<Hearing> hearings = hearingJson.getJsonArray(LISTED_CASES).getValuesAs(JsonObject.class).stream()
                            .map(listedCase -> createHearing(hearingJson, hearingStartTime, sequence, listedCase))
                            .collect(toList());

                    unsortedListMultimap.computeIfAbsent(startTime.toLocalTime(), k -> new ArrayList<>()).addAll(hearings);
                });


        final Map<LocalTime, List<Hearing>> sortedListMultimap = new TreeMap<>();

        unsortedListMultimap.entrySet()
                .forEach(e -> {
                    List<Hearing> hearingsRow = e.getValue().stream()
                            .sorted(Comparator.comparing(Hearing::getSequence))
                            .collect(toList());
                    sortedListMultimap.put(e.getKey(), hearingsRow);
                });


        final List<Timeslot> timeslots = sortedListMultimap.keySet().stream().map(key -> Timeslot.timeslot()
                .withHearings(sortedListMultimap.get(key))
                .build())
                .collect(Collectors.toList());

        return CourtRoom.courtRoom()
                .withCourtRoomName(courtRoomDetails.getCourtRoomName())
                .withWelshCourtRoomName(courtRoomDetails.getWelshCourtRoomName())
                .withJudiciaryNames(judiciaryNamesWithCommas)
                .withWelshJudiciaryNames(judiciaryNamesWelshWithCommas)
                .withTimeslots(timeslots)
                .build();
    }

    private String createJudiciaryNamesWelsh(List<JsonObject> hearingsByCourtRoom, JsonObject judiciariesJsonObject) {
        final List<String> courtRoomJudiciaryIds = getJudicialIds(hearingsByCourtRoom);
        final List<String> judiciaryNames = judiciariesJsonObject.getJsonArray(JUDICIARIES).getValuesAs(JsonObject.class).stream()
                .filter(j -> courtRoomJudiciaryIds.contains(j.getString(ID)))
                .sorted(Comparator.comparing(jo -> jo.getString(SURNAME)))
                .map(j -> j.getString(TITLE_PREFIX_WELSH, BLANK_STRING) + SPACE + j.getString(TITLE_JUDICIARY_PREFIX_WELSH, BLANK_STRING) + SPACE + j.getString(SURNAME, BLANK_STRING) + (j.getString(TITLE_SUFFIX_WELSH, BLANK_STRING).equals(BLANK_STRING) ? BLANK_STRING : SPACE + j.getString(TITLE_SUFFIX_WELSH)))
                .collect(toList());
        return String.join(", ", judiciaryNames);
    }
    

    private String createJudiciaryNames(List<JsonObject> hearingsByCourtRoom, JsonObject judiciariesJsonObject) {
        final List<String> courtRoomJudiciaryIds = getJudicialIds(hearingsByCourtRoom);

        final List<String> judiciaryNames = judiciariesJsonObject.getJsonArray(JUDICIARIES).getValuesAs(JsonObject.class).stream()
                .filter(j -> courtRoomJudiciaryIds.contains(j.getString(ID)))
                .sorted(Comparator.comparing(jo -> jo.getString(SURNAME)))
                .map(j -> j.getString(TITLE_PREFIX, BLANK_STRING) + SPACE + j.getString(TITLE_JUDICIARY_PREFIX, BLANK_STRING) + SPACE + j.getString(SURNAME, BLANK_STRING) + (j.getString(TITLE_SUFFIX, BLANK_STRING).equals(BLANK_STRING) ? BLANK_STRING : SPACE + j.getString(TITLE_SUFFIX)))
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
                .withCourtCentreAddress2(blankStringIfNull(courtCentre.getAddress3()) + SPACE + blankStringIfNull(courtCentre.getAddress4())  + SPACE + blankStringIfNull(courtCentre.getAddress5()) + SPACE + blankStringIfNull(courtCentre.getPostcode()))
                .withWelshCourtCentreAddress2(blankStringIfNull(courtCentre.getWelshAddress3()) + SPACE + blankStringIfNull(courtCentre.getWelshAddress4())  + SPACE + blankStringIfNull(courtCentre.getWelshAddress5()) + SPACE + blankStringIfNull(courtCentre.getPostcode()))
                .withHearingDates(hearingDates)
                .build();
    }

    private String blankStringIfNull(String stringToCheck) {
        return stringToCheck != null ? stringToCheck : BLANK_STRING;
    }


    private Hearing createHearing(JsonObject hearingJson, String hearingStartTime, Integer sequence, JsonObject listedCase) {
        final String hearingType = hearingJson.getJsonObject(TYPE).getString(DESCRIPTION);
        final String reportingRestrictionReason = hearingJson.getString(REPORTING_RESTRICTION_REASON, BLANK_STRING);
        final String welshReportingRestrictionReason = hearingJson.getString(WELSH_REPORTING_RESTRICTION_REASON, BLANK_STRING);

        return Hearing.hearing()
                .withCaseNumber(listedCase.getJsonObject(CASE_IDENTIFIER).getString(CASE_REFERENCE))
                .withHearingType(hearingType)
                .withWelshHearingType(hearingType)
                .withProsecutorType(listedCase.getJsonObject(CASE_IDENTIFIER).getString(AUTHORITY_CODE))
                .withSequence(sequence)
                .withReportingRestrictionReason(reportingRestrictionReason)
                .withWelshReportingRestrictionReason(reportingRestrictionReason)
                .withWelshReportingRestrictionReason(welshReportingRestrictionReason)
                .withDefendants(listedCase.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                        .map(d -> {
                            final String dateOfBirth = d.getString(DATE_OF_BIRTH, null);
                            return createDefendant(d, dateOfBirth);
                        })
                        .collect(toList()))
                .withStartTime(hearingStartTime)
                .build();
    }

    private Defendant createDefendant(JsonObject d, String dateOfBirth) {
        Defendant.Builder builder = Defendant.defendant()
                .withFirstName(d.getString(FIRST_NAME))
                .withSurname(d.getString(LAST_NAME));
        if (dateOfBirth != null) {
            builder.withDateOfBirth(parse(dateOfBirth).format(DOB_FORMATTER));
            builder.withAge(String.valueOf(Period.between(parse(dateOfBirth), LocalDate.now()).getYears()));
        }
        builder.withOffences(d.getJsonArray(OFFENCES).getValuesAs(JsonObject.class).stream()
                .map(o -> Offence.offence()
                        .withOffenceTitle(o.getJsonObject(STATEMENT_OF_OFFENCE).getString(TITLE))
                        .withWelshOffenceTitle(o.getJsonObject(STATEMENT_OF_OFFENCE).getString(WELSH_TITLE, BLANK_STRING))
                        .withOffenceWording(o.getString(OFFENCE_WORDING, BLANK_STRING))
                        .build())
                .collect(toList()));

        return builder.build();
    }
}
