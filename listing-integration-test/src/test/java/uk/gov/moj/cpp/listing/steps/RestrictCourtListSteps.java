package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.RestrictCourtListData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.Arrays;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RestrictCourtListSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictCourtListSteps.class);

    private static final String LISTING_COMMAND_RESTRICT_COURT_LIST = "listing.command.restrict-court-list";
    private static final String PUBLIC_EVENT_RESTRICT_COURT_LIST = "public.listing.court-list-restricted";
    private static final String PRIVATE_EVENT_RESTRICT_COURT_LIST = "listing.events.court-list-restricted";
    private static final String MEDIA_TYPE_RESTRICT_COURT_LIST = "application/vnd.listing.command.restrict-court-list+json";


    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";

    private MessageProducer publicEventListingRestrictedFromCourt;
    private MessageConsumer publicEventMessageConsumerRestrictCourtList;
    private MessageConsumer privateMessageConsumerRestrictCourtList;

    private String request;

    private final HearingsData hearingsData;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public RestrictCourtListSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        publicEventListingRestrictedFromCourt = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerRestrictCourtList = QueueUtil.publicEvents.createConsumer(PUBLIC_EVENT_RESTRICT_COURT_LIST);
        privateMessageConsumerRestrictCourtList = QueueUtil.privateEvents.createConsumer(PRIVATE_EVENT_RESTRICT_COURT_LIST);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }
    public void whenRestrictingCaseOrStandaloneApplicationForCourtListing(RestrictCourtListData restrictListingFromCourtData) {
        final JsonObject restrictCourtListDataObject = (JsonObject) objectToJsonValueConverter.convert(restrictListingFromCourtData);
        final String hearingRestrictUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_RESTRICT_COURT_LIST, hearingsData.getHearingData().get(0).getId().toString())));

        request = restrictCourtListDataObject.toString();
        final Response response = restClient.postCommand(hearingRestrictUrl, MEDIA_TYPE_RESTRICT_COURT_LIST,
                request, getLoggedInHeader());
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", LISTING_COMMAND_RESTRICT_COURT_LIST, request, getLoggedInHeader());
        Assert.assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    public void verifyRestrictCourtListInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerRestrictCourtList);
        LOGGER.debug("jsonResponse from publicEventMessageConsumerRestrictCourtList: {}", jsonResponse.prettify());
        assertThat(jsonResponse.getString("hearingId"), is(jsRequest.getString("hearingId")));
        assertThat(jsonResponse.getString("defendantIds"), is(jsRequest.getString("defendantIds")));
        assertThat(jsonResponse.getString("caseIds"), is(jsRequest.getString("caseIds")));
        assertThat(jsonResponse.getString("offenceIds"), is(jsRequest.getString("offenceIds")));
    }


    public void verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(Boolean restrictCourtListingOfCase, Boolean restrictCourtListingOfDefendant, Boolean restrictCourtListingOfOffence ) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), false));

        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        final RequestParamsBuilder requestParamsBuilder = requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser());
        poll(requestParamsBuilder)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].restrictFromCourtList",
                                        equalTo(restrictCourtListingOfCase)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList",
                                        equalTo(restrictCourtListingOfDefendant)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].restrictFromCourtList",
                                        equalTo(restrictCourtListingOfOffence))
                        )));
    }

    public void verifyListingRestrictedInHearing(Boolean restrictCourtListingOfCase, Boolean restrictCourtListingOfDefendant, Boolean restrictCourtListingOfOffence ) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), true));

        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        final RequestParamsBuilder requestParamsBuilder = requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser());
        poll(requestParamsBuilder)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].restrictFromCourtList",
                                        equalTo(restrictCourtListingOfCase)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList",
                                        equalTo(restrictCourtListingOfDefendant)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].restrictFromCourtList",
                                        equalTo(restrictCourtListingOfOffence)),
                               hasNoJsonPath("hearing.listedCases[0].defendants[0].offences[0].reportingRestrictions[0]")

                        )));
    }

    public void verifyCourtApplicationorApplicantorRespondentListingRestrictedInHearing(Boolean restrictCourtListingOfCourtApplication, Boolean restrictCourtListingOfApplicant, Boolean restrictCourtListingOfRespondent, Boolean restrictCourtListingOfCourtApplicationType ) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), false));

        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        final RequestParamsBuilder requestParamsBuilder = requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser());
        poll(requestParamsBuilder)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.id",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getApplicant().getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRespondent().getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].restrictFromCourtList",
                                        equalTo(restrictCourtListingOfCourtApplication)),
                               withJsonPath("$.hearings[0].courtApplications[0].applicant.restrictFromCourtList",
                                        equalTo(restrictCourtListingOfApplicant)),
                               withJsonPath("$.hearings[0].courtApplications[0].respondents[0].restrictFromCourtList",
                                       equalTo(restrictCourtListingOfRespondent)),
                               withJsonPath("$.hearings[0].courtApplications[0].restrictCourtApplicationType",
                                        equalTo(restrictCourtListingOfCourtApplicationType))
                        )));
    }

    public RestrictCourtListData getRestrictListingFromCourtData(HearingsData hearingsData) {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingsData.getHearingData().get(0).getListedCases().get(0);

        return RestrictCourtListData.restrictCourtList()
                .withCaseIds(Arrays.asList(listedCaseData.getCaseId()))
                .withDefendantIds(Arrays.asList(listedCaseData.getDefendants().get(0).getDefendantId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getDefendantsAndOffencesDataToBeUnrestricted(HearingsData hearingsData){

        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingsData.getHearingData().get(0).getListedCases().get(0);

        return RestrictCourtListData.restrictCourtList()
                .withOffenceIds(Arrays.asList(listedCaseData.getDefendants().get(0).getOffences().get(0).getOffenceId()))
                .withDefendantIds(Arrays.asList(listedCaseData.getDefendants().get(0).getDefendantId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(false)
                .build();
    }


    public RestrictCourtListData getDefendantsAndOffencesDataToBeRestricted(HearingsData hearingsData){

        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingsData.getHearingData().get(0).getListedCases().get(0);

        return RestrictCourtListData.restrictCourtList()
                .withOffenceIds(Arrays.asList(listedCaseData.getDefendants().get(0).getOffences().get(0).getOffenceId()))
                .withDefendantIds(Arrays.asList(listedCaseData.getDefendants().get(0).getDefendantId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getCourtApplicationDataToBeRestricted(HearingsData hearingsData){

        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicatonIds(Arrays.asList(courtApplicationData.getId()))
                .withCourtApplicationApplicantIds(Arrays.asList(courtApplicationData.getApplicant().getId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getCourtApplicationTypeToBeRestricted(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicatonIds(Arrays.asList(courtApplicationData.getId()))
                .withCourtApplicationType(java.util.Optional.of((courtApplicationData.getType())))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    @Override
    public void close() {
        try {
            publicEventListingRestrictedFromCourt.close();
            publicEventMessageConsumerRestrictCourtList.close();
            privateMessageConsumerRestrictCourtList.close();

        } catch (JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}

