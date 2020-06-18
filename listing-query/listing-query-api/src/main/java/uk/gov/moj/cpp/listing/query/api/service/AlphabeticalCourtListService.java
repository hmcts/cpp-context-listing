package uk.gov.moj.cpp.listing.query.api.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.drools.core.util.StringUtils.EMPTY;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON;
import static uk.gov.moj.cpp.listing.domain.utils.ZonedDateTimeFormatter.adjustDateTime;
import static uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist.AlphabeticalListDefendant.AlphabeticalListDefendantBuilder.anAlphabeticalListDefendant;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType;
import uk.gov.moj.cpp.listing.domain.WelshMonth;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist.AlphabeticalCourtList;
import uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist.AlphabeticalListDefendant;
import uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist.AlphabeticalListDefendant.AlphabeticalListDefendantBuilder;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AlphabeticalCourtListService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlphabeticalCourtListService.class);

    private static final String DATE_FORMAT = "dd MMMM YYYY";
    private static final String HEARING_DATE = "hearingDate";
    private static final String START_TIME = "startTime";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String HEARINGS_BY_HEARING_DATE = "hearingsByHearingDate";
    private static final String DEFENDANTS = "defendants";
    private static final String HEARINGS = "hearings";
    private static final String HEARING = "hearing";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String RESTRICT_FROM_COURT_LIST = "restrictFromCourtList";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String LISTED_CASES = "listedCases";
    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String APPLICATION_REFERENCE = "applicationReference";
    private static final String RESPONDENTS = "respondents";
    private static final String COURT_APPLICATION_PARTY_TYPE = "courtApplicationPartyType";
    private static final String APPLICANT = "applicant";

    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public Optional<JsonObject> buildAlphabeticalCourtListData(JsonEnvelope envelope, final String courtCentreId) {
        final JsonArray hearings = envelope.payloadAsJsonObject().getJsonArray(HEARINGS);
        if (!hearings.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(" found hearings {}", envelope.toObfuscatedDebugString());
            }
            final CourtCentreDetails courtCentreDetails = courtCentreFactory.getCourtCentre(fromString(courtCentreId), envelope);
            final boolean welsh = courtCentreDetails.isWelsh();
            return hearings.getValuesAs(JsonObject.class)
                    .stream().map(hearing -> getDataForCourtList(courtCentreDetails, hearing, welsh)).findFirst();
        }
        return empty();
    }


    private JsonObject getDataForCourtList(final CourtCentreDetails courtCentreDetails, final JsonObject hearing, final boolean welsh) {
        final List<AlphabeticalListDefendant> defendants = new ArrayList<>();
        final LocalDate hearingDate = LocalDates.from(hearing.getString(HEARING_DATE));
        if (hearing.containsKey(HEARINGS_BY_HEARING_DATE)) {
            final List<AlphabeticalListDefendant> alphabeticalListDefendants = hearing.getJsonArray(HEARINGS_BY_HEARING_DATE).getValuesAs(JsonObject.class).stream()
                    .map(hearingByDate -> hearingByDate.getJsonObject(HEARING))
                    .flatMap(hearingJson -> getAlphabeticalListDefendantFromHearing(hearingJson, hearingDate, courtCentreDetails, welsh))
                    .collect(toList());
            defendants.addAll(alphabeticalListDefendants);
        }
        defendants.sort(comparing(AlphabeticalListDefendant::getDefendantFullName));
        return objectToJsonObjectConverter.convert(getAlphabeticalCourtList(defendants, hearingDate, courtCentreDetails, welsh));
    }

    private Stream<AlphabeticalListDefendant> getAlphabeticalListDefendantFromHearing(final JsonObject hearing, final LocalDate hearingDate,
                                                                                      final CourtCentreDetails courtCentreDetails, final boolean welsh) {
        final List<AlphabeticalListDefendant> defendantsFromHearing = newArrayList();
        final Optional<String> hearingStartTime = getHearingStartTime(hearing, hearingDate);
        final CourtRoomDetails courtRoomDetails = courtCentreDetails.getCourtRooms().get(fromString(hearing.getString(COURT_ROOM_ID)));
        if (hearing.containsKey(LISTED_CASES) && !hearing.getJsonArray(LISTED_CASES).isEmpty()) {
            final List<AlphabeticalListDefendant> defendantsFromListedCases = hearing.getJsonArray(LISTED_CASES).getValuesAs(JsonObject.class).stream()
                    .filter(listedCase -> !listedCase.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE))
                    .flatMap(listedCase -> getAlphabeticalListDefendantFromListedCase(listedCase, courtRoomDetails, hearingStartTime.orElse(EMPTY), welsh))
                    .collect(toList());
            defendantsFromHearing.addAll(defendantsFromListedCases);
        }
        if (hearing.containsKey(COURT_APPLICATIONS) && !hearing.getJsonArray(COURT_APPLICATIONS).isEmpty()) {
            final List<AlphabeticalListDefendant> defendantsFromCourtApplications = hearing.getJsonArray(COURT_APPLICATIONS).getValuesAs(JsonObject.class).stream()
                    .filter(courtApplication -> !courtApplication.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE))
                    .flatMap(courtApplication -> getAlphabeticalListDefendantFromCourtApplication(courtApplication, courtRoomDetails, hearingStartTime.orElse(EMPTY), welsh))
                    .collect(toList());
            defendantsFromHearing.addAll(defendantsFromCourtApplications);
        }
        return defendantsFromHearing.stream();
    }

    private Stream<AlphabeticalListDefendant> getAlphabeticalListDefendantFromCourtApplication(final JsonObject courtApplication, final CourtRoomDetails courtRoomDetails,
                                                                                               final String hearingStartTime, final boolean welsh) {
        final String applicationReference = courtApplication.getString(APPLICATION_REFERENCE);
        if (courtApplication.containsKey(RESPONDENTS) && !courtApplication.getJsonArray(RESPONDENTS).isEmpty() &&
                courtApplication.getJsonArray(RESPONDENTS).getValuesAs(JsonObject.class).stream()
                        .anyMatch(respondent -> CourtApplicationPartyType.valueOf(respondent.getString(COURT_APPLICATION_PARTY_TYPE)) == PERSON)) {
            return courtApplication.getJsonArray(RESPONDENTS).getValuesAs(JsonObject.class).stream()
                    .filter(respondent -> !respondent.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE))
                    .map(respondent -> getAlphabeticalListDefendantFromDefendantEquivalent(respondent, hearingStartTime, applicationReference, courtRoomDetails, welsh))
                    .collect(toList()).stream();
        }
        final JsonObject applicant = courtApplication.getJsonObject(APPLICANT);
        if (!applicant.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE)) {
            return Stream.of(getAlphabeticalListDefendantFromDefendantEquivalent(applicant, hearingStartTime, applicationReference, courtRoomDetails, welsh));
        }
        return Stream.empty();
    }

    private Stream<AlphabeticalListDefendant> getAlphabeticalListDefendantFromListedCase(final JsonObject listedCase, final CourtRoomDetails courtRoomDetails,
                                                                                         final String hearingStartTime, final boolean welsh) {
        final String caseReference = listedCase.getJsonObject(CASE_IDENTIFIER).getString(CASE_REFERENCE);
        return listedCase.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                .filter(defendant -> !defendant.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE))
                .map(defendant -> getAlphabeticalListDefendant(defendant, hearingStartTime, caseReference, courtRoomDetails,
                        welsh))
                .collect(toList()).stream();
    }

    private Optional<String> getHearingStartTime(final JsonObject hearing, final LocalDate hearingDate) {
        final Optional<JsonObject> matchedHearingDay = hearing.getJsonArray(HEARING_DAYS).getValuesAs(JsonObject.class).stream()
                .filter(hearingDay -> LocalDates.from(hearingDay.getString(HEARING_DATE)).equals(hearingDate))
                .findFirst();
        if (matchedHearingDay.isPresent()) {
            final ZonedDateTime dateTime = adjustDateTime(ZonedDateTimes.fromString(matchedHearingDay.get().getString(START_TIME)));
            final DecimalFormat formatter = new DecimalFormat("00");
            return Optional.of(formatter.format(dateTime.getHour()) + ":" + formatter.format(dateTime.getMinute()));
        }
        return empty();
    }

    private AlphabeticalListDefendant getAlphabeticalListDefendantFromDefendantEquivalent(final JsonObject defendantEquivalent, final String hearingStartTime, final String applicationReference,
                                                                                          final CourtRoomDetails courtRoomDetails, final boolean welsh) {
        final AlphabeticalListDefendantBuilder builder = anAlphabeticalListDefendant();
        final String fullName;
        if (CourtApplicationPartyType.valueOf(defendantEquivalent.getString(COURT_APPLICATION_PARTY_TYPE)) == PERSON) {
            fullName = format("%s, %s", upperCase(defendantEquivalent.getString(LAST_NAME)), defendantEquivalent.getString(FIRST_NAME));
        } else {
            fullName = defendantEquivalent.getString(LAST_NAME);
        }
        builder.withDefendantFullName(fullName)
                .withCourtRoomName(courtRoomDetails.getCourtRoomName())
                .withHearingStartTime(hearingStartTime)
                .withCaseReference(applicationReference);
        if (welsh) {
            builder.withCourtRoomNameWelsh(courtRoomDetails.getWelshCourtRoomName());
        }
        return builder.build();
    }

    private AlphabeticalListDefendant getAlphabeticalListDefendant(final JsonObject defendant, final String startTime, final String caseReference,
                                                                   final CourtRoomDetails courtRoomDetails, final boolean isWelsh) {
        final AlphabeticalListDefendantBuilder alphabeticalListDefendantBuilder = anAlphabeticalListDefendant();
        alphabeticalListDefendantBuilder.withDefendantFullName(
                isNotBlank(defendant.getString(ORGANISATION_NAME, EMPTY)) ? upperCase(defendant.getString(ORGANISATION_NAME)) :
                        upperCase(defendant.getString(LAST_NAME)) + "," + SPACE + defendant.getString(FIRST_NAME))
                .withCourtRoomName(courtRoomDetails.getCourtRoomName())
                .withHearingStartTime(startTime)
                .withCaseReference(caseReference);
        if (isWelsh) {
            alphabeticalListDefendantBuilder.withCourtRoomNameWelsh(courtRoomDetails.getWelshCourtRoomName());
        }
        return alphabeticalListDefendantBuilder.build();
    }

    private AlphabeticalCourtList getAlphabeticalCourtList(final List<AlphabeticalListDefendant> defendants, final LocalDate hearingDate,
                                                           final CourtCentreDetails courtCentreDetails, final boolean welsh) {
        final String hearingDateEng = hearingDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));

        final AlphabeticalCourtList.AlphabeticalCourtListBuilder alphabeticalCourtListBuilder =
                AlphabeticalCourtList.AlphabeticalCourtListBuilder.anAlphabeticalCourtList();
        alphabeticalCourtListBuilder
                .withCourtCentreName(courtCentreDetails.getCourtCentreName())
                .withHearingDate(hearingDateEng)
                .withCourtCentreAddress1(trim(
                        courtCentreDetails.getAddress1() + SPACE + defaultString
                                (courtCentreDetails.getAddress2()) + SPACE +
                                defaultString(courtCentreDetails.getAddress3())) + ",")
                .withCourtCentreAddress2(trim(
                        defaultString(courtCentreDetails.getAddress4()) + SPACE +
                                defaultString(courtCentreDetails.getAddress5()) +
                                defaultString(courtCentreDetails.getPostcode())))
                .withDefendants(defendants).build();
        if (welsh) {
            final Optional<WelshMonth> welshMonth = WelshMonth.valueFor(hearingDate.getMonth());
            welshMonth.ifPresent(wm ->
                    alphabeticalCourtListBuilder.withWelshHearingDate(hearingDate.getDayOfMonth() +
                            SPACE + capitalize(lowerCase(wm.name())) + SPACE + hearingDate.getYear()));


            alphabeticalCourtListBuilder
                    .withWelsh(true)
                    .withWelshCourtCentreName(courtCentreDetails.getWelshCourtCentreName())
                    .withWelshCourtCentreAddress1(
                            trim(courtCentreDetails.getWelshAddress1() + SPACE +
                                    defaultString(courtCentreDetails.getWelshAddress2()) + SPACE +
                                    defaultString(courtCentreDetails.getWelshAddress3())) + ",")
                    .withWelshCourtCentreAddress2(trim(
                            defaultString(courtCentreDetails.getWelshAddress4()) + SPACE +
                                    defaultString(courtCentreDetails.getWelshAddress5()) + defaultString(courtCentreDetails.getPostcode())));
        }
        return alphabeticalCourtListBuilder.build();
    }

}
