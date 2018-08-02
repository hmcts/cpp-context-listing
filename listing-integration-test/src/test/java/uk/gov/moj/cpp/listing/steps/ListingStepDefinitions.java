package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.random.Generator;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CaseData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

@SuppressWarnings("unchecked")
public class ListingStepDefinitions extends AbstractIT {

    private static final String MEDIA_TYPE_SEND_CASE_FOR_LISTING = "application/vnd.listing" +
            ".command.send-case-for-listing+json";

    private static final String MEDIA_TYPE_DEFENDANTS_CHANGED = "application/vnd.public.progression"
            + ".case-defendant-changed+json";

    private static final String MEDIA_TYPE_DEFENDANT_OFFENCES_CHANGED = "application/vnd.public.progression" +
            ".case-defendant-offences-changed+json";

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
    private static final String FIELD_HEARING_END_DATE = "endDate";
    private static final String FIELD_HEARING_ESTIMATE_MINUTES = "estimateMinutes";
    private static final String FIELD_CUSTODY_TIME_LIMIT = "custodyTimeLimit";
    private static final String FIELD_CUSTODY_TIME_LIMIT_Date = "custodyTimeLimitDate";
    private static final String FIELD_JUDGE_ID = "judgeId";
    private static final String FIELD_HEARING_DOT_ID = "hearing.id";
    private static final String FIELD_COURT_ROOM_ID = "courtRoomId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_START_TIMES = "startTimes";
    private static final String FIELD_NON_SITTING_DAYS = "nonSittingDays";
    private static final String FIELD_NATIONALITY = "nationality";
    private static final String FIELD_PERSON = "person";
    private static final String FIELD_ADDRESS_LINE_1 = "address1";
    private static final String FIELD_ADDRESS_LINE_2 = "address2";
    private static final String FIELD_ADDRESS_LINE_3 = "address3";
    private static final String FIELD_ADDRESS_LINE_4 = "address4";
    private static final String FIELD_ADDRESS_POSTCODE = "postCode";
    private static final String FIELD_GENDER = "gender";
    private static final String FIELD_WORK_TELE = "workTelephone";
    private static final String FIELD_HOME_TELE = "homeTelephone";
    private static final String FIELD_MOBILE = "mobile";
    private static final String FIELD_FAX = "fax";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_INTERPRETER = "interpreter";
    private static final String FIELD_INTERPRETER_NAME = "name";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_INTERPRETER_NEEDED = "needed";
    private static final boolean INTERPRETER_NEEDED = false;
    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command" +
            ".send-case-for-listing";
    private static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING = "listing.command" +
            ".update-hearing-for-listing";
    private static final String PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    private static final String PUBLIC_EVENT_PROGRESSION_DEFENDANT_OFFENCES_CHANGED = "public.progression.defendant-offences-changed";
    private static final String NOT_A_BOOLEAN = "not_a_boolean";
    private static final boolean UNALLOCATED = false;
    private static final String FIELD_GENERIC_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";
    private static final Boolean ALLOCATED = Boolean.TRUE;
    public static final Generator<String> STRING = new StringGenerator();
    private static final String FIELD_MODIFIED_DATE = "modifiedDate";
    private static final String FIELD_UPDATED_OFFENCES = "updatedOffences";
    private static final String FIELD_DELETED_OFFENCES = "deletedOffences";
    private static final String FIELD_ADDED_OFFENCES = "addedOffences";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_WORDING = "wording";
    private static final String FIELD_COUNT = "count";
    private static final String FIELD_CONVICTION_DATE = "convictionDate";

    // Given steps

    public static void givenAUserHasLoggedInAsAListingOfficers(final UUID validUserId) {
        setLoggedInUser(validUserId);
    }

    // When steps

