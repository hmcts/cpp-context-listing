package uk.gov.moj.cpp.listing.query.view.courtlist;

import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonPropertyUtils.getOptionalUUID;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CaseDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CourtApplicationDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.SittingKey;

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

    private SittingsPojoBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Sitting> assignFlatHearingsToSittings(final List<FlatHearing> flatHearings) {

        final List<Sitting> sittings = new ArrayList<>();

        // Create new sitting for hearing or add to existing hearing

        for (final FlatHearing flatHearing : flatHearings) {

            final Optional<Sitting> sitting = findExistingSittingForFlatHearing(sittings, flatHearing);

            if (sitting.isPresent()) {

                sitting.get().getHearings().add(convertCaseHearings(flatHearing.getCaseHearings()));
            } else {

                sittings.add(createNewSitting(flatHearing));
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

        return Optional.empty();
    }

    private static Sitting createNewSitting(final FlatHearing flatHearing) {

        final Hearing hearing = convertCaseHearings(flatHearing.getCaseHearings());

        final List<Hearing> hearings = new ArrayList<>();
        hearings.add(hearing);

        return new Sitting(
                buildSittingKey(flatHearing),
                flatHearing.getJudiciary(),
                hearings);
    }

    private static SittingKey buildSittingKey(final FlatHearing flatHearing) {

        final JsonArray judiciaryArray = flatHearing.getJudiciary();

        final Optional<UUID> judicialId = judiciaryArray.isEmpty() ? Optional.empty() :
                Optional.of(UUID.fromString(judiciaryArray.getJsonObject(0).getString("judicialId")));

        return new SittingKey(
                flatHearing.getHearingDate(),
                flatHearing.getCourtRoomId(),
                judicialId
        );
    }

    private static LocalDateTime getHearingStartTime(final JsonObject caseHearingsJson) {

        if (isHearingWeekCommencing(caseHearingsJson)) {
            return ZonedDateTime.parse(caseHearingsJson.getJsonArray("nonDefaultDays")
                    .getJsonObject(0).getString("startTime")).toLocalDateTime();
        } else {
            return ZonedDateTime.parse(caseHearingsJson.getJsonArray("hearingDays")
                    .getJsonObject(0).getString("startTime")).toLocalDateTime();
        }
    }

    private static Optional<LocalDateTime> getHearingEndTime(final JsonObject caseHearingsJson) {

        if (isHearingWeekCommencing(caseHearingsJson)) {
            final Optional<String> endTimeString = Optional.ofNullable(caseHearingsJson.getJsonArray("nonDefaultDays")
                    .getJsonObject(0).getString("endTime", null));

            return endTimeString.map(s -> ZonedDateTime.parse(s).toLocalDateTime());
        } else {
            return Optional.of(ZonedDateTime.parse(caseHearingsJson.getJsonArray("hearingDays")
                    .getJsonObject(0).getString("endTime")).toLocalDateTime());
        }
    }

    private static Hearing convertCaseHearings(final JsonObject caseHearingsJson) {

        final Hearing hearing = new Hearing();

        hearing.setStartTime(getHearingStartTime(caseHearingsJson));
        hearing.setEndTime(getHearingEndTime(caseHearingsJson));

        hearing.setHearingType(caseHearingsJson.getJsonObject("type"));

        hearing.setRestrictFromCourtList(isHearingRestricted(caseHearingsJson));

        if (caseHearingsJson.containsKey("committingCourtCentreId")) {
            hearing.setCommittingCourtCentreId(
                    getOptionalUUID(caseHearingsJson, "committingCourtCentreId"));
        } else {
            hearing.setCommittingCourtCentreId(Optional.empty());
        }

        if (isCaseHearing(caseHearingsJson)) {

            hearing.setCaseDetails(buildCaseDetails(caseHearingsJson.getJsonArray(LISTED_CASES).getJsonObject(0)));
            hearing.setCourtApplicationDetails(Optional.empty());

        } else {
            hearing.setCourtApplicationDetails(buildCourtApplicationDetails(caseHearingsJson.getJsonArray("courtApplications").getJsonObject(0)));
            hearing.setCaseDetails(Optional.empty());
        }

        return hearing;
    }

    private static boolean isCaseHearing(final JsonObject caseHearingsJson) {
        return caseHearingsJson.containsKey(LISTED_CASES);
    }

    private static boolean isHearingWeekCommencing(final JsonObject caseHearingsJson) {
        return caseHearingsJson.containsKey("weekCommencingStartDate");
    }

    private static boolean isHearingRestricted(final JsonObject caseHearingsJson) {

        if (isCaseHearing(caseHearingsJson)) {
            // TODO SCSL-89 Add support for hearings with multiple cases
            return caseHearingsJson.getJsonArray(LISTED_CASES).getJsonObject(0).getBoolean("restrictFromCourtList");
        } else {
            // TODO SCSL-89 Add support for hearings with multiple cases
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
