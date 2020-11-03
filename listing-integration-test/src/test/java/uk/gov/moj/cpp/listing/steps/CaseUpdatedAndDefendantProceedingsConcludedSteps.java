package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.io.IOException;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseUpdatedAndDefendantProceedingsConcludedSteps extends AbstractIT implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseUpdatedAndDefendantProceedingsConcludedSteps.class);
    private static final String PUBLIC_EVENT_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";
    private static final String LISTING_EVENTS_CASE_RESULTED_AND_DEFENDANT_PROCEEDINGS_CONCLUDED = "listing.events.case-resulted-defendant-proceedings-updated";
    private static final String LISTING_EVENT_DEFENDANT_COURT_PROCEEDINGS_UPDATED = "listing.events.defendant-court-proceedings-updated";
    private static final String MEDIA_TYPE_HEARING_RESULTED_CASE_UPDATED_JSON = "application/vnd" +
            ".public.progression.hearing-resulted-case-updated+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";

    private MessageProducer publicEventCaseUpdatedAndHearingResulted;
    private MessageConsumer publicEventMessageConsumerCaseUpdatedAndHearingResulted;
    private MessageConsumer privateEventMessageConsumerCaseUpdatedAndHearingResulted;
    private MessageConsumer privateEventMessageConsumerDefendantCourtProceedingsUpdated;
    private final UUID metadataId;
    private final UUID userId;
    private final UUID caseId;
    private final HearingData hearingData;
    private final ListedCaseData listedCaseData;
    private String request;

    public CaseUpdatedAndDefendantProceedingsConcludedSteps(UUID caseId, HearingData hearingData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.userId = randomUUID();
        this.metadataId = randomUUID();
        this.listedCaseData = hearingData.getListedCases().get(0);

        this.publicEventCaseUpdatedAndHearingResulted = QueueUtil.publicEvents.createProducer();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        this.publicEventMessageConsumerCaseUpdatedAndHearingResulted = QueueUtil.publicEvents.createConsumer(PUBLIC_EVENT_HEARING_RESULTED_CASE_UPDATED);
        this.privateEventMessageConsumerCaseUpdatedAndHearingResulted = privateEvents.createConsumer(LISTING_EVENTS_CASE_RESULTED_AND_DEFENDANT_PROCEEDINGS_CONCLUDED);
        this.privateEventMessageConsumerDefendantCourtProceedingsUpdated = privateEvents.createConsumer(LISTING_EVENT_DEFENDANT_COURT_PROCEEDINGS_UPDATED);
    }

    public void whenPublicEventCaseUpdatedAndHearingResultedIsPublished() {
        final UUID defendantId = listedCaseData.getDefendants().get(0).getDefendantId();
        final String eventPayloadString = getPayload("public.progression.hearing-resulted-case-updated.json")
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString());
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        QueueUtil.sendMessage(publicEventCaseUpdatedAndHearingResulted,
                PUBLIC_EVENT_HEARING_RESULTED_CASE_UPDATED,
                jsonObject,
                metadataOf(metadataId, PUBLIC_EVENT_HEARING_RESULTED_CASE_UPDATED)
                        .withUserId(userId.toString())
                        .build());
        this.request = jsonObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n loggedHeader {}",
                MEDIA_TYPE_HEARING_RESULTED_CASE_UPDATED_JSON, request, getLoggedInHeader());

    }

    public void verifyPrivateEventCaseResultedDefendantProceedingsUpdatedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventMessageConsumerCaseUpdatedAndHearingResulted);
        LOGGER.debug("jsonResponse from privateEventMessageConsumerCaseUpdatedAndHearingResulted: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("prosecutionCase"), CoreMatchers.notNullValue());
    }

    public void verifyPrivateEventDefendantCourtProceedingsUpdatedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventMessageConsumerDefendantCourtProceedingsUpdated);
        LOGGER.debug("jsonResponse from privateEventMessageConsumerDefendantCourtProceedingsUpdated: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("prosecutionCase"), CoreMatchers.notNullValue());
    }

    public void verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPI(boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));
        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].proceedingsConcluded",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[0].caseStatus",
                                        equalTo("CLOSED"))
                        )));
    }

    @Override
    public void close() {
        try {
            this.publicEventCaseUpdatedAndHearingResulted.close();
            this.publicEventMessageConsumerCaseUpdatedAndHearingResulted.close();
            this.privateEventMessageConsumerCaseUpdatedAndHearingResulted.close();
            this.privateEventMessageConsumerDefendantCourtProceedingsUpdated.close();
        } catch (JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
