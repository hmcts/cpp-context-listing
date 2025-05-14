package uk.gov.moj.cpp.listing.query.document.generator;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Counsel;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Defendant;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.HearingDay;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.JudgeList;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Sitting;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.SittingHearing;
import uk.gov.moj.cpp.listing.query.document.generator.util.SittingsSorter;
import uk.gov.moj.cpp.listing.query.view.courtlist.FlatHearingsConverter;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Judiciaries;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.SittingKeyByJudiciaries;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JudgeListTemplateAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JudgeListTemplateAssembler.class);

    private static final String HEARINGS = "hearings";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String CASE_REFERENCE = "caseReference";
    public static final String LISTED_CASES = "listedCases";
    private static final String ATTENDANCE_DAYS = "attendanceDays";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String MIDDLE_NAME = "middleName";
    private static final String TITLE = "title";
    private static final String PROSECUTION_COUNSELS = "prosecutionCounsels";
    private static final String DEFENCE_COUNSELS = "defenceCounsels";
    private static final String DEFENDANTS = "defendants";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String ID = "id";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String TIME_PATTERN = "hh:mm a";
    private static final String TYPE = "type";
    private static final String DESCRIPTION = "description";
    private static final String DURATION_MINUTES = "durationMinutes";
    private static final String JUDICIAL_ID = "judicialId";
    private static final String JUDICIAL_ROLE_TYPE = "judicialRoleType";
    private static final String JUDICIAL_TYPE = "judiciaryType";
    private static final String JP = "JP";
    private static final ZoneId ZONE = ZoneId.of("Europe/London");
    private static final List<String> JUDICIARY_TYPES_JUDGES = asList(
            "Circuit Judge",
            "Deputy Circuit Judge",
            "Circuit Judge, Central Criminal Court",
            "Senior Circuit Judge",
            "Common Serjeant",
            "Recorder",
            "Deputy High Court Judge",
            "Judge Advocate Genera",
            "President of the Queen’s Bench Division",
            "Specialist Circuit Judge",
            "Senior Presiding Judge",
            "Vice President of the QBD",
            "Recorder of London",
            "High Court Judge",
            "High Court Judge- Sitting in Retirement");
    private static final List<String> JUDICIARY_TYPES_MAGISTRATES = asList("Magistrate");

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private ReferenceDataCache referenceDataCache;

    @Inject
    private SittingsSorter sittingsSorter;

    public Optional<JsonObject> assemble(JsonEnvelope envelope, final String courtCentreId, final String courtRoomId, final CourtListType courtListType, final String startDate) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final List<FlatHearing> allFlatHearings = FlatHearingsConverter.generateFlatHearingList(payload.getJsonArray(HEARINGS));


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Judge List for Court Centre {} and Court Room {} with List Type {} and Hearings {}", courtCentreId, courtRoomId, courtListType, payload);
        }

        final List<Sitting> sittings = new ArrayList<>();

        final List<FlatHearing> flatHearingsByHearingDate = allFlatHearings.stream().filter(fh -> fh.getHearingDate().compareTo(LocalDate.parse(startDate)) == 0).collect(toList());

        for (final FlatHearing flatHearing : flatHearingsByHearingDate) {
            final Optional<Sitting> sitting = findExistingSittingForFlatHearing(sittings, flatHearing);

            if (sitting.isPresent()) {

                buildUpdatedSitting(sittings, sitting.get(), flatHearing, startDate);
            } else {

                buildNewSitting(sittings, flatHearing, startDate);
            }
        }

        final CourtCentreDetails courtCentreDetails = courtCentreFactory.getCourtCentre(fromString(courtCentreId), envelope);

        return of(objectToJsonObjectConverter.convert(createJudgeList(courtCentreDetails, courtRoomId, sittingsSorter.sort(sittings), startDate)));
    }

    private void buildUpdatedSitting(final List<Sitting> sittings, final Sitting sitting, final FlatHearing flatHearing, final String startDate) {
        final List<SittingHearing> sittingHearings = buildSittingHearingDetails(flatHearing, startDate);
        if (!isEmpty(sittingHearings)) {
            sittingHearings.addAll(sitting.getHearings());
            final Sitting.Builder sittingBuilder = Sitting.sitting();
            sittingBuilder.withSittingKey(sitting.getSittingKey());
            sittingBuilder.withHearings(sittingHearings);
            sittingBuilder.withSittingTime(extractSittingTime(sittingHearings));
            sittingBuilder.withJudiciaryNames(sitting.getJudiciaryNames());
            sittings.add(sittingBuilder.build());
            sittings.remove(sitting);
        }
    }

    private String extractSittingTime(final List<SittingHearing> sittingHearings) {
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);
        final List<LocalTime> times = sittingHearings.stream()
                .map(sh -> LocalTime.parse(sh.getHearingDay().getStartTime(), timeFormatter))
                .sorted(Comparator.reverseOrder())
                .collect(toList());
        return times.get(times.size() - 1).format(timeFormatter);
    }

    private void buildNewSitting(final List<Sitting> sittings, final FlatHearing flatHearing, final String startDate) {
        final List<SittingHearing> sittingHearings = buildSittingHearingDetails(flatHearing, startDate);
        if (!isEmpty(sittingHearings)) {
            final Sitting.Builder sittingBuilder = Sitting.sitting();
            sittingBuilder.withHearings(sittingHearings);
            sittingBuilder.withSittingTime(extractSittingTime(sittingHearings));

            sittingBuilder.withSittingKey(buildSittingKey(flatHearing));
            sittingBuilder.withJudiciaryNames(buildJudiciaryNames(flatHearing));
            sittings.add(sittingBuilder.build());
        }
    }

    private List<SittingHearing> buildSittingHearingDetails(final FlatHearing flatHearing, final String startDate) {
        final JsonObject hearingJson = flatHearing.getCaseHearings();
        final HearingDay hearingDay = buildHearingDay(flatHearing);
        if (isCaseHearing(hearingJson)) {
            return hearingJson
                    .getJsonArray(LISTED_CASES).getValuesAs(JsonObject.class)
                    .stream().map(caseDetails -> {
                        final SittingHearing.Builder sittingHearingBuilder = SittingHearing.hearing();
                        sittingHearingBuilder.withHearingDay(hearingDay);
                        sittingHearingBuilder.withCaseNumber(caseDetails.getJsonObject(CASE_IDENTIFIER).getString(CASE_REFERENCE));
                        sittingHearingBuilder.withHearingType(hearingJson.getJsonObject(TYPE).getString(DESCRIPTION));
                        final JsonArray defendantJson = caseDetails.getJsonArray(DEFENDANTS);
                        final List<Defendant> defendants = buildDefendants(defendantJson);
                        sittingHearingBuilder.withDefendants(defendants);

                        if (hearingJson.containsKey(DEFENCE_COUNSELS)) {
                            final List<Counsel> defenseCounsels = defendantJson.getValuesAs(JsonObject.class).stream()
                                    .map(defendant -> extractCounsels(hearingJson.getJsonArray(DEFENCE_COUNSELS), defendant.getString(ID), DEFENDANTS, startDate))
                                    .collect(toList()).stream().flatMap(List::stream).collect(toList());
                            sittingHearingBuilder.withDefenceCounsels(defenseCounsels);

                        }
                        if (hearingJson.containsKey(PROSECUTION_COUNSELS)) {
                            final List<Counsel> prosecutionCounsel = extractCounsels(hearingJson.getJsonArray(PROSECUTION_COUNSELS), caseDetails.getString(ID), PROSECUTION_CASES, startDate);
                            sittingHearingBuilder.withProsecutionCounsels(prosecutionCounsel);
                        }

                        return sittingHearingBuilder.build();
                    }).collect(toList());

        }
        return emptyList();
    }

    private HearingDay buildHearingDay(final FlatHearing flatHearing) {
        final JsonObject hearingDayJson = flatHearing.getCaseHearings().getJsonArray(HEARING_DAYS).getValuesAs(JsonObject.class).stream()
                .filter(hd -> flatHearing.getHearingDate().equals(ZonedDateTime.parse(hd.getString(START_TIME)).toLocalDate()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        final String startTime = prettifyDate(hearingDayJson, START_TIME);
        final String endTime = prettifyDate(hearingDayJson, END_TIME);
        final int duration = hearingDayJson.containsKey(DURATION_MINUTES) ? hearingDayJson.getInt(DURATION_MINUTES) : 0;
        return HearingDay.hearingDay().withEndTime(endTime).withStartTime(startTime).withHearingDate(flatHearing.getHearingDate().toString()).withDurationMinutes(duration).build();

    }

    private String prettifyDate(final JsonObject hearingDayJson, final String unformattedTime) {
        final ZonedDateTime formattedTime = ZonedDateTime.parse(hearingDayJson.getString(unformattedTime)).withZoneSameInstant(ZONE);
        return formattedTime.format(DateTimeFormatter.ofPattern(TIME_PATTERN));
    }

    private List<Defendant> buildDefendants(final JsonArray defendants) {
        return defendants.getValuesAs(JsonObject.class).stream()
                .map(defendant -> {
                            final Defendant.Builder defendantBuilder = Defendant.defendant();
                            if (defendant.containsKey(ORGANISATION_NAME)) {
                                defendantBuilder.withOrganisationName(defendant.getString(ORGANISATION_NAME));
                            } else {
                                defendantBuilder.withSurname(defendant.getString(LAST_NAME))
                                        .withFirstName(defendant.getString(FIRST_NAME));
                            }
                            return defendantBuilder.build();
                        }
                )
                .collect(toList());
    }

    private List<Counsel> extractCounsels(final JsonArray requestCounsels, final String id, final String type, final String startDate) {
        final List<Counsel> counsels = new ArrayList<>();
        for (final JsonObject requestCounsel : requestCounsels.getValuesAs(JsonObject.class)) {
            if (idMatches(requestCounsel, type, id)
                    && attendanceDaysMatches(requestCounsel, startDate)) {
                counsels.add(Counsel.counsel()
                        .withFirstName(extractName(requestCounsel, FIRST_NAME))
                        .withMiddleName(extractName(requestCounsel, MIDDLE_NAME))
                        .withLastName(extractName(requestCounsel, LAST_NAME))
                        .withTitle(extractName(requestCounsel, TITLE))
                        .build());
            }
        }
        return counsels;
    }
    
    private boolean idMatches(final JsonObject requestCounsel, final String jsonNode, final String id) {
        return arrayContains(requestCounsel, jsonNode, id);
    }

    private boolean attendanceDaysMatches(final JsonObject requestCounsel, final String startDate) {
        return arrayContains(requestCounsel, ATTENDANCE_DAYS, startDate);
    }

    private boolean arrayContains(final JsonObject requestCounsel, final String jsonNode, final String filter) {
        return requestCounsel.getJsonArray(jsonNode)
                .getValuesAs(JsonValue.class).stream()
                .filter(JsonString.class::isInstance)
                .map(JsonString.class::cast)
                .anyMatch(value -> filter.equals(value.getString()));
    }

    @SuppressWarnings("PMD.NullAssignment")
    private String extractName(final JsonObject requestCounsel, final String key) {
        return requestCounsel.containsKey(key) ? requestCounsel.getString(key) : null;
    }

    private static Optional<Sitting> findExistingSittingForFlatHearing(final List<Sitting> sittings, final FlatHearing flatHearing) {

        for (final Sitting sitting : sittings) {
            if (sitting.getSittingKey().equals(buildSittingKey(flatHearing))) {
                return of(sitting);
            }
        }
        return empty();
    }

    private static SittingKeyByJudiciaries buildSittingKey(final FlatHearing flatHearing) {

        final JsonArray judiciaryArray = flatHearing.getJudiciary();

        final List<Judiciaries> judiciaries = judiciaryArray == JsonArray.NULL ? emptyList() : judiciaryArray.getValuesAs(JsonObject.class).stream()
                .map(judicial ->
                        new Judiciaries(fromString(judicial.getString(JUDICIAL_ID)),
                                judicial.getJsonObject(JUDICIAL_ROLE_TYPE).getString(JUDICIAL_TYPE)))
                .collect(toList());
        return new SittingKeyByJudiciaries(
                flatHearing.getHearingDate(),
                flatHearing.getCourtRoomId(),
                ofNullable(judiciaries)
        );
    }

    private List<String> buildJudiciaryNames(final FlatHearing flatHearing) {
        final JsonArray judiciaryArray = flatHearing.getJudiciary();

        final List<UUID> judiciaryIdList = judiciaryArray == JsonArray.NULL ? emptyList() : judiciaryArray.getValuesAs(JsonObject.class).stream()
                .map(judicial -> UUID.fromString(judicial.getString(JUDICIAL_ID)))
                .collect(toList());

        List<String> judiciaryNames = emptyList();
        if (!judiciaryIdList.isEmpty()) {
            final List<Judiciary> referenceDataJudiciaries = judiciaryIdList
                    .stream()
                    .map(id -> referenceDataCache.getJudiciariesMapCache(id).orElse(null))
                    .collect(toList());

            judiciaryNames = referenceDataJudiciaries
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(a -> JUDICIARY_TYPES_JUDGES.contains(a.getJudiciaryType()))
                    .map(Judiciary::getRequestedName)
                    .collect(toList());

            judiciaryNames.addAll(referenceDataJudiciaries
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(a -> JUDICIARY_TYPES_MAGISTRATES.contains(a.getJudiciaryType()))
                    .map(a -> join(" ", a.getTitlePrefix(), a.getSurname(), JP))
                    .collect(toList()));

            judiciaryNames.addAll(referenceDataJudiciaries
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(a -> !JUDICIARY_TYPES_JUDGES.contains(a.getJudiciaryType()))
                    .filter(a -> !JUDICIARY_TYPES_MAGISTRATES.contains(a.getJudiciaryType()))
                    .map(Judiciary::getRequestedName)
                    .collect(toList()));
        }

        return judiciaryNames;
    }

    private static boolean isCaseHearing(final JsonObject caseHearingsJson) {
        return caseHearingsJson.containsKey(LISTED_CASES);
    }


    private JudgeList createJudgeList(final CourtCentreDetails courtCentre, final String courtRoomId, List<Sitting> sittings, final String startDate) {
        return JudgeList.judgeList()
                .withCourtCentre(courtCentre.getOuCodeL3Name())
                .withCourtRoomName(courtCentre.getCourtRooms().get(fromString(courtRoomId)).getCourtRoomName())
                .withHearingDate(startDate)
                .withSitting(sittings)
                .build();
    }
}
