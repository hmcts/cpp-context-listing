package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_DURATION_HOURS_MINS;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.MEDIA_TYPE_SEARCH_HEARINGS_JSON;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING;
import static uk.gov.moj.cpp.listing.utils.JsonObjectBuilderHelper.prepareJsonForUpdatedHearingData;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeekCommencingHearingSteps extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeekCommencingHearingSteps.class);

    private static final String EVENT_SELECTOR_START_DATE_REMOVED = "listing.events.start-date-removed-for-hearing";
    private static final String EVENT_SELECTOR_WEEK_COMMENCING_DATES_CHANGED = "listing.events.week-commencing-date-changed-for-hearing";
    private static final String EVENT_SELECTOR_END_DATE_REMOVED = "listing.events.end-date-removed-from-hearing";

    private final UpdatedHearingData updatedHearingData;

    private String request;

    private JmsMessageConsumerClient privateMessageConsumerEndDateRemoved;
    private JmsMessageConsumerClient privateMessageConsumerStartDateRemoved;

    private JmsMessageConsumerClient privateMessageConsumerWeekCommencingDatesChanged;

    public WeekCommencingHearingSteps(final UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        createMessageConsumers();
    }

    private void createMessageConsumers() {
        privateMessageConsumerStartDateRemoved = privateEvents.createPrivateConsumer(EVENT_SELECTOR_START_DATE_REMOVED);
        privateMessageConsumerEndDateRemoved = privateEvents.createPrivateConsumer(EVENT_SELECTOR_END_DATE_REMOVED);
        privateMessageConsumerWeekCommencingDatesChanged = privateEvents.createPrivateConsumer(EVENT_SELECTOR_WEEK_COMMENCING_DATES_CHANGED);

    }

    public void whenHearingIsUpdatedForListingForWeekCommencingDate() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), updatedHearingData.getName()));

        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingData(updatedHearingData);

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    private void verifyWeekCommencingDateChangedEvent() {
        JsonPath jsonResponse = retrieveMessage(privateMessageConsumerWeekCommencingDatesChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerWeekCommencingDatesChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("weekCommencingStartDate"), is(updatedHearingData.getWeekCommencingStartDate()));
        assertThat(jsonResponse.get("weekCommencingEndDate"), is(updatedHearingData.getWeekCommencingEndDate()));
    }

    public void verifyHearingUpdatedResultsForWeekCommencingInMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyWeekCommencingDateChangedEvent();
        verifyStartDateRemovedEvent();
        verifyEndDateRemovedEvent();
    }

    private void verifyStartDateRemovedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerStartDateRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerStartDateRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    private void verifyEndDateRemovedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerEndDateRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    public void verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings.by.week.commencing"), updatedHearingData.getWeekCommencingStartDate(), updatedHearingData.getWeekCommencingEndDate(), updatedHearingData.getCourtCentreId(), FALSE));


        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(updatedHearingData.getJurisdictionType())),
                                withJsonPath("$.hearings[0].courtCentreId",
                                        equalTo(updatedHearingData.getCourtCentreId().toString())),
                                withJsonPath("$.hearings[0].type.id",
                                        equalTo(updatedHearingData.getHearingTypData().getTypeId().toString())),
                                hasNoJsonPath("$.hearings[0].startDate"),
                                hasNoJsonPath("$.hearings[0].endDate"),
                                withJsonPath("$.hearings[0].weekCommencingStartDate",
                                        equalTo(updatedHearingData.getWeekCommencingStartDate())),
                                withJsonPath("$.hearings[0].weekCommencingEndDate",
                                        equalTo(updatedHearingData.getWeekCommencingEndDate())),
                                withJsonPath("$.hearings[0].weekCommencingDurationInWeeks",
                                        equalTo(updatedHearingData.getWeekCommencingDurationInWeeks().toString()))

                        )));
    }
}
