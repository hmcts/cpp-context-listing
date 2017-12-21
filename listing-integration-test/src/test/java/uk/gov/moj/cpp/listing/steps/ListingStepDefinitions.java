package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
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
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudge;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CaseData;
import uk.gov.moj.cpp.listing.steps.data.CourtReferenceData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    private static final String MEDIA_TYPE_SEND_CASE_FOR_LISTING = "application/vnd.listing" +
            ".command.send-case-for-listing+json";

    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING = "application/vnd.listing" +
            ".command.update-hearing-for-listing+json";

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";

    private static final String FIELD_PERSON_ID = "personId";
    private static final String FIELD_DATE_BIRTH = "dateOfBirth";
    private static final String FIELD_OFFENCE_CODE = "offenceCode";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_LEGISLATION = "legislation";
    private static final String FIELD_STATEMENT_OF_OFFENCE = "statementOfOffence";
    private static final String FIELD_BAIL_STATUS = "bailStatus";
    private static final String FIELD_DEFENCE_ORGANISATION = "defenceOrganisation";
    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_URN = "urn";
    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_OFFENCES = "offences";
    private static final String FIELD_DEFENDANTS = "defendants";
    private static final String FIELD_HEARINGS = "hearings";
    private static final String FIELD_HEARING_TYPE = "type";
    private static final String FIELD_HEARING_START_DATE = "startDate";
    private static final String FIELD_HEARING_ESTIMATE_MINUTES = "estimateMinutes";
    private static final String FIELD_CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    private static final String FIELD_JUDGE_ID = "judgeId";
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_HEARING_DOT_ID = "hearing.id";
    private static final String FIELD_HEARING_DOT_START_DATE_TIME = "hearing.startDateTime";
    private static final String FIELD_COURT_ROOM_ID = "courtRoomId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_NOT_BEFORE = "notBefore";
    private static final String FIELD_ESTIMATE_MINUTES = "estimateMinutes";
    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command" +
            ".send-case-for-listing";
    private static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING = "listing.command" +
            ".update-hearing-for-listing";
    private static final String NOT_A_BOOLEAN = "not_a_boolean";
    private static final boolean UNALLOCATED = false;
    private static final String FIELD_GENERIC_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";
    private static final Boolean ALLOCATED = Boolean.TRUE;
    private static final String DEFAULT_START_TIME = "10:30";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Given steps

    public static void givenAUserHasLoggedInAsAListingOfficers(final UUID validUserId) {
        setLoggedInUser(validUserId);
    }

    public static void andReferenceDataForCourtsAreAvailable(CourtReferenceData courtReferenceData) {
        stubGetReferenceDataCourtCentre(courtReferenceData);
        stubGetReferenceDataJudge(courtReferenceData.getJudge());
    }

    // When steps

    public static void whenCaseIsSubmittedForListing(final CaseData caseData) {
        final String listCaseForHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_SEND_CASE_FOR_LISTING)));

        final JsonObjectBuilder caseDataJson = prepareJsonForCaseData(caseData);

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_SEND_CASE_FOR_LISTING,
                caseDataJson.build().toString(), getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public static void whenHearingIsUpdatedForListing(final UpdatedHearingData updatedHearingData) {
        final String updatedHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        final JsonObjectBuilder caseDataJson = prepareJsonForUpdatedHearingData(updatedHearingData);

        final Response response = restClient.postCommand(updatedHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                caseDataJson.build().toString(), getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

    }

    // Then steps

    public static void thenCaseSentForListingPublicEventShouldBePublished(
            final CaseData caseData,
            final MessageConsumerClient publicMessageConsumer) throws JMSException {

        verifyInPublicMQ(FIELD_CASE_ID, caseData.getCaseId().toString(), publicMessageConsumer);
    }

    public static void thenHearingScheduledPublicEventShouldBePublished(
            final UUID hearingId,
            final MessageConsumerClient publicMessageConsumer) throws JMSException {

        verifyInPublicMQ(FIELD_HEARING_DOT_ID, hearingId.toString(), publicMessageConsumer);
    }

    public static void thenHearingScheduledPublicEventShouldBePublishedWithDefaultStartTime(
            final UpdatedHearingData updatedHearingData,
            final MessageConsumerClient publicMessageConsumer) throws JMSException{

        final LocalDateTime startDateTime = LocalDateTime.of(LocalDate.parse(updatedHearingData.getStartDate()), LocalTime.parse(DEFAULT_START_TIME));
        verifyInPublicMQ(FIELD_HEARING_DOT_START_DATE_TIME,  DATE_TIME_FORMAT.format(startDateTime), publicMessageConsumer);
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

    public static void thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId(final CaseData caseData, final UpdatedHearingData updatedHearingData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));
        final Filter myFilter = filter(where("id").is(caseData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                hasNoJsonPath("$.hearings[0].judgeId"),
                                withJsonPath("$.hearings[0].courtRoomId",
                                        equalTo(updatedHearingData.getCourtRoomId().toString())),
                                withJsonPath("$.hearings[0].type",
                                        equalTo(updatedHearingData.getType())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate().toString())),
                                withJsonPath("$.hearings[0].startTime",
                                        equalTo(updatedHearingData.getStartTime().toString())),
                                hasNoJsonPath("$.hearings[0].notBefore"),
                                withJsonPath("$.hearings[0].estimateMinutes",
                                        equalTo(updatedHearingData.getEstimateMinutes()))
                        )));
    }

    public static void thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomId(final CaseData caseData, final UpdatedHearingData updatedHearingData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));
        final Filter myFilter = filter(where("id").is(caseData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                hasNoJsonPath("$.hearings[0].courtRoomId"),
                                withJsonPath("$.hearings[0].judgeId",
                                        equalTo(updatedHearingData.getJudgeId().toString())),
                                withJsonPath("$.hearings[0].type",
                                        equalTo(updatedHearingData.getType())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate().toString())),
                                withJsonPath("$.hearings[0].startTime",
                                        equalTo(updatedHearingData.getStartTime().toString())),
                                hasNoJsonPath("$.hearings[0].notBefore"),
                                withJsonPath("$.hearings[0].estimateMinutes",
                                        equalTo(updatedHearingData.getEstimateMinutes()))
                        )));
    }

    public static void thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomAndStartTime(final CaseData caseData, final UpdatedHearingData updatedHearingData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));
        final Filter myFilter = filter(where("id").is(caseData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                hasNoJsonPath("$.hearings[0].courtRoomId"),
                                hasNoJsonPath("$.hearings[0].startTime"),
                                withJsonPath("$.hearings[0].judgeId",
                                        equalTo(updatedHearingData.getJudgeId().toString())),
                                withJsonPath("$.hearings[0].type",
                                        equalTo(updatedHearingData.getType())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate().toString())),
                                hasNoJsonPath("$.hearings[0].notBefore"),
                                withJsonPath("$.hearings[0].estimateMinutes",
                                        equalTo(updatedHearingData.getEstimateMinutes()))
                        )));
    }

    public static void thenAllocatedHearingsForACourtCentreShouldContainUpdatedHearingData(final CaseData caseData, final UpdatedHearingData updatedHearingData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), ALLOCATED));
        final Filter myFilter = filter(where("id").is(caseData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].judgeId",
                                        equalTo(updatedHearingData.getJudgeId().toString())),
                                withJsonPath("$.hearings[0].courtRoomId",
                                        equalTo(updatedHearingData.getCourtRoomId().toString())),
                                withJsonPath("$.hearings[0].type",
                                        equalTo(updatedHearingData.getType())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate().toString())),
                                withJsonPath("$.hearings[0].startTime",
                                        equalTo(updatedHearingData.getStartTime().toString())),
                                withJsonPath("$.hearings[0].notBefore",
                                        equalTo(updatedHearingData.getNotBefore())),
                                withJsonPath("$.hearings[0].estimateMinutes",
                                        equalTo(updatedHearingData.getEstimateMinutes()))
                        )));
    }

    public static void thenAllocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithDefaultStartTime(final CaseData caseData, final UpdatedHearingData updatedHearingData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), ALLOCATED));
        final Filter myFilter = filter(where("id").is(caseData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].judgeId",
                                        equalTo(updatedHearingData.getJudgeId().toString())),
                                withJsonPath("$.hearings[0].courtRoomId",
                                        equalTo(updatedHearingData.getCourtRoomId().toString())),
                                withJsonPath("$.hearings[0].type",
                                        equalTo(updatedHearingData.getType())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate().toString())),
                                withJsonPath("$.hearings[0].startTime",
                                        equalTo(LocalTime.parse(DEFAULT_START_TIME).toString())),
                                withJsonPath("$.hearings[0].notBefore",
                                        equalTo(updatedHearingData.getNotBefore())),
                                withJsonPath("$.hearings[0].estimateMinutes",
                                        equalTo(updatedHearingData.getEstimateMinutes()))
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


    // Private Helpers

    private static JsonObjectBuilder prepareJsonForCaseData(final CaseData caseData) {
        final JsonObjectBuilder caseDataJson = createObjectBuilder();

        return caseDataJson.add(FIELD_CASE_ID, caseData.getCaseId().toString())
                .add(FIELD_URN, caseData.getUrn())
                .add(FIELD_HEARINGS, prepareJsonForHearings(caseData.getHearingData()));
    }

    private static JsonObjectBuilder prepareJsonForUpdatedHearingData(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, updatedHearingData.getType())
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_NOT_BEFORE, updatedHearingData.getNotBefore())
                .add(FIELD_ESTIMATE_MINUTES, updatedHearingData.getEstimateMinutes());

        addField(builder, FIELD_JUDGE_ID, getStringOrNull(updatedHearingData.getJudgeId()));
        addField(builder, FIELD_COURT_ROOM_ID, getStringOrNull(updatedHearingData.getCourtRoomId()));
        addField(builder, FIELD_START_TIME, updatedHearingData.getStartTime());

        return builder;
    }

    private static void addField(JsonObjectBuilder builder, String fieldName, String value) {
        if (value != null) {
            builder.add(fieldName, value);
        } else {
            builder.addNull(fieldName);
        }
    }

    private static String getStringOrNull(UUID id) {
        return id!=null ? id.toString() : null;
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

    private static void verifyInPublicMQ(final String key, final String expectedValue, final MessageConsumerClient
            publicMessageConsumer) throws JMSException {
        JsonPath response = new JsonPath(publicMessageConsumer.retrieveMessage().get());
        assertThat(response.get(key), CoreMatchers.equalTo(expectedValue));
    }

}
