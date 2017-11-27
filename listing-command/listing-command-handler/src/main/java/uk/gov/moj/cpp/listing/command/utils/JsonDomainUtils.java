package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.time.LocalDate;
import java.util.List;

import javax.json.JsonObject;

public class JsonDomainUtils {

    private static final boolean UNALLOCATED = false;

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String START_DATE = "startDate";
    private static final String ESTIMATE_MINUTES = "estimateMinutes";
    private static final String CASE_ID = "caseId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
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

    private static Hearing createHearingFrom(final JsonObject hearingJson, String caseId) {
        return new Hearing (
                hearingJson.getString(ID),
                caseId,
                hearingJson.getString(COURT_CENTRE_ID),
                hearingJson.getString(TYPE),
                LocalDates.from(hearingJson.getString(START_DATE)),
                hearingJson.getInt(ESTIMATE_MINUTES),
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
                createOffencesFrom(defendantJson)
        );
    }

    private static List<Offence> createOffencesFrom(final JsonObject defendantJson) {
        return defendantJson.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(JsonDomainUtils::createOffenceFrom)
                .collect(toList());
    }

    private static Offence createOffenceFrom(final JsonObject offenceJson) {
        return new Offence(
                offenceJson.getString(ID),
                offenceJson.getString(OFFENCE_CODE),
                LocalDates.from(offenceJson.getString(START_DATE)),
                LocalDates.from(offenceJson.getString(END_DATE)),
                createStatementOfOffenceFrom(offenceJson)
        );
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
