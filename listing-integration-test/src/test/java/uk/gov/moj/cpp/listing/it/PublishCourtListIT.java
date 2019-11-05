package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.listing.steps.PublishCourtListSteps;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishCourtListIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCourtListIT.class);

    private static final String LISTING_COMMAND_PUBLISH_COURT_LIST = "listing.command.publish-court-list";
    private static final String MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST = "application/vnd.listing.command.publish-court-list+json";


    @Test
    public void shouldRequestToPublishCourtList() {
        final JsonObject publishCourtListJsonObject = buildPublishCourtListJsonString();

        try (final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(publishCourtListJsonObject)) {
            sendPublishCourtListCommand(publishCourtListJsonObject);
            publishCourtListSteps.verifyPublishCourtListEventsInActiveMQ();
            publishCourtListSteps.verifyPublishCourtListEventsInViewStore();
        }

    }


    public void sendPublishCourtListCommand(final JsonObject publishCourtListJsonObject) {

        final String updateHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_PUBLISH_COURT_LIST), publishCourtListJsonObject.getString("courtCentreId")));

        final String request = publishCourtListJsonObject.toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    private JsonObject buildPublishCourtListJsonString() {
        return createObjectBuilder()
                .add("courtCentreId", randomUUID().toString())
                .add("startDate", "2018-12-06")
                .add("endDate", "2018-12-20")
                .add("courtListType", "FIRM")
                .add("requestedTime", "2019-10-30T16:34:45.132Z")
                .build();
    }
}
