package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.ApplicationEjectedData;
import uk.gov.moj.cpp.listing.steps.data.CaseEjectedData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.Arrays;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EjectCaseApplicationSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EjectCaseApplicationSteps.class);


    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";

    private static final String LISTING_COMMAND_EJECT_CASE_OR_APPLICATION = "listing.command.eject-case-or-application";
    private static final String EVENT_SELECTOR_CASE_EJECTED = "listing.events.case-ejected";
    private static final String EVENT_SELECTOR_APPLICATION_EJECTED = "listing.events.application-ejected";

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";


    private MessageProducer publicEventEjectCaseOrApplication;
    private MessageConsumer publicEventMessageConsumerCaseApplication;
    private MessageConsumer privateEventsMessageCaseEjected;
    private MessageConsumer privateEventsMessageApplicationEjected;


    private String request;


    private final HearingsData hearingsData;
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public EjectCaseApplicationSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        publicEventEjectCaseOrApplication = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerCaseApplication = QueueUtil.publicEvents.createConsumer(PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED);
        privateEventsMessageCaseEjected = privateEvents.createConsumer(EVENT_SELECTOR_CASE_EJECTED);
        privateEventsMessageApplicationEjected = privateEvents.createConsumer(EVENT_SELECTOR_APPLICATION_EJECTED);
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }

    public void buildEjectCaseData() {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingsData.getHearingData().get(0).getListedCases().get(0);

        CaseEjectedData ejectCaseApplicationData = CaseEjectedData.caseEjected()
                .withHearingIds(Arrays.asList(hearingData.getId()))
                .withProsecutionCaseId(listedCaseData.getCaseId())
                .withRemovalReason("SomeReason")
                .build();

        whenEjectCaseOrApplicationPublicEventIsPublished(ejectCaseApplicationData);
    }

    public void buildEjectApplicationData() {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        ApplicationEjectedData ejectCaseApplicationData = ApplicationEjectedData.applicationEjected()
                .withHearingIds(Arrays.asList(hearingData.getId()))
                .withApplicationId(courtApplicationData.getId())
                .withRemovalReason("SomeReason")
                .build();
        whenEjectCaseOrApplicationPublicEventIsPublished(ejectCaseApplicationData);
    }

    public void whenEjectCaseOrApplicationPublicEventIsPublished(Object ejectCaseApplicationData) {

        final JsonObject ejectCaseDataObject = (JsonObject) objectToJsonValueConverter.convert(ejectCaseApplicationData);

        QueueUtil.sendMessage(
                publicEventEjectCaseOrApplication,
                PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED,
                ejectCaseDataObject,
                metadataOf(randomUUID(), PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED).withUserId(randomUUID().toString()).build());

        request = ejectCaseDataObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED, request, getLoggedInHeader());
    }

    public void verifyEventCaseEjectedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventsMessageCaseEjected);
        LOGGER.debug("jsonResponse from privateEventsMessageCaseEjected: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("hearingIds"), is(jsRequest.getString("hearingIds")));
        assertThat(jsonResponse.getString("prosecutionCaseId"), is(jsRequest.getString("prosecutionCaseId")));
    }

    public void verifyEventApplicationEjectedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventsMessageApplicationEjected);
        LOGGER.debug("jsonResponse from privateEventsMessageApplicationEjected: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("hearingIds"), is(jsRequest.getString("hearingIds")));
        assertThat(jsonResponse.getString("applicationId"), is(jsRequest.getString("applicationId")));
    }


    public void verifyListedCasesInHearings(boolean isAllocated, int numberOfListedCases) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

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
                                withJsonPath("$.hearings[0].listedCases", hasSize(numberOfListedCases))
                        )));
    }

    public void verifyCourtApplicationInHearings(boolean isAllocated, int numberOfCourtApplications) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

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
                                withJsonPath("$.hearings[0].courtApplications", hasSize(numberOfCourtApplications))
                        )));
    }
    public void verifyNoHearingsReturned(boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        final RequestParamsBuilder requestParamsBuilder = requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser());

        poll(requestParamsBuilder)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings", hasSize(0))
                        )));
    }

    @Override
    public void close() {
        try {
            publicEventEjectCaseOrApplication.close();
            publicEventMessageConsumerCaseApplication.close();
            //privateEventMessageDefendantsToBeAdded.close();
            privateEventsMessageCaseEjected.close();
        } catch (JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}



