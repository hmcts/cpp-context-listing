package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.listing.events.Hearing.hearing;

import uk.gov.justice.listing.events.BailStatus;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655"})
public class JsonEventUtils {

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String START_DATE = "startDate";
    private static final String ESTIMATE_MINUTES = "estimateMinutes";
    private static final String CASE_ID = "caseId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String JUDGE_ID = "judgeId";
    private static final String START_TIME = "startTime";
    private static final String HEARINGS = "hearings";
    private static final String DEFENDANTS = "defendants";
    private static final String PERSON_ID = "personId";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String BAIL_STATUS = "bailStatus";
    private static final String CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    private static final String DEFENCE_ORGANISATION = "defenceOrganisation";
    private static final String OFFENCES = "offences";
    private static final String OFFENCE_CODE = "offenceCode";
    private static final String END_DATE = "endDate";
    private static final String STATEMENT_OF_OFFENCE = "statementOfOffence";
    private static final String TITLE = "title";
    private static final String LEGISLATION = "legislation";

    private JsonEventUtils() {
    }

    public static List<Hearing> createHearingsFrom(final JsonObject caseJson) {
        final UUID caseId = fromString(caseJson.getString(CASE_ID));
        return caseJson.getJsonArray(HEARINGS)
                .getValuesAs(JsonObject.class).stream()
                .map((JsonObject hearingJson) -> createHearingFrom(hearingJson, caseId))
                .collect(toList());
    }

    public static List<Defendant> createDefendantsFrom(final JsonObject hearingJson) {
        return hearingJson.getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonEventUtils::createDefendantFrom)
                .collect(toList());
    }


    private static Hearing createHearingFrom(final JsonObject hearingJson, UUID caseId) {
        Hearing.Builder builder = hearing()
                .withId(fromString(hearingJson.getString(ID)))
                .withCaseId(caseId)
                .withCourtCentreId(fromString(hearingJson.getString(COURT_CENTRE_ID)))
                .withType(hearingJson.getString(TYPE))
                .withEstimateMinutes(hearingJson.getInt(ESTIMATE_MINUTES))
                .withStartDate(hearingJson.getString(START_DATE))
                .withDefendants(createDefendantsFrom(hearingJson));
        if (hearingJson.containsKey(JUDGE_ID)) {
            builder.withJudgeId(fromString(hearingJson.getString(JUDGE_ID)));
        }
        if (hearingJson.containsKey(COURT_ROOM_ID)) {
            builder.withCourtRoomId(fromString(hearingJson.getString(COURT_ROOM_ID)));
        }
        if (hearingJson.containsKey(START_TIME)) {
            builder.withStartTime(of(hearingJson.getString(START_TIME)));
        }
        return builder.build();
    }


    private static Defendant createDefendantFrom(final JsonObject defendantJson) {
        final Optional<String> custodyTimeLimit = Optional.ofNullable(defendantJson.getString(CUSTODY_TIME_LIMIT, null));
        final String bailStatusStr = defendantJson.getString(BAIL_STATUS, null);
        final BailStatus bailStatus = BailStatus.valueFor(bailStatusStr).isPresent() ? BailStatus.valueFor(bailStatusStr).get() : null;

        return Defendant.defendant()
                .withId(fromString(defendantJson.getString(ID)))
                .withPersonId(fromString(defendantJson.getString(PERSON_ID)))
                .withFirstName(defendantJson.getString(FIRST_NAME))
                .withLastName(defendantJson.getString(LAST_NAME))
                .withDateOfBirth(defendantJson.getString(DATE_OF_BIRTH))
                .withBailStatus(bailStatus)
                .withCustodyTimeLimit(custodyTimeLimit)
                .withDefenceOrganisation(defendantJson.getString(DEFENCE_ORGANISATION))
                .withOffences(createOffencesFrom(defendantJson))
                .build();
    }


    private static List<Offence> createOffencesFrom(final JsonObject defendantJson) {
        return defendantJson.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonEventUtils::createOffenceFrom)
                .collect(toList());
    }

    private static Offence createOffenceFrom(final JsonObject offenceJson) {
        return Offence.offence()
                .withId(fromString(offenceJson.getString(ID)))
                .withOffenceCode(offenceJson.getString(OFFENCE_CODE))
                .withStartDate(offenceJson.getString(START_DATE))
                .withEndDate(Optional.ofNullable(offenceJson.getString(END_DATE)))
                .withStatementOfOffence(createStatementOfOffenceFrom(offenceJson))
                .build();

    }

    private static StatementOfOffence createStatementOfOffenceFrom(final JsonObject offenceJson) {
        final JsonObject statementOfOffenceJson = offenceJson.getJsonObject(STATEMENT_OF_OFFENCE);
        return StatementOfOffence.statementOfOffence()
                .withTitle(statementOfOffenceJson.getString(TITLE))
                .withLegislation(statementOfOffenceJson.getString(LEGISLATION))
                .build();
    }

}
