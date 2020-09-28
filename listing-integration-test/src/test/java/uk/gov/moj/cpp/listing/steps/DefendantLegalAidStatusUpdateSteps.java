package uk.gov.moj.cpp.listing.steps;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;

public class DefendantLegalAidStatusUpdateSteps extends AbstractIT implements AutoCloseable  {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantLegalAidStatusUpdateSteps.class);


    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";

    private static final String EVENT_SELECTOR_DEFENDANT_LEGALAID_STATUS_UPDATED_FOR_HEAIRNG = "listing.events.defendant-legalaid-status-updated-for-hearing";


    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";


    private MessageProducer publicEventDefendantLegalAidStatusUpdated;
    private MessageConsumer publicEventMessageConsumerDefendantUpdated;
    private MessageConsumer privateEventsMessageDefendantLegalAidStatusUpdated;


    private String request;


    private final HearingData hearingData;
    private final ListedCaseData listedCaseData;
    private final UUID caseId;

    public DefendantLegalAidStatusUpdateSteps(UUID caseId, HearingData hearingData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);


        publicEventDefendantLegalAidStatusUpdated = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerDefendantUpdated = QueueUtil.publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED);
        privateEventsMessageDefendantLegalAidStatusUpdated = privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANT_LEGALAID_STATUS_UPDATED_FOR_HEAIRNG);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseDefendantLegalAidStatusUpdatedPublicEventIsPublished() {

        final JsonObject payload = getPayloadForPublicEventFromHearingData();
        QueueUtil.sendMessage(
                publicEventDefendantLegalAidStatusUpdated,
                PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED,
                payload,
                metadataOf(randomUUID(), PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).withUserId(randomUUID().toString()).build());

        request = payload.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED, request, getLoggedInHeader());
    }

    public void verifyEventDefendantLegalAidStatusUpdatedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventsMessageDefendantLegalAidStatusUpdated);
        LOGGER.debug("jsonResponse from privateEventsMessageDefendantLegalAidStatusUpdated: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("defendantId"), is(jsRequest.getString("defendantId")));
        assertThat(jsonResponse.get("caseId"), is(jsRequest.getString("caseId")));
        assertThat(jsonResponse.get("legalAidStatus"), is(jsRequest.getString("legalAidStatus")));
        assertThat(jsonResponse.get("hearingId"), is(hearingData.getId().toString()));

    }


    public void verifyHearingListedFromAPI(boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));
        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].legalAidStatus",
                                        equalTo("Granted"))
                        )));
    }



    @Override
    public void close() throws Exception {
        try {
            publicEventDefendantLegalAidStatusUpdated.close();
            privateEventsMessageDefendantLegalAidStatusUpdated.close();
            publicEventMessageConsumerDefendantUpdated.close();
        } catch (JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private JsonObject getPayloadForPublicEventFromHearingData() {
        return Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", listedCaseData.getDefendants().get(0).getDefendantId().toString())
                .add("legalAidStatus", "Granted")
                .build();

    }
}
