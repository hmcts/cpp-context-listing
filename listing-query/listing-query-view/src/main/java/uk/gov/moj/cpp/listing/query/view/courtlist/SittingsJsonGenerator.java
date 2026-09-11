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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonArray;


public class SittingsJsonGenerator {

    private static final DateTimeFormatter COURT_PROCEEDINGS_INITIATED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String PARTY_ID = "id";
    private static final String COURT_APPLICATION_PARTY_TYPE = "courtApplicationPartyType";
    private static final String PERSON_DEFENDANT_PARTY_TYPE = "PERSON_DEFENDANT";

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

            final String courtProceedingsInitiated = hearing.getStartTime().format(COURT_PROCEEDINGS_INITIATED_FORMATTER);

            hearingJsonBuilder.add("defendants", buildCourtApplicationDefendants(courtApplicationDetails, hearing.getCourtApplicationOffences(), courtProceedingsInitiated, hearing.getCourtApplicationLinkedCaseIds()));
        }

        return hearingJsonBuilder.build();
    }

    // Treat the application's applicant, subject and every respondent like a defendant. Deduped by
    // id only, so the same party isn't listed twice when e.g. applicant and subject are the same person.
    private static JsonArrayBuilder buildCourtApplicationDefendants(final CourtApplicationDetails courtApplicationDetails, final JsonArray applicationOffences, final String courtProceedingsInitiated, final JsonArray linkedCaseIds) {

        final JsonArrayBuilder defendants = JsonObjects.createArrayBuilder();
        final Set<String> addedPartyIds = new HashSet<>();

        if (nonNull(courtApplicationDetails.getRespondents())) {
            for (final JsonObject respondent : courtApplicationDetails.getRespondents().getValuesAs(JsonObject.class)) {
                addDefendant(defendants, addedPartyIds, respondent, applicationOffences, courtProceedingsInitiated, linkedCaseIds);
            }
        }

        addDefendant(defendants, addedPartyIds, courtApplicationDetails.getApplicant(), applicationOffences, courtProceedingsInitiated, linkedCaseIds);
        addDefendant(defendants, addedPartyIds, courtApplicationDetails.getSubject(), applicationOffences, courtProceedingsInitiated, linkedCaseIds);

        return defendants;
    }

    private static void addDefendant(final JsonArrayBuilder defendants, final Set<String> addedPartyIds, final JsonObject party, final JsonArray applicationOffences, final String courtProceedingsInitiated, final JsonArray linkedCaseIds) {
        if (isNull(party) || !isPersonDefendantParty(party)) {
            return;
        }
        final String partyId = party.getString(PARTY_ID, null);
        if (partyId == null || addedPartyIds.add(partyId)) {
            defendants.add(withMandatoryDefendantFields(party, applicationOffences, courtProceedingsInitiated, linkedCaseIds));
        }
    }

    private static JsonObjectBuilder withMandatoryDefendantFields(final JsonObject party, final JsonArray applicationOffences, final String courtProceedingsInitiated, final JsonArray linkedCaseIds) {
        final JsonObjectBuilder partyBuilder = JsonObjects.createObjectBuilder(party);

        if (!party.containsKey("offences")) {
            partyBuilder.add("offences", nonNull(applicationOffences) ? applicationOffences : JsonObjects.createArrayBuilder().build());
        }

        if (!party.containsKey("courtProceedingsInitiated")) {
            partyBuilder.add("courtProceedingsInitiated", courtProceedingsInitiated);
        }

        if (!party.containsKey("prosecutionCaseId") && nonNull(linkedCaseIds) && !linkedCaseIds.isEmpty()) {
            partyBuilder.add("prosecutionCaseId", linkedCaseIds.getString(0));
        }

        return partyBuilder;
    }

    private static boolean isPersonDefendantParty(final JsonObject party) {
        return PERSON_DEFENDANT_PARTY_TYPE.equals(party.getString(COURT_APPLICATION_PARTY_TYPE, null));
    }
}
