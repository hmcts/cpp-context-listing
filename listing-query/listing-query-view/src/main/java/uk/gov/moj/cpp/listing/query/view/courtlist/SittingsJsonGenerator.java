package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CaseDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CourtApplicationDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.util.List;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


public class SittingsJsonGenerator {

    private SittingsJsonGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonArrayBuilder buildSittingsJson(final List<Sitting> sittings) {

        final JsonArrayBuilder sittingsBuilder = JsonObjects.createArrayBuilder();

        sittings.forEach(s -> sittingsBuilder.add(buildSittingJson(s)));

        return sittingsBuilder;
    }

    private static JsonObject buildSittingJson(final Sitting sitting) {

        final JsonObjectBuilder sittingJson = JsonObjects.createObjectBuilder()
                .add("sittingDate", sitting.getSittingKey().getSittingDate().toString())
                .add("weekCommencing", sitting.isWeekCommencing());

        sitting.getSittingKey().getCourtRoomId().ifPresent(uuid -> sittingJson.add("courtRoomId", uuid.toString()));

        if (isNotEmpty(sitting.getCourtRoomName())) {
            sittingJson.add("courtRoomName", sitting.getCourtRoomName());
        }
        if (isNotEmpty(sitting.getWelshCourtRoomName())) {
            sittingJson.add("welshCourtRoomName", sitting.getWelshCourtRoomName());
        }

        sittingJson.add("judiciary", sitting.getJudiciaryJson());

        sittingJson.add("hearings", buildHearingsJsonArray(sitting.getHearings()));

        return sittingJson.build();
    }

    private static JsonArrayBuilder buildHearingsJsonArray(final List<Hearing> hearings) {

        final JsonArrayBuilder hearingsArray = JsonObjects.createArrayBuilder();

        hearings.forEach(h -> hearingsArray.add(buildHearingJson(h)));

        return hearingsArray;
    }

    private static JsonObject buildHearingJson(final Hearing hearing) {

        final JsonObjectBuilder hearingJsonBuilder = JsonObjects.createObjectBuilder()
                .add("startTime", hearing.getStartTime().toString())
                .add("hearingType", hearing.getHearingType())
                .add("restrictFromCourtList", hearing.isRestrictFromCourtList())
                .add("weekCommencing", hearing.isWeekCommencing());

        if (nonNull(hearing.hasVideoLink())) {
            hearingJsonBuilder.add("hasVideoLink", hearing.hasVideoLink());
        }

        if (isNotEmpty(hearing.getPublicListNote())) {
            hearingJsonBuilder.add("publicListNote", hearing.getPublicListNote());
        }

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
            if (nonNull(caseDetails.getProsecutor())) {
                hearingJsonBuilder.add("prosecutor", caseDetails.getProsecutor());

            }
        } else {

            final CourtApplicationDetails courtApplicationDetails = hearing.getCourtApplicationDetails().orElseThrow(IllegalStateException::new);

            hearingJsonBuilder
                    .add("applicationReference", courtApplicationDetails.getApplicationReference())
                    .add("applicant", courtApplicationDetails.getApplicant())
                    .add("respondents", courtApplicationDetails.getRespondents());

            // Add subject only if it is present
            if (courtApplicationDetails.getSubject() != null) {
                hearingJsonBuilder.add("subject", courtApplicationDetails.getSubject());
            }
        }

        return hearingJsonBuilder.build();
    }
}
