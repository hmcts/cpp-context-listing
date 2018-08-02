package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.domain.*;

import javax.json.JsonObject;
import javax.json.JsonString;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

public class JsonDomainUtils {

    private static final boolean UNALLOCATED = false;

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String ESTIMATE_MINUTES = "estimateMinutes";
    private static final String CASE_ID = "caseId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String JUDGE_ID = "judgeId";
    private static final String START_TIME = "startTime";
    private static final String HEARINGS = "hearings";
    private static final String DEFENDANTS = "defendants";
    private static final String PERSON_ID = "personId";
    private static final String PERSON = "person";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String BAIL_STATUS = "bailStatus";
    private static final String CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    private static final String CUSTODY_TIME_LIMIT_DATE = "custodyTimeLimitDate";
    private static final String DEFENCE_ORGANISATION = "defenceOrganisation";
    private static final String OFFENCES = "offences";
    private static final String OFFENCE_CODE = "offenceCode";
    private static final String STATEMENT_OF_OFFENCE = "statementOfOffence";
    private static final String TITLE = "title";
    private static final String LEGISLATION = "legislation";
    private static final String UPDATED_OFFENCES = "updatedOffences";
    private static final String ADDED_OFFENCES = "addedOffences";
    private static final String DELETED_OFFENCES = "deletedOffences";
    private static final String DEFENDANT_ID = "defendantId";

    private JsonDomainUtils() {}

    public static List<Hearing> createHearingsFrom(final JsonObject caseJson) {
        final String caseId = caseJson.getString(CASE_ID);
        return caseJson.getJsonArray(HEARINGS)
                .getValuesAs(JsonObject.class).stream()
                .map((JsonObject hearingJson) -> createHearingFrom(hearingJson, caseId))
                .collect(toList());
    }

