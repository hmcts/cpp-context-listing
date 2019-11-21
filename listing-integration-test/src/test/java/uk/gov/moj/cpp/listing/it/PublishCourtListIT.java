package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;

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

        UUID courtCentreId = randomUUID();
        final JsonObject publishCourtListJsonObject = buildPublishCourtListJsonString(courtCentreId.toString());

        loadHearingDataWithJudiciary(courtCentreId);

        try (final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(publishCourtListJsonObject)) {
            sendPublishCourtListCommand(publishCourtListJsonObject);
            publishCourtListSteps.verifyPublishCourtListEventsInActiveMQ();
            publishCourtListSteps.verifyCourtListPublishStatusReturnedWhenQueryingFromAPI("COURT_LIST_PRODUCED");
        }

    }

    private void sendPublishCourtListCommand(final JsonObject publishCourtListJsonObject) {

        final String updateHearingUrl = String.format("%s/%s", baseUri, format(ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_PUBLISH_COURT_LIST),
                publishCourtListJsonObject.getString("courtCentreId")));
        final String request = publishCourtListJsonObject.toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    private JsonObject buildPublishCourtListJsonString(final String courtCentreId) {
        return createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("startDate", now().toString())
                .add("endDate", now().plusDays(2).toString())
                .add("publishCourtListType", "FIRM")
                .add("requestedTime", "2019-10-30T16:34:45.132Z")
                .build();
    }

}
