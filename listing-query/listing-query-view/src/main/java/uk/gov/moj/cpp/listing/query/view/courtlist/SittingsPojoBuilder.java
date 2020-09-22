package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Optional.empty;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonPropertyUtils.getOptionalUUID;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CaseDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CourtApplicationDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.SittingKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SittingsPojoBuilder {

    public static final String LISTED_CASES = "listedCases";
    private static final Logger LOGGER = LoggerFactory.getLogger(SittingsPojoBuilder.class);
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String UNABLE_TO_GET_DEFAULT_START_OR_END_TIME = "Unable to get default start or end time";
    private static final String VIDEO_LINK_DETAILS = "videoLinkDetails";
    private static final String HAS_VIDEO_LINK = "hasVideoLink";
    private static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";

    private SittingsPojoBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Sitting> assignFlatHearingsToSittings(final List<FlatHearing> flatHearings, final LocalDate startDate, final String endDate) {

        final List<Sitting> sittings = new ArrayList<>();

        // Create new sitting for hearing or add to existing hearing

        for (final FlatHearing flatHearing : flatHearings) {
            logParamsForSittingCreation(startDate, endDate, flatHearing);

            final Optional<Sitting> sitting = findExistingSittingForFlatHearing(sittings, flatHearing);

            if (sitting.isPresent()) {

                sitting.get().getHearings().add(convertFlatHearing(flatHearing, startDate));
            } else {
                buildNewSitting(startDate, endDate, sittings, flatHearing);

            }
        }

        return sittings;
    }

    private static void logParamsForSittingCreation(final LocalDate startDate, final String endDate, final FlatHearing flatHearing) {
        LOGGER.debug("hearing date = {}", flatHearing.getHearingDate());
        LOGGER.debug("is week commencing = {}", flatHearing.isWeekCommencing());
        LOGGER.debug("startDate = {}", startDate);
        LOGGER.debug("endDate = {}", endDate);
        LOGGER.debug("caseHearings = {}", flatHearing.getCaseHearings());
    }

    private static void buildNewSitting(final LocalDate startDate, final String endDate, final List<Sitting> sittings, final FlatHearing flatHearing) {
        if (flatHearing.getHearingDate().equals(startDate) || flatHearing.isWeekCommencing() || isForMultiDay(flatHearing.getHearingDate(), startDate, endDate)) {
            LOGGER.debug("Creating new sitting for FlatHearing with {}", flatHearing.getHearingDate());
            sittings.add(createNewSitting(flatHearing, startDate));
        }
    }

    private static boolean isForMultiDay(final LocalDate hearingDate, final LocalDate startDate, final String endDate) {
        LOGGER.debug("isForMultiDay params {}, {}, {}", hearingDate, startDate, endDate);
        return StringUtils.isNotBlank(endDate) && isHearingDateValidForMultiDay(hearingDate, startDate, LocalDate.parse(endDate));
    }

    private static boolean isHearingDateValidForMultiDay(final LocalDate hearingDate, final LocalDate startDate, final LocalDate endDate) {
        return (hearingDate.isEqual(startDate) || hearingDate.isAfter(startDate)) && (hearingDate.isEqual(endDate) || hearingDate.isBefore(endDate));
    }

    private static Optional<Sitting> findExistingSittingForFlatHearing(final List<Sitting> sittings, final FlatHearing flatHearing) {

        for (final Sitting sitting : sittings) {
            if (sitting.getSittingKey().equals(buildSittingKey(flatHearing))) {
                return Optional.of(sitting);
            }
        }

        return empty();
    }

    private static Sitting createNewSitting(final FlatHearing flatHearing, final LocalDate startDate) {

        final Hearing hearing = convertFlatHearing(flatHearing, startDate);

        final List<Hearing> hearings = new ArrayList<>();
        hearings.add(hearing);

        return new Sitting(
                buildSittingKey(flatHearing),
                flatHearing.getJudiciary(),
                hearings,
                flatHearing.isWeekCommencing());
    }

    private static SittingKey buildSittingKey(final FlatHearing flatHearing) {

        final JsonArray judiciaryArray = flatHearing.getJudiciary();

        final Optional<UUID> judicialId = judiciaryArray.isEmpty() ? empty() :
                Optional.of(UUID.fromString(judiciaryArray.getJsonObject(0).getString("judicialId")));

        return new SittingKey(
                flatHearing.getHearingDate(),
                flatHearing.getCourtRoomId(),
                judicialId
        );
    }

    private static LocalDateTime getHearingStartTime(final JsonObject caseHearingsJson, final Optional<JsonObject> hearingDayJson, final Optional<JsonObject> hearingDateJson) {

        if (caseHearingsJson.containsKey(WEEK_COMMENCING_START_DATE)) {
            final String weekCommencingStartDate = caseHearingsJson.getString(WEEK_COMMENCING_START_DATE);
            return LocalDate.parse(weekCommencingStartDate).atTime(0, 0);
        } else {
            return hearingDayJson.map(jsonObject -> ZonedDateTime.parse(jsonObject
                    .getString(START_TIME)).toLocalDateTime()).orElseGet(() -> getDefaultStartOrEndTime(hearingDateJson));

        }
    }

    private static Optional<LocalDateTime> getHearingEndTime(final Optional<JsonObject> hearingDayJson, final JsonObject caseHearingsJson, final Optional<JsonObject> hearingDateJson) {

        if (caseHearingsJson.containsKey(WEEK_COMMENCING_END_DATE)) {
            final String weekCommencingEndDate = caseHearingsJson.getString(WEEK_COMMENCING_END_DATE);
            return Optional.ofNullable(LocalDate.parse(weekCommencingEndDate).atTime(0, 0));
        } else {
            return Optional.ofNullable(hearingDateJson.map(jsonObject -> ZonedDateTime.parse(jsonObject
                    .getString(END_TIME)).toLocalDateTime()).orElseGet(() -> getDefaultStartOrEndTime(hearingDayJson)));

        }
    }

    private static LocalDateTime getDefaultStartOrEndTime(final Optional<JsonObject> hearingDayJson) {

        if (hearingDayJson.isPresent()) {
            final String defaultTime = hearingDayJson.get().getString(END_TIME, "");
            if (StringUtils.isNotBlank(defaultTime)) {
                return ZonedDateTime.parse(defaultTime).toLocalDateTime();
            } else {
                try {
                    throw new IllegalArgumentException(UNABLE_TO_GET_DEFAULT_START_OR_END_TIME);
                } catch (final IllegalArgumentException e) {
                    LOGGER.info(UNABLE_TO_GET_DEFAULT_START_OR_END_TIME, e);

                }
            }
        }
        return null;
    }

    private static Hearing convertFlatHearing(final FlatHearing flatHearing, final LocalDate startDate) {

        final JsonObject caseHearingsJson = flatHearing.getCaseHearings();

        final Hearing hearing = new Hearing();

        Optional<JsonObject> jsonObjectByStartDate = empty();
        Optional<JsonObject> jsonObjectByHearingDate = empty();

        if (!flatHearing.isWeekCommencing()) {
            jsonObjectByStartDate = getJsonObjectBySpecifiedDate(caseHearingsJson, startDate.toString());
            jsonObjectByHearingDate = getJsonObjectBySpecifiedDate(caseHearingsJson, flatHearing.getHearingDate().toString());
        }

        hearing.setStartTime(getHearingStartTime(caseHearingsJson, jsonObjectByHearingDate, jsonObjectByStartDate));
        hearing.setEndTime(getHearingEndTime(jsonObjectByStartDate, caseHearingsJson, jsonObjectByHearingDate));
        hearing.setWeekCommencing(flatHearing.isWeekCommencing());

        hearing.setHearingType(caseHearingsJson.getJsonObject("type"));

        hearing.setRestrictFromCourtList(isHearingRestricted(caseHearingsJson));

        if (caseHearingsJson.containsKey("committingCourtCentreId")) {
            hearing.setCommittingCourtCentreId(
                    getOptionalUUID(caseHearingsJson, "committingCourtCentreId"));
        } else {
            hearing.setCommittingCourtCentreId(empty());
        }

        if (isCaseHearing(caseHearingsJson)) {

            hearing.setCaseDetails(buildCaseDetails(caseHearingsJson.getJsonArray(LISTED_CASES).getJsonObject(0)));
            hearing.setCourtApplicationDetails(empty());

        } else {
            hearing.setCourtApplicationDetails(buildCourtApplicationDetails(caseHearingsJson.getJsonArray("courtApplications").getJsonObject(0)));
            hearing.setCaseDetails(empty());
        }

        if (caseHearingsJson.containsKey(HAS_VIDEO_LINK) && caseHearingsJson.getBoolean(HAS_VIDEO_LINK)) {
            hearing.setHasVideoLink(true);
            if (caseHearingsJson.containsKey(VIDEO_LINK_DETAILS) && !caseHearingsJson.isNull(VIDEO_LINK_DETAILS)) {
                hearing.setVideoLinkDetails(caseHearingsJson.getString(VIDEO_LINK_DETAILS));
            }
        }
        return hearing;
    }

    private static Optional<JsonObject> getJsonObjectBySpecifiedDate(final JsonObject caseHearingsJson, final String date) {

        final Optional<JsonObject> jsonObject = caseHearingsJson.getJsonArray("hearingDays").getValuesAs(JsonObject.class)
                .stream().filter(c -> c.getString("hearingDate").equals(date))
                .findFirst();
        return jsonObject;
    }

    private static boolean isCaseHearing(final JsonObject caseHearingsJson) {
        return caseHearingsJson.containsKey(LISTED_CASES);
    }

    private static boolean isHearingRestricted(final JsonObject caseHearingsJson) {

        if (isCaseHearing(caseHearingsJson)) {
            return caseHearingsJson.getJsonArray(LISTED_CASES).getJsonObject(0).getBoolean("restrictFromCourtList");
        } else {
            return caseHearingsJson.getJsonArray("courtApplications").getJsonObject(0).getBoolean("restrictFromCourtList");
        }
    }

    private static Optional<CaseDetails> buildCaseDetails(final JsonObject caseDetailsJson) {

        final CaseDetails caseDetails = new CaseDetails();

        caseDetails.setCaseIdentifier(caseDetailsJson.getJsonObject("caseIdentifier"));

        caseDetails.setDefendants(caseDetailsJson.getJsonArray("defendants"));

        return Optional.of(caseDetails);
    }

    private static Optional<CourtApplicationDetails> buildCourtApplicationDetails(final JsonObject courtApplicationJson) {

        final CourtApplicationDetails courtApplicationDetails = new CourtApplicationDetails();

        courtApplicationDetails.setApplicationReference(courtApplicationJson.getString("applicationReference"));

        courtApplicationDetails.setApplicant(courtApplicationJson.getJsonObject("applicant"));

        courtApplicationDetails.setRespondents(courtApplicationJson.getJsonArray("respondents"));

        return Optional.of(courtApplicationDetails);
    }
}
