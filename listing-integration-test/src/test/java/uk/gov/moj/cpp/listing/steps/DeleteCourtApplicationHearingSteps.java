package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import javax.json.Json;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteCourtApplicationHearingSteps extends AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCourtApplicationHearingSteps.class);

    private final JmsMessageProducerClient publicCourtApplicationHearingDeleted;
    private final JmsMessageConsumerClient privateMessageConsumerCourtApplicationHearingDeleted;

    public DeleteCourtApplicationHearingSteps() {
        publicCourtApplicationHearingDeleted = publicEvents.createPublicProducer();
        privateMessageConsumerCourtApplicationHearingDeleted = QueueUtil.privateEvents.createPrivateConsumer("listing.events.court-application-hearing-deleted");
    }

    public void whenRaisedCourtApplicationHearingPublicEvent(final String hearingId, final String applicationId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId)
                .add("applicationId", applicationId)
                .build();
        QueueUtil.sendMessage(
                publicCourtApplicationHearingDeleted,
                "public.progression.events.court-application-deleted",
                payload,
                metadataOf(randomUUID(), "public.progression.events.court-application-deleted").withUserId(randomUUID().toString()).build());
    }

    public void verifyCourtApplicationHearingDeletedPrivateEvent(final String hearingId) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtApplicationHearingDeleted);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("hearingId"), is(hearingId));
    }

    public void verifyOldHearingDeleted(final String hearingId, final String courtCentreId) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"),
                        courtCentreId, false));

        poll(requestParams(searchHearingUrl, "application/vnd.listing.search.hearings+json").withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(not(withJsonPath("$.hearings[0].id", equalTo(hearingId)))));
    }
}
