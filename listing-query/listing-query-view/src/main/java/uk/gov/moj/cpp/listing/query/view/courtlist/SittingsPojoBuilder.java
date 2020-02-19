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

public class SittingsPojoBuilder {

    public static final String LISTED_CASES = "listedCases";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";

    private SittingsPojoBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Sitting> assignFlatHearingsToSittings(final List<FlatHearing> flatHearings, final LocalDate startDate) {

        final List<Sitting> sittings = new ArrayList<>();

        // Create new sitting for hearing or add to existing hearing

        for (final FlatHearing flatHearing : flatHearings) {

            final Optional<Sitting> sitting = findExistingSittingForFlatHearing(sittings, flatHearing);

            if (sitting.isPresent()) {

                sitting.get().getHearings().add(convertFlatHearing(flatHearing, startDate));
            } else {
                if(flatHearing.getHearingDate().equals(startDate)){
                    sittings.add(createNewSitting(flatHearing, startDate));
                }

            }
        }

        return sittings;
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

    private static LocalDateTime getHearingStartTime(final Optional<JsonObject> hearingDayJson, final JsonObject caseHearingsJson, final Optional<JsonObject> hearingDateJson) {

        if (isHearingWeekCommencing(caseHearingsJson)) {
            return ZonedDateTime.parse(caseHearingsJson.getJsonArray("nonDefaultDays")
                    .getJsonObject(0).getString(START_TIME)).toLocalDateTime();
        } else {
            return hearingDayJson.map(jsonObject -> ZonedDateTime.parse(jsonObject
                    .getString(START_TIME)).toLocalDateTime()).orElseGet(() -> getDefaultStartOrEndTime(hearingDateJson, caseHearingsJson, START_TIME));

        }
    }

    private static Optional<LocalDateTime> getHearingEndTime(final Optional<JsonObject> hearingDayJson, final JsonObject caseHearingsJson, final Optional<JsonObject> hearingDateJson) {

        if (isHearingWeekCommencing(caseHearingsJson)) {
            final Optional<String> endTimeString = Optional.ofNullable(caseHearingsJson.getJsonArray("nonDefaultDays")
                    .getJsonObject(0).getString(END_TIME, null));

            return endTimeString.map(s -> ZonedDateTime.parse(s).toLocalDateTime());
        } else {
            return Optional.ofNullable(hearingDayJson.map(jsonObject -> ZonedDateTime.parse(jsonObject
                    .getString(END_TIME)).toLocalDateTime()).orElseGet(() -> getDefaultStartOrEndTime(hearingDateJson, caseHearingsJson, END_TIME)));

        }
    }

    private static LocalDateTime getDefaultStartOrEndTime(final Optional<JsonObject> hearingDateJson, final JsonObject caseHearingsJson, final String time){

        return hearingDateJson.map(jsonObject -> ZonedDateTime.parse(jsonObject.getString(time)).toLocalDateTime()).orElseGet(() -> ZonedDateTime.parse(caseHearingsJson.getJsonArray("hearingDays")
                .getJsonObject(0).getString(START_TIME)).toLocalDateTime());
    }

    private static Hearing convertFlatHearing(final FlatHearing flatHearing, final LocalDate startDate) {

        final JsonObject caseHearingsJson = flatHearing.getCaseHearings();

        final Hearing hearing = new Hearing();

        Optional<JsonObject> jsonObjectByStartDate = empty();
        Optional<JsonObject> jsonObjectByHearingDate = empty();

        if(!flatHearing.isWeekCommencing()){
            jsonObjectByStartDate = getJsonObjectBySpecifiedDate(caseHearingsJson, startDate.toString());
            jsonObjectByHearingDate = getJsonObjectBySpecifiedDate(caseHearingsJson, flatHearing.getHearingDate().toString());
        }

        hearing.setStartTime(getHearingStartTime(jsonObjectByStartDate, caseHearingsJson, jsonObjectByHearingDate));
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

    private static boolean isHearingWeekCommencing(final JsonObject caseHearingsJson) {
        return caseHearingsJson.containsKey("weekCommencingStartDate");
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
