package uk.gov.moj.cpp.listing.steps;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import uk.gov.moj.cpp.listing.it.AbstractIT;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishCourtListSteps extends AbstractIT implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCourtListSteps.class);


    private static final String PUBLISH_COURT_LIST_REQUESTED = "listing.event.publish-court-list-requested";

    private MessageConsumer privateMessageConsumerPublishCourtListRequested;
    private JsonObject commandJsonObject;

    public PublishCourtListSteps(final JsonObject commandJsonObject) {
        createMessageConsumers();
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        this.commandJsonObject = commandJsonObject;
    }


    public void verifyPublishCourtListEventsInActiveMQ() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerPublishCourtListRequested);
        assertThat(jsonResponse.getString("courtCentreId"), is(commandJsonObject.getString("courtCentreId")));
        assertThat(jsonResponse.getString("courtListType"), is(commandJsonObject.getString("courtListType")));
        assertThat(jsonResponse.getString("endDate"), is(commandJsonObject.getString("endDate")));
        assertThat(jsonResponse.getString("startDate"), is(commandJsonObject.getString("startDate")));
    }

    public void verifyPublishCourtListEventsInViewStore() {

    }


    private void createMessageConsumers() {
        privateMessageConsumerPublishCourtListRequested = privateEvents.createConsumer(PUBLISH_COURT_LIST_REQUESTED);
    }


    @Override
    public void close() {
        try {
            privateMessageConsumerPublishCourtListRequested.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }
    }
}