    public static List<Defendant> createDefendantsFrom(final JsonObject hearingJson) {
        return hearingJson.getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createDefendantFrom)
                .collect(toList());
    }

    public static List<Defendant> createDefendantsFromProgression(final JsonObject progressionDefendantsChanged) {
        return progressionDefendantsChanged.getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createDefendantFromProgression)
                .collect(toList());
    }

    public static List<CaseOffences> createUpdatedCaseBasesOffencesFrom(JsonObject progressionOffencesUpdated) {
        if (progressionOffencesUpdated.getJsonArray(UPDATED_OFFENCES) ==  null) {
            return emptyList();
        }
        return progressionOffencesUpdated.getJsonArray(UPDATED_OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createCaseOffencesFrom)
                .collect(toList());
    }

    public static List<Offence> createUpdatedOffencesFrom(JsonObject offencesUpdated) {
        return offencesUpdated.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createOffenceFrom)
                .collect(toList());
    }

    public static List<CaseOffences> createAddedCaseOffencesFrom(JsonObject progressionOffencesAdded) {
        if (progressionOffencesAdded.getJsonArray(ADDED_OFFENCES) ==  null) {
            return emptyList();
        }
        return progressionOffencesAdded.getJsonArray(ADDED_OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createCaseOffencesFrom)
                .collect(toList());
    }

    public static List<Offence> createAddedOffencesFrom(JsonObject offencesAdded) {
        return offencesAdded.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createOffenceFrom)
                .collect(toList());
    }


    public static List<CaseSimpleOffences> createDeletedCaseSimpleOffencesFrom(JsonObject progressionOffencesDeleted) {
        if (progressionOffencesDeleted.getJsonArray(DELETED_OFFENCES) ==  null) {
            return emptyList();
        }
        return progressionOffencesDeleted.getJsonArray(DELETED_OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createCaseSimpleOffencesFrom)
                .collect(toList());
    }

    public static List<SimpleOffence> createDeletedSimpleOffencesFrom(JsonObject offencesDeleted) {
        return offencesDeleted.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createSimpleOffenceFrom)
                .collect(toList());
    }

    private static CaseSimpleOffences createCaseSimpleOffencesFrom(JsonObject jsonObject) {
        final String caseId = jsonObject.getString(CASE_ID);
        final String defendantId = jsonObject.getString(DEFENDANT_ID);
        final List<SimpleOffence> offences = createSimpleOffencesFrom(jsonObject, defendantId);
        return CaseSimpleOffences.createCaseSimpleOffencesBuilder()
                .setCaseId(UUID.fromString(caseId))
                .setOffences(offences)
                .build();

    }

    private static CaseOffences createCaseOffencesFrom(JsonObject jsonObject) {
        final String caseId = jsonObject.getString(CASE_ID);
        final String defendantId = jsonObject.getString(DEFENDANT_ID);
        final List<Offence> offences = createOffencesFrom(jsonObject, defendantId);
        return CaseOffences.createCaseOffencesBuilder()
                .setCaseId(UUID.fromString(caseId))
                .setOffences(offences)
                .build();
    }

    private static List<SimpleOffence> createSimpleOffencesFrom(JsonObject jsonObject, String defendantId) {
        return jsonObject.getJsonArray(OFFENCES)
                .getValuesAs(JsonString.class).stream()
                .map(jsonObject1 -> createSimpleOffenceFrom(jsonObject1, defendantId))
                .collect(toList());
    }

    private static SimpleOffence createSimpleOffenceFrom(JsonString jsonObject, String defendantId) {
        return SimpleOffence.createSimpleOffenceBuilder()
                .setDefendantId(defendantId)
                .setId(jsonObject.getString())
                .build();
    }

    private static SimpleOffence createSimpleOffenceFrom(JsonObject jsonObject) {
        return SimpleOffence.createSimpleOffenceBuilder()
                .setDefendantId(jsonObject.getString(DEFENDANT_ID))
                .setId(jsonObject.getString(ID))
                .build();
    }

    private static Hearing createHearingFrom(final JsonObject hearingJson, String caseId) {
        final String endDate = hearingJson.getString(END_DATE, null);
        return new Hearing (
                hearingJson.getString(ID),
                caseId,
                hearingJson.getString(COURT_CENTRE_ID),
                hearingJson.getString(TYPE),
                LocalDates.from(hearingJson.getString(START_DATE)),
                endDate!=null ? LocalDates.from(endDate) : null,
                //endDate!=null ? Optional.of(LocalDates.from(endDate)) : Optional.empty(),
                hearingJson.getInt(ESTIMATE_MINUTES),
                hearingJson.getString(COURT_ROOM_ID, null),
                hearingJson.getString(JUDGE_ID, null),
                hearingJson.getString(START_TIME, null),
                createDefendantsFrom(hearingJson),
                UNALLOCATED);
    }

    private static Defendant createDefendantFrom(final JsonObject defendantJson) {
        final LocalDate custodyTimeLimit = getLocalDateOrNull(defendantJson, CUSTODY_TIME_LIMIT); // Optional field
        return new Defendant(
                defendantJson.getString(ID),
                defendantJson.getString(PERSON_ID),
                defendantJson.getString(FIRST_NAME),
                defendantJson.getString(LAST_NAME),
                getLocalDateOrNull(defendantJson, DATE_OF_BIRTH),
                defendantJson.getString(BAIL_STATUS),
                custodyTimeLimit,
                defendantJson.getString(DEFENCE_ORGANISATION),
                createOffencesFrom(defendantJson, defendantJson.getString(ID))
        );
    }

    private static List<Offence> createOffencesFrom(final JsonObject defendantJson, final String defendantId) {
        return defendantJson.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(offenceJson -> createOffenceFrom(offenceJson, defendantId))
                .collect(toList());
    }


    private static Defendant createDefendantFromProgression(final JsonObject defendantJson) {
        final LocalDate custodyTimeLimit = getLocalDateOrNull(defendantJson, CUSTODY_TIME_LIMIT_DATE); // Optional field
        return new Defendant(
                defendantJson.getString(ID),
                defendantJson.getJsonObject(PERSON).getString(ID),
                defendantJson.getJsonObject(PERSON).getString(FIRST_NAME),
                defendantJson.getJsonObject(PERSON).getString(LAST_NAME),
                getLocalDateOrNull(defendantJson.getJsonObject(PERSON), DATE_OF_BIRTH),
                defendantJson.getString(BAIL_STATUS),
                custodyTimeLimit,
                defendantJson.getString(DEFENCE_ORGANISATION),
                createOffencesFromProgression()
        );
    }

    private static List<Offence> createOffencesFromProgression() {
        // Defendant updates from Progression do not contain offence data
        return emptyList();
    }

    private static Offence createOffenceFrom(final JsonObject offenceJson, final String defendantId) {
        final LocalDate endDate = getLocalDateOrNull(offenceJson, END_DATE);
        return Offence.createOffenceBuilder()
                .setId(offenceJson.getString(ID))
                .setOffenceCode(offenceJson.getString(OFFENCE_CODE))
                .setStartDate(LocalDates.from(offenceJson.getString(START_DATE)))
                .setEndDate(endDate)
                .setStatementOfOffence(createStatementOfOffenceFrom(offenceJson))
                .setDefendantId(defendantId).build();
    }

    private static Offence createOffenceFrom(final JsonObject offenceJson) {
        final LocalDate endDate = getLocalDateOrNull(offenceJson, END_DATE);
        return Offence.createOffenceBuilder()
                .setId(offenceJson.getString(ID))
                .setOffenceCode(offenceJson.getString(OFFENCE_CODE))
                .setStartDate(LocalDates.from(offenceJson.getString(START_DATE)))
                .setEndDate(endDate)
                .setStatementOfOffence(createStatementOfOffenceFrom(offenceJson))
                .setDefendantId(offenceJson.getString((DEFENDANT_ID))).build();
    }

    private static StatementOfOffence createStatementOfOffenceFrom(final JsonObject offenceJson) {
        final JsonObject statementOfOffenceJson = offenceJson.getJsonObject(STATEMENT_OF_OFFENCE);
        return new StatementOfOffence(
                statementOfOffenceJson.getString(TITLE),
                statementOfOffenceJson.getString(LEGISLATION)
        );
    }

    private static LocalDate getLocalDateOrNull(final JsonObject jsonObject, final String fieldName) {
        return getString(jsonObject, fieldName)
                .map(LocalDate::parse).orElse(null);
    }
}

