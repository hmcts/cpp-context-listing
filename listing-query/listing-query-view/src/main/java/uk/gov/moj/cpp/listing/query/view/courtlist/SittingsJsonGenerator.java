package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CaseDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.CourtApplicationDetails;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


public class SittingsJsonGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String PARTY_ID = "id";

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
                .add("startTime", hearing.getStartTime().format(DATE_TIME_FORMATTER))
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
            hearingJsonBuilder.add("endTime", hearing.getEndTime().orElseThrow(IllegalStateException::new).format(DATE_TIME_FORMATTER));
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
                    .add("caseIdentifier", JsonObjects.createObjectBuilder()
                            .add("caseReference", courtApplicationDetails.getApplicationReference()))
                    .add("applicant", courtApplicationDetails.getApplicant())
                    .add("respondents", courtApplicationDetails.getRespondents());

            // Add subject only if it is present
            if (courtApplicationDetails.getSubject() != null) {
                hearingJsonBuilder.add("subject", courtApplicationDetails.getSubject());
            }

            hearingJsonBuilder.add("defendants", buildCourtApplicationDefendants(courtApplicationDetails));
        }

        return hearingJsonBuilder.build();
    }

    // Treat the application's applicant, subject and every respondent like a defendant. Deduped by
    // id only, so the same party isn't listed twice when e.g. applicant and subject are the same person.
    private static JsonArrayBuilder buildCourtApplicationDefendants(final CourtApplicationDetails courtApplicationDetails) {

        final JsonArrayBuilder defendants = JsonObjects.createArrayBuilder();
        final Set<String> addedPartyIds = new HashSet<>();

        if (nonNull(courtApplicationDetails.getRespondents())) {
            for (final JsonObject respondent : courtApplicationDetails.getRespondents().getValuesAs(JsonObject.class)) {
                addDefendant(defendants, addedPartyIds, respondent);
            }
        }

        addDefendant(defendants, addedPartyIds, courtApplicationDetails.getApplicant());
        addDefendant(defendants, addedPartyIds, courtApplicationDetails.getSubject());

        return defendants;
    }

    private static void addDefendant(final JsonArrayBuilder defendants, final Set<String> addedPartyIds, final JsonObject party) {
        if (isNull(party)) {
            return;
        }
        final String partyId = party.getString(PARTY_ID, null);
        if (partyId == null || addedPartyIds.add(partyId)) {
            defendants.add(party);
        }
    }
}
