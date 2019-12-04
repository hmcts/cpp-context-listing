package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishCourtListSteps extends CommonHearingSteps implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCourtListSteps.class);

    private static final String PUBLISH_COURT_LIST_PRODUCED = "listing.event.publish-court-list-produced";
    private static final String PUBLISH_COURT_LIST_REQUESTED = "listing.event.publish-court-list-requested";

    private static final String MEDIA_TYPE_QUERY_COURT_LIST_STATUS = "application/vnd.listing.court.list.publish.status+json";

    private MessageConsumer privateMessageConsumerPublishCourtListRequested;
    private MessageConsumer privateMessageConsumerPublishCourtListProduced;

    private JsonObject commandJsonObject;
    private static final String TOPIC_PRIVATE_EVENT = "listing.event";

    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();

    public PublishCourtListSteps(final HearingsData hearingsData, final JsonObject commandJsonObject) {
        super(hearingsData);
        createMessageConsumers();
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        this.commandJsonObject = commandJsonObject;
        privateEventsProducer.startProducer(TOPIC_PRIVATE_EVENT);

    }

    public void verifyPublishCourtListEventsInActiveMQ() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerPublishCourtListRequested);
        assertThat(jsonResponse.getString("courtCentreId"), is(commandJsonObject.getString("courtCentreId")));
        assertThat(jsonResponse.getString("publishCourtListType"), is(commandJsonObject.getString("publishCourtListType")));
        assertThat(jsonResponse.getString("publishStatus"), is("COURT_LIST_REQUESTED"));
        assertThat(jsonResponse.getString("endDate"), is(commandJsonObject.getString("endDate")));
        assertThat(jsonResponse.getString("startDate"), is(commandJsonObject.getString("startDate")));
    }

    private void createMessageConsumers() {
        privateMessageConsumerPublishCourtListRequested = privateEvents.createConsumer(PUBLISH_COURT_LIST_REQUESTED);
        privateMessageConsumerPublishCourtListProduced = privateEvents.createConsumer(PUBLISH_COURT_LIST_PRODUCED);
    }


    @Override
    public void close() {
        try {
            privateMessageConsumerPublishCourtListRequested.close();
            privateMessageConsumerPublishCourtListRequested.close();
            privateEventsProducer.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }
    }

    public void verifyCourtListPublishStatusReturnedWhenQueryingFromAPI(final String expectedPublishStatus) {
        final String courtCentreId = commandJsonObject.getString("courtCentreId");
        final String courtListType = commandJsonObject.getString("publishCourtListType");
        final String publishDate = LocalDate.now().toString();
        final String weekCommencing = "true";
        final String queryPart = format(ENDPOINT_PROPERTIES.getProperty("listing.court.list.publish.status"),
                courtCentreId,
                courtListType,
                publishDate,
                weekCommencing);
        final String searchCourtListUrl = String.format("%s/%s", baseUri, queryPart);

        poll(requestParams(searchCourtListUrl, MEDIA_TYPE_QUERY_COURT_LIST_STATUS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.publishCourtListStatuses[0].courtCentreId",
                                        equalTo(courtCentreId)),
                                withJsonPath("$.publishCourtListStatuses[0].publishCourtListType",
                                        equalTo(courtListType)),
                                withJsonPath("$.publishCourtListStatuses[0].lastUpdated",
                                        is(notNullValue())),
                                withJsonPath("$.publishCourtListStatuses[0].publishStatus",
                                        equalTo(expectedPublishStatus)),
                                withJsonPath("$.publishCourtListStatuses[0].failureMessage",
                                        equalTo(""))
                        )));
    }


    public static HearingsData loadHearingDataWithJudiciary(final UUID courtCentreId) {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(courtCentreId, "DISTRICT_JUDGE");

        createHearingListed(hearingsData);

        return hearingsData;
    }

    private static void createHearingListed(final HearingsData hearingsData) {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }
    }


}
