package uk.gov.moj.cpp.listing.query.api.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static java.lang.Boolean.FALSE;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.upperCase;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.WelshMonth;
import uk.gov.moj.cpp.listing.domain.utils.ZonedDateTimeFormatter;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist.AlphabeticalCourtList;
import uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist.AlphabeticalListDefendant;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AlphabeticalCourtListService {
    private static final String DATE_FORMAT = "dd MMMM YYYY";
    private static final String HEARING_DATE = "hearingDate";
    private static final String START_TIME = "startTime";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String HEARINGS_BY_HEARING_DATE = "hearingsByHearingDate";
    private static final String DEFENDANTS = "defendants";
    private static final String HEARINGS = "hearings";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final Logger LOGGER = LoggerFactory.getLogger(AlphabeticalCourtListService.class);
    private static final String RESTRICT_FROM_COURT_LIST = "restrictFromCourtList";

    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public Optional<JsonObject> buildAlphabeticalCourtListData(JsonEnvelope envelope, final String courtCentreId) {
        final JsonArray hearings = envelope.payloadAsJsonObject().getJsonArray(HEARINGS);
        if (!hearings.isEmpty()) {
            LOGGER.info(" found hearings {}", envelope.payloadAsJsonObject());
            final CourtCentreDetails courtCentreDetails = courtCentreFactory.getCourtCentre(UUID.fromString(courtCentreId), envelope);
            final Boolean welsh = courtCentreDetails.isWelsh();
            return hearings.getValuesAs(JsonObject.class)
                    .stream().map(hearing -> getDataForCourtList(courtCentreDetails, hearing, welsh)).findFirst();
        }
        return Optional.empty();

    }


    private JsonObject getDataForCourtList(final CourtCentreDetails courtCentreDetails, final JsonObject hearing, final Boolean welsh) {

        final List<AlphabeticalListDefendant> defendants = new ArrayList<>();
        final LocalDate hearingDate = LocalDates.from(hearing.getString(HEARING_DATE));
        if(hearing.get(HEARINGS_BY_HEARING_DATE) != null) {
            hearing.getJsonArray(HEARINGS_BY_HEARING_DATE).getValuesAs(JsonObject.class).stream().filter(hearingByDate -> !hearingByDate.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE)).forEach(hearingByDate -> {
                final ZonedDateTime dateTime = ZonedDateTimeFormatter.adjustDateTime(ZonedDateTimes.fromString(hearingByDate.getString(START_TIME)));
                final DecimalFormat format = new DecimalFormat("00");
                final String hearingStartTime = format.format(dateTime.getHour()) + ":" + format.format(dateTime.getMinute());
                final String caseReference = hearingByDate.getJsonObject(CASE_IDENTIFIER).getString(CASE_REFERENCE);
                hearingByDate.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream().filter(defendant -> !defendant.getBoolean(RESTRICT_FROM_COURT_LIST, FALSE)).forEach(defendant ->
                        defendants.add(
                                getAlphabeticalListDefendant(courtCentreDetails.getCourtRooms().get(
                                        UUID.fromString(
                                                hearingByDate.getString(COURT_ROOM_ID))),
                                        hearingStartTime, defendant, caseReference, welsh)));


            });
        }
        defendants.sort(Comparator.comparing(AlphabeticalListDefendant::getDefendantFullName));
        return objectToJsonObjectConverter.convert(getAlphabeticalCourtList(courtCentreDetails, welsh, defendants, hearingDate));
    }

    private AlphabeticalCourtList getAlphabeticalCourtList(CourtCentreDetails courtCentreDetails,
                                                           Boolean welsh, List<AlphabeticalListDefendant> defendants,
                                                           LocalDate hearingDate) {
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
                    .withWelsh(welsh)
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

    private AlphabeticalListDefendant getAlphabeticalListDefendant(final CourtRoomDetails courtRoomDetails,
                                                                   final String startTime, final JsonObject defendant,
                                                                   final String caseReference, final boolean isWelsh) {
        final AlphabeticalListDefendant.AlphabeticalListDefendantBuilder alphabeticalListDefendantBuilder =
                AlphabeticalListDefendant.AlphabeticalListDefendantBuilder.anAlphabeticalListDefendant();
        alphabeticalListDefendantBuilder.withDefendantFullName(
                isNotBlank(defendant.getString(ORGANISATION_NAME,EMPTY)) ? upperCase(defendant.getString(ORGANISATION_NAME)) :
                        upperCase(defendant.getString(LAST_NAME)) + "," + SPACE + defendant.getString(FIRST_NAME))
                .withCourtRoomName(courtRoomDetails.getCourtRoomName())
                .withHearingStartTime(startTime)
                .withCaseReference(caseReference);
        if (isWelsh) {
            alphabeticalListDefendantBuilder.withCourtRoomNameWelsh(courtRoomDetails.getWelshCourtRoomName());
        }
        return alphabeticalListDefendantBuilder.build();
    }

}
