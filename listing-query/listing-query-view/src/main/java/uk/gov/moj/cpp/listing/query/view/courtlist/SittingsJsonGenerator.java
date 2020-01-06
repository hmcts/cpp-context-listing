package uk.gov.moj.cpp.listing.query.view.courtlist;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CaseDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CourtApplicationDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class SittingsJsonGenerator {

    private SittingsJsonGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonArrayBuilder buildSittingsJson(final List<Sitting> sittings) {

        final JsonArrayBuilder sittingsBuilder = Json.createArrayBuilder();

        sittings.forEach(s -> sittingsBuilder.add(buildSittingJson(s)));

        return sittingsBuilder;
    }

    private static JsonObject buildSittingJson(final Sitting sitting) {

        final JsonObjectBuilder sittingJson = Json.createObjectBuilder()
                .add("sittingDate", sitting.getSittingKey().getSittingDate().toString())
                .add ("weekCommencing", sitting.isWeekCommencing())
                .add("judiciary", sitting.getJudiciaryJson())
                .add("hearings", buildHearingsJsonArray(sitting.getHearings()));

        final Optional<UUID> courtRoomId = sitting.getSittingKey().getCourtRoomId();

        courtRoomId.ifPresent(uuid -> sittingJson.add("courtRoomId", uuid.toString()));

        return sittingJson.build();
    }

    private static JsonArrayBuilder buildHearingsJsonArray(final List<Hearing> hearings) {

        final JsonArrayBuilder hearingsArray = Json.createArrayBuilder();

        hearings.forEach(h -> hearingsArray.add(buildHearingJson(h)));

        return hearingsArray;
    }

    private static JsonObject buildHearingJson(final Hearing hearing) {

        final JsonObjectBuilder hearingJsonBuilder = Json.createObjectBuilder()
                .add("startTime", hearing.getStartTime().toString())
                .add("hearingType", hearing.getHearingType())
                .add("restrictFromCourtList", hearing.isRestrictFromCourtList())
                .add("weekCommencing", hearing.isWeekCommencing());

        if (hearing.getEndTime().isPresent()) {
            hearingJsonBuilder.add("endTime", hearing.getEndTime().orElseThrow(IllegalStateException::new).toString());
        }

        if (hearing.getCommittingCourtCentreId().isPresent()) {
            hearingJsonBuilder.add("committingCourtCentreId", hearing.getCommittingCourtCentreId().toString());
        }

        if (hearing.getCaseDetails().isPresent()) {

            final CaseDetails caseDetails = hearing.getCaseDetails().orElseThrow(IllegalStateException::new);

            hearingJsonBuilder
                    .add("caseIdentifier", caseDetails.getCaseIdentifier())
                    .add("defendants", caseDetails.getDefendants());
        } else {

            final CourtApplicationDetails courtApplicationDetails = hearing.getCourtApplicationDetails().orElseThrow(IllegalStateException::new);

            hearingJsonBuilder
                    .add("applicationReference", courtApplicationDetails.getApplicationReference())
                    .add("applicant", courtApplicationDetails.getApplicant())
                    .add("respondents", courtApplicationDetails.getRespondents());
        }

        return hearingJsonBuilder.build();
    }
}
