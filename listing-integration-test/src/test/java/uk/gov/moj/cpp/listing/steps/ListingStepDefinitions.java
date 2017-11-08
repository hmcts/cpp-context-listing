package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CaseData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;

import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;

@SuppressWarnings("unchecked")
public class ListingStepDefinitions extends AbstractIT {

    private static final String MEDIA_TYPE_LIST_CASE_FOR_HEARING = "application/vnd.listing" +
            ".command.send-case-for-listing+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";


    private static final String FIELD_GENERIC_ID = "id";
    private static final String FIELD_PERSON_ID = "personId";
    private static final String FIELD_DATE_BIRTH = "dateOfBirth";
    private static final String FIELD_OFFENCE_CODE = "offenceCode";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_LEGISLATION = "legislation";
    private static final String FIELD_STATEMENT_OF_OFFENCE = "statementOfOffence";
    private static final String FIELD_BAIL_STATUS = "bailStatus";
    private static final String FIELD_DEFENCE_ORGANISATION = "defenceOrganisation";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";
    private static final String FIELD_CASE_PROGRESSION_ID = "caseProgressionId";
    private static final String FIELD_URN = "urn";
    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_OFFENCES = "offences";
    private static final String FIELD_DEFENDANTS = "defendants";
    private static final String FIELD_HEARINGS = "hearings";
    private static final String FIELD_HEARING_TYPE = "type";
    private static final String FIELD_HEARING_START_DATE = "startDate";
    private static final String FIELD_HEARING_ESTIMATE_MINUTES = "estimateMinutes";
    private static final String FIELD_CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command" +
            ".send-case-for-listing";
    private static final String NOT_A_BOOLEAN = "not_a_boolean";
    private static final boolean UNALLOCATED = false;

    public static void givenAUserHasLoggedInAsAListingOfficers(final UUID validUserId) {
        setLoggedInUser(validUserId);
    }

    public static void whenCaseIsSubmittedForListing(final CaseData caseData) {
        final String listCaseForHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_SEND_CASE_FOR_LISTING)));

        final JsonObjectBuilder caseDataJson = prepareJsonForCaseData(caseData);

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_CASE_FOR_HEARING,
                caseDataJson.build().toString(), getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    public static void thenCaseSentForListingPublicEventShouldBePublished(
            final CaseData caseData,
            final MessageConsumerClient publicMessageConsumer) throws JMSException {

        verifyInPublicMQ(caseData.getCaseProgressionId().toString(), publicMessageConsumer);

    }

    private static JsonObjectBuilder prepareJsonForCaseData(final CaseData caseData) {
        final JsonObjectBuilder caseDataJson = createObjectBuilder();

        return caseDataJson.add(FIELD_CASE_PROGRESSION_ID, caseData.getCaseProgressionId().toString())
                .add(FIELD_URN, caseData.getUrn())
                .add(FIELD_HEARINGS, prepareJsonForHearings(caseData.getHearingData()));
    }

    private static JsonArrayBuilder prepareJsonForHearings(final List<HearingData> hearings) {
        return hearings.stream()
                .map(hearing -> {
                            return createObjectBuilder().add(FIELD_GENERIC_ID, hearing.getId().toString())
                                    .add(FIELD_COURT_CENTRE_ID, hearing.getCourtCentreId())
                                    .add(FIELD_HEARING_TYPE, hearing.getHearingType())
                                    .add(FIELD_HEARING_START_DATE, hearing.getHearingStartDate().toString())
                                    .add(FIELD_HEARING_ESTIMATE_MINUTES, hearing.getHearingEstimateMinutes())
                                    .add(FIELD_DEFENDANTS, prepareJsonForDefendants(hearing.getDefendants()));
                        }
                )
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static JsonArrayBuilder prepareJsonForOffences(final List<OffenceData> offences) {
        return offences.stream()
                .map(offenceData -> {

                    JsonObjectBuilder statementOfOffenceBuilder = createObjectBuilder()
                            .add(FIELD_TITLE, offenceData.getStatementOfOffenceTitle())
                            .add(FIELD_LEGISLATION, offenceData.getStatementOfOffenceLegislation());

                    return createObjectBuilder()
                            .add(FIELD_GENERIC_ID, offenceData.getOffenceId().toString())
                            .add(FIELD_OFFENCE_CODE, offenceData.getOffenceCode())
                            .add(FIELD_START_DATE, offenceData.getStartDate().toString())
                            .add(FIELD_END_DATE, offenceData.getEndDate().toString())
                            .add(FIELD_STATEMENT_OF_OFFENCE, statementOfOffenceBuilder);
                        }
                )
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static JsonArrayBuilder prepareJsonForDefendants(final List<DefendantData> defendants) {
        return defendants.stream()
                .map(defendantData -> createObjectBuilder()
                        .add(FIELD_GENERIC_ID, defendantData.getDefendantId().toString())
                        .add(FIELD_PERSON_ID, defendantData.getPersonId().toString())
                        .add(FIELD_FIRST_NAME, defendantData.getFirstName())
                        .add(FIELD_LAST_NAME, defendantData.getLastName())
                        .add(FIELD_DATE_BIRTH, defendantData.getDateOfBirth().toString())
                        .add(FIELD_CUSTODY_TIME_LIMIT, defendantData.getCustodyTimeLimit().toString())
                        .add(FIELD_BAIL_STATUS, defendantData.getBailStatus())
                        .add(FIELD_DEFENCE_ORGANISATION, defendantData.getDefenceOrganisation())
                        .add(FIELD_OFFENCES, prepareJsonForOffences(defendantData.getOffences()))
                )
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }


    private static void verifyInPublicMQ(final String caseId, final MessageConsumerClient
            publicMessageConsumer) throws JMSException {
        JsonPath response = new JsonPath(publicMessageConsumer.retrieveMessage().get());
        System.out.println(response);
        assertThat(response.get(FIELD_CASE_PROGRESSION_ID), CoreMatchers.equalTo(caseId));
    }

    public static void thenUnallocatedHearingsForACourtCentreShouldContainTwoExpectedHearingsWhenQueried(final CaseData caseData, final CaseData caseDataNew) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));
        final Filter myFilterInitial = filter(where("id").contains(caseData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilterInitial = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilterInitial);

        final Filter myFilterNew = filter(where("id").contains(caseDataNew.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilterNew = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilterNew);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath(hearingFilterNew)),
                        payload().isJson(withJsonPath(hearingFilterInitial)));

    }

    public static void thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(final CaseData caseData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));
        final Filter myFilter = filter(where("id").is(caseData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingFilter),
                                withJsonPath("$.hearings[0].defendants[0].firstName",
                                        equalTo(caseData.getHearingData().get(0)
                                                .getDefendants().get(0).getFirstName())),
                                withJsonPath("$.hearings[0].defendants[0].lastName",
                                        equalTo(caseData.getHearingData().get(0)
                                                .getDefendants().get(0).getLastName()))
                        )));
    }

    public static void thenQueryValidationFailureOccursWhenQueried(final CaseData caseData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData
                        .getHearingData().get(0).getCourtCentreId(), NOT_A_BOOLEAN));

        final Response response = restClient.query(searchHearingUrl,
                MEDIA_TYPE_SEARCH_HEARINGS_JSON, getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_BAD_REQUEST));

    }
}