    public static void whenCaseIsSubmittedForListing(final CaseData caseData) {
        final String listCaseForHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_SEND_CASE_FOR_LISTING)));

        final JsonObjectBuilder sendCaseForListingData = prepareJsonForListHearing(caseData);

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_SEND_CASE_FOR_LISTING,
                sendCaseForListingData.build().toString(), getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public static void whenCaseDefendantsChangedPublicEventIsPublished(final CaseData caseData) {
        final JsonObjectBuilder updateDefendantData = prepareJsonForUpdateDefendant(caseData);

        QueueUtil.sendMessage(
                QueueUtil.publicEvents.createProducer(),
                PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED,
                updateDefendantData.build(),
                metadataOf(randomUUID(), PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED).withUserId(randomUUID().toString()).build());
    }

    public static void whenCaseDefendantOffencesChangedPublicEventIsPublished(final CaseData caseData) {
        final JsonObjectBuilder changedOffences = prepareJsonForUpdateDefendantOffences(caseData);

        QueueUtil.sendMessage(
                QueueUtil.publicEvents.createProducer(),
                PUBLIC_EVENT_PROGRESSION_DEFENDANT_OFFENCES_CHANGED,
                changedOffences.build(),
                metadataOf(randomUUID(), PUBLIC_EVENT_PROGRESSION_DEFENDANT_OFFENCES_CHANGED).withUserId(randomUUID().toString()).build());
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

    public static void thenHearingConfirmedPublicEventShouldBePublished(
            final UUID hearingId,
            final MessageConsumerClient publicMessageConsumer) throws JMSException {

        verifyInPublicMQ(FIELD_HEARING_DOT_ID, hearingId.toString(), publicMessageConsumer);
    }

    public static void thenHearingUpdatedPublicEventShouldBePublished(
            final UUID hearingId,
            final MessageConsumerClient publicMessageConsumer) throws JMSException {

        verifyInPublicMQ(FIELD_HEARING_DOT_ID, hearingId.toString(), publicMessageConsumer);
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
                                                .getDefendants().get(0).getLastName())),
                                withJsonPath("$.hearings[0].urn",
                                        equalTo(caseData.getUrn())))
                        ));
    }

    public static void thenDefendantsShouldHaveChangedWhenQueried(final CaseData caseData) {
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
                                                .getDefendants().get(0).getChangedFirstName())),
                                withJsonPath("$.hearings[0].defendants[0].lastName",
                                        equalTo(caseData.getHearingData().get(0)
                                                .getDefendants().get(0).getChangedLastName())),
                                withJsonPath("$.hearings[0].urn",
                                        equalTo(caseData.getUrn())))
                        ));
    }

    public static void thenDefendantOffencesShouldHaveDeletedWhenQueried(final CaseData caseData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));


        final com.jayway.jsonpath.JsonPath deletedOffenceFilter = getJsonPathFilter(caseData, 1);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(deletedOffenceFilter),
                                withJsonPath("$.hearings[0].defendants[0].offences", hasSize(0)))
                        )
                );
    }

    public static void thenDefendantOffencesShouldHaveUpdatedWhenQueried(final CaseData caseData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));

        final com.jayway.jsonpath.JsonPath updatedOffenceFilter = getJsonPathFilter(caseData, 0);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(updatedOffenceFilter),
                                withJsonPath("$.hearings[0].defendants[0].offences", hasSize(2)))

                        )
                );
    }

    public static void thenDefendantOffencesShouldHaveAddedWhenQueried(final CaseData caseData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), caseData.getHearingData().get(0).getCourtCentreId(), UNALLOCATED));

        final com.jayway.jsonpath.JsonPath addedOffenceFilter = getJsonPathFilter(caseData, 2);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(addedOffenceFilter),
                                withJsonPath("$.hearings[0].defendants[0].offences", hasSize(4)))

                        )
                );
    }

    private static com.jayway.jsonpath.JsonPath getJsonPathFilter(CaseData caseData, int defendantIndex) {
        final Filter hearingFilter = filter(where("id").is(caseData.getHearingData().get(0).getId().toString()));
        final Filter defendantFilter = filter(where("defendantId").is(caseData.getHearingData().get(0).getDefendants().get(defendantIndex).getDefendantId().toString()));
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].defendants[?]", hearingFilter, defendantFilter);
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
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(updatedHearingData.getEndDate())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate())),
                                withJsonPath("$.hearings[0].nonSittingDays",
                                        equalTo(updatedHearingData.getNonSittingDays())),
                                withJsonPath("$.hearings[0].startTimes[0]",
                                        equalTo(updatedHearingData.getStartTimes().get(0)))
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
                                        equalTo(updatedHearingData.getStartDate())),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(updatedHearingData.getEndDate())),
                                withJsonPath("$.hearings[0].startTimes[0]",
                                        equalTo(updatedHearingData.getStartTimes().get(0))),
                                withJsonPath("$.hearings[0].nonSittingDays",
                                        equalTo(updatedHearingData.getNonSittingDays()))
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
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(updatedHearingData.getEndDate())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate())),
                                withJsonPath("$.hearings[0].startTimes[0]",
                                        equalTo(updatedHearingData.getStartTimes().get(0))),
                                withJsonPath("$.hearings[0].nonSittingDays",
                                        equalTo(updatedHearingData.getNonSittingDays())))
                        ));
    }

    public static void thenAllocatedHearingsForACourtCentreShouldContainAllocatedHearingData(final HearingData hearingData) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings"), hearingData.getCourtCentreId(), ALLOCATED));
        final Filter myFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", myFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingData.getId().toString())),
                                withJsonPath("$.hearings[0].judgeId",
                                        equalTo(hearingData.getJudgeId().toString())),
                                withJsonPath("$.hearings[0].courtRoomId",
                                        equalTo(hearingData.getCourtRoomId().toString())),
                                withJsonPath("$.hearings[0].type",
                                        equalTo(hearingData.getHearingType())),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(hearingData.getHearingEndDate().toString())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(hearingData.getHearingStartDate().toString())),
                                withJsonPath("$.hearings[0].estimateMinutes",
                                        equalTo(hearingData.getHearingEstimateMinutes()))
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

    private static JsonObjectBuilder prepareJsonForListHearing(final CaseData caseData) {
        final JsonObjectBuilder caseDataJson = createObjectBuilder();

        return caseDataJson.add(FIELD_CASE_ID, caseData.getCaseId().toString())
                .add(FIELD_URN, caseData.getUrn())
                .add(FIELD_HEARINGS, prepareJsonForHearings(caseData.getHearingData()));
    }

    private static JsonObjectBuilder prepareJsonForUpdateDefendant(final CaseData caseData) {
        final JsonObjectBuilder caseDataJson = createObjectBuilder();
        final List<DefendantData> defendants = caseData.getHearingData().get(0).getDefendants();
        final JsonArrayBuilder defendantsJson = prepareJsonForProgressionDefendants(defendants);
        return caseDataJson
                .add(FIELD_CASE_ID, caseData.getCaseId().toString())
                .add(FIELD_DEFENDANTS, defendantsJson);
    }

    private static JsonObjectBuilder prepareJsonForUpdateDefendantOffences(final CaseData caseData) {
        final JsonObjectBuilder caseDataJson = createObjectBuilder();
        final List<DefendantData> defendants = caseData.getHearingData().get(0).getDefendants();
        final JsonArrayBuilder updatedOffencesJson = prepareJsonForProgressionUpdatedOffences(caseData, defendants);
        final JsonArrayBuilder deletedOffencesJson = prepareJsonForProgressionDeletedOffences(caseData, defendants);
        final JsonArrayBuilder addedOffencesJson = prepareJsonForProgressionAddedOffences(caseData, defendants);
        return caseDataJson
                .add(FIELD_MODIFIED_DATE, LocalDate.now().toString())
                .add(FIELD_UPDATED_OFFENCES, updatedOffencesJson)
                .add(FIELD_DELETED_OFFENCES, deletedOffencesJson)
                .add(FIELD_ADDED_OFFENCES, addedOffencesJson);
    }

    private static JsonObjectBuilder prepareJsonForRandomAddress() {
        final JsonObjectBuilder addressJson = createObjectBuilder();
        return addressJson
                .add(FIELD_ADDRESS_LINE_1, STRING.next())
                .add(FIELD_ADDRESS_LINE_2, STRING.next())
                .add(FIELD_ADDRESS_LINE_3, STRING.next())
                .add(FIELD_ADDRESS_LINE_4, STRING.next())
                .add(FIELD_ADDRESS_POSTCODE, STRING.next());
    }

    private static JsonObjectBuilder prepareJsonForUpdatedHearingData(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, updatedHearingData.getType())
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_END_DATE, updatedHearingData.getEndDate())
                .add(FIELD_START_TIMES, prepareJsonStringArray(updatedHearingData.getStartTimes()))
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()));

        addNullableField(builder, FIELD_JUDGE_ID, getStringOrNull(updatedHearingData.getJudgeId()));
        addNullableField(builder, FIELD_COURT_ROOM_ID, getStringOrNull(updatedHearingData.getCourtRoomId()));

        return builder;
    }

    private static JsonArray prepareJsonStringArray(List<String> strings) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        if(strings!=null && !strings.isEmpty()) {
            strings.forEach(builder::add);
        }
        return builder.build();

    }

    private static void addNullableField(JsonObjectBuilder builder, String fieldName, String value) {
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
                            JsonObjectBuilder hearingBuilder = createObjectBuilder()
                                    .add(FIELD_GENERIC_ID, hearing.getId().toString())
                                    .add(FIELD_COURT_CENTRE_ID, hearing.getCourtCentreId())
                                    .add(FIELD_HEARING_TYPE, hearing.getHearingType())
                                    .add(FIELD_HEARING_START_DATE, hearing.getHearingStartDate().toString())
                                    .add(FIELD_HEARING_ESTIMATE_MINUTES, hearing.getHearingEstimateMinutes())
                                    .add(FIELD_DEFENDANTS, prepareJsonForDefendants(hearing.getDefendants()));
                            if (hearing.getJudgeId() != null) {
                                hearingBuilder.add(FIELD_JUDGE_ID, hearing.getJudgeId().toString());
                            }
                            if (hearing.getCourtRoomId() != null) {
                                hearingBuilder.add(FIELD_COURT_ROOM_ID, hearing.getCourtRoomId().toString());
                            }
                            if(hearing.getHearingEndDate() != null){
                                hearingBuilder.add(FIELD_HEARING_END_DATE, hearing.getHearingEndDate().toString());
                            }
                            return hearingBuilder;
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
//
    private static JsonArrayBuilder prepareJsonForUpdatedProgressionOffences(final List<OffenceData> offences) {
        return offences.stream()
                .map(offenceData -> {

                            JsonObjectBuilder statementOfOffenceBuilder = createObjectBuilder()
                                    .add(FIELD_TITLE, offenceData.getChangedStatementOfOffenceTitle())
                                    .add(FIELD_LEGISLATION, offenceData.getChangedStatementOfOffenceLegislation());

                            return createObjectBuilder()
                                    .add(FIELD_GENERIC_ID, offenceData.getOffenceId().toString())
                                    .add(FIELD_OFFENCE_CODE, offenceData.getChangedOffenceCode())
                                    .add(FIELD_START_DATE, offenceData.getStartDate().toString())
                                    .add(FIELD_END_DATE, offenceData.getEndDate().toString())
                                    .add(FIELD_WORDING, STRING.next())
                                    .add(FIELD_COUNT, 1)
                                    .add(FIELD_CONVICTION_DATE, LocalDate.now().toString())
                                    .add(FIELD_STATEMENT_OF_OFFENCE, statementOfOffenceBuilder);
                        }
                )
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static JsonArrayBuilder prepareJsonForProgressionOffencesDeleted(final List<OffenceData> offences) {
        return offences.stream()
                .map(offenceData -> offenceData.getOffenceId().toString())
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }
//
    private static JsonArrayBuilder prepareJsonForAddedProgressionOffences(final List<OffenceData> offences) {
        return offences.stream()
                .map(offenceData -> {

                            JsonObjectBuilder statementOfOffenceBuilder = createObjectBuilder()
                                    .add(FIELD_TITLE, offenceData.getStatementOfOffenceTitle())
                                    .add(FIELD_LEGISLATION, offenceData.getStatementOfOffenceLegislation());

                            return createObjectBuilder()
                                    .add(FIELD_GENERIC_ID, offenceData.getRandomOffenceId().toString())
                                    .add(FIELD_OFFENCE_CODE, offenceData.getNewOffenceCode())
                                    .add(FIELD_START_DATE, offenceData.getStartDate().toString())
                                    .add(FIELD_END_DATE, offenceData.getEndDate().toString())
                                    .add(FIELD_WORDING, STRING.next())
                                    .add(FIELD_COUNT, 1)
                                    .add(FIELD_CONVICTION_DATE, LocalDate.now().toString())
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

    private static JsonArrayBuilder prepareJsonForProgressionDefendants(final List<DefendantData> defendants) {
        return defendants.stream()
                .map(defendantData -> createObjectBuilder()
                        .add(FIELD_GENERIC_ID, defendantData.getDefendantId().toString())
                        .add(FIELD_PERSON, prepareJsonForProgressionDefendant(defendantData))
                        .add(FIELD_CUSTODY_TIME_LIMIT_Date, defendantData.getCustodyTimeLimit().toString())
                        .add(FIELD_BAIL_STATUS, defendantData.getBailStatus())
                        .add(FIELD_DEFENCE_ORGANISATION, defendantData.getDefenceOrganisation())
                        .add(FIELD_INTERPRETER, prepareJsonForRandomInterpreter())
                )
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static JsonArrayBuilder prepareJsonForProgressionUpdatedOffences(final CaseData caseData, final List<DefendantData> defendants) {
        DefendantData defendantData = defendants.get(0);
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add(FIELD_DEFENDANT_ID, defendantData.getDefendantId().toString())
                        .add(FIELD_CASE_ID, caseData.getCaseId().toString())
                        .add(FIELD_OFFENCES, prepareJsonForUpdatedProgressionOffences(defendantData.getOffences()))
                );
    }

    private static JsonArrayBuilder prepareJsonForProgressionDeletedOffences(final CaseData caseData, final List<DefendantData> defendants) {
        DefendantData defendantData = defendants.get(1);
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add(FIELD_DEFENDANT_ID, defendantData.getDefendantId().toString())
                        .add(FIELD_CASE_ID, caseData.getCaseId().toString())
                        .add(FIELD_OFFENCES, prepareJsonForProgressionOffencesDeleted(defendantData.getOffences()))
                );
    }

    private static JsonArrayBuilder prepareJsonForProgressionAddedOffences(final CaseData caseData, final List<DefendantData> defendants) {
        DefendantData defendantData = defendants.get(2);
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add(FIELD_DEFENDANT_ID, defendantData.getDefendantId().toString())
                        .add(FIELD_CASE_ID, caseData.getCaseId().toString())
                        .add(FIELD_OFFENCES, prepareJsonForAddedProgressionOffences(defendantData.getOffences()))
                );
    }

    private static JsonObjectBuilder prepareJsonForProgressionDefendant(DefendantData defendantData) {
        final JsonObjectBuilder defendantJson = createObjectBuilder();
        return defendantJson
                .add(FIELD_GENERIC_ID, defendantData.getPersonId().toString())
                .add(FIELD_FIRST_NAME, defendantData.getChangedFirstName())
                .add(FIELD_LAST_NAME, defendantData.getChangedLastName())
                .add(FIELD_DATE_BIRTH, defendantData.getDateOfBirth().toString())
                .add(FIELD_NATIONALITY, defendantData.getFirstName()) // TODO: change to nationality
                .add(FIELD_TITLE, STRING.next())
                .add(FIELD_GENDER, STRING.next())
                .add(FIELD_HOME_TELE, STRING.next())
                .add(FIELD_WORK_TELE, STRING.next())
                .add(FIELD_MOBILE, STRING.next())
                .add(FIELD_FAX, STRING.next())
                .add(FIELD_EMAIL, STRING.next())
                .add(FIELD_ADDRESS, prepareJsonForRandomAddress());
    }

    private static JsonObjectBuilder prepareJsonForRandomInterpreter() {
        final JsonObjectBuilder defendantJson = createObjectBuilder();
        return defendantJson
                .add(FIELD_INTERPRETER_NEEDED, INTERPRETER_NEEDED)
                .add(FIELD_INTERPRETER_NAME, STRING.next())
                .add(FIELD_LANGUAGE, STRING.next());
    }

    private static void verifyInPublicMQ(final String key, final String expectedValue, final MessageConsumerClient
            publicMessageConsumer) throws JMSException {
        JsonPath response = new JsonPath(publicMessageConsumer.retrieveMessage().get());
        assertThat(response.get(key), CoreMatchers.equalTo(expectedValue));
    }

}
