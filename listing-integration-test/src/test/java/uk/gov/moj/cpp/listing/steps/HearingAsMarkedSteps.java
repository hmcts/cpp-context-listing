package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.Arrays;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingAsMarkedSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAsMarkedSteps.class);

    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String PRIVATE_HEARING_MARKED_AS_DUPLICATE_EVENT = "listing.events.hearing-marked-as-duplicate";
    private static final String PRIVATE_HEARING_MARKED_AS_DUPLICATE_FOR_CASE_EVENT = "listing.events.hearing-marked-as-duplicate-for-case";


    private final MessageProducer publicEventHearingMarkedAsDuplicateEvent;
    private final MessageConsumer publicMessageConsumerHearingMarkedAsDuplicateEvent;
    private final MessageConsumer privateMessageConsumerHearingMarkedAsDuplicateEvent;
    private final MessageConsumer privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent;

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";


    private String request;

    private final HearingData hearingData;


    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public HearingAsMarkedSteps(final HearingData hearingData) {
        this.hearingData = hearingData;

        publicMessageConsumerHearingMarkedAsDuplicateEvent = publicEvents.createConsumer(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT);
        publicEventHearingMarkedAsDuplicateEvent = QueueUtil.publicEvents.createProducer();
        privateMessageConsumerHearingMarkedAsDuplicateEvent = privateEvents.createConsumer(PRIVATE_HEARING_MARKED_AS_DUPLICATE_EVENT);
        privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent = privateEvents.createConsumer(PRIVATE_HEARING_MARKED_AS_DUPLICATE_FOR_CASE_EVENT);
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenHearingMarkedAsDuplicatePublicEventIsPublished(){
        final String eventPayloadString = getPayload("public.hearing.marked-as-duplicate.json")
                .replaceAll("HEARING_ID", hearingData.getId().toString())
                .replaceAll("CASE_ID_1", hearingData.getListedCases().get(0).getCaseId().toString())
                .replaceAll("DEFENDANT_ID_1",hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())
                .replaceAll("OFFENCE_ID_1", hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())
                .replaceAll("CASE_ID_2", hearingData.getListedCases().get(1).getCaseId().toString())
                .replaceAll("DEFENDANT_ID_2", hearingData.getListedCases().get(1).getDefendants().get(1).getDefendantId().toString())
                .replaceAll("OFFENCE_ID_2",hearingData.getListedCases().get(1).getDefendants().get(1).getOffences().get(1).getOffenceId().toString());

        JsonObject hearingMarkedAsDuplicateObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        QueueUtil.sendMessage(
                publicEventHearingMarkedAsDuplicateEvent,
                PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT,
                hearingMarkedAsDuplicateObject,
                metadataOf(randomUUID(), PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT).withUserId(randomUUID().toString()).build());

        request = hearingMarkedAsDuplicateObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, request, getLoggedInHeader());
    }

    public void verifyHearingMarkedAsDuplicatePublicEventInActiveMQ(){
        final JsonPath jsRequest = new JsonPath(request);
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingMarkedAsDuplicateEvent);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingMarkedAsDuplicateEvent: {}", jsonResponse.prettify());


        assertThat(jsonResponse.get("hearingId").toString(), is(jsRequest.getString("hearingId")));
        assertThat(jsonResponse.get("prosecutionCaseIds[0]").toString(), is(jsRequest.getString("prosecutionCaseIds[0]")));
        assertThat(jsonResponse.get("prosecutionCaseIds[1]").toString(), is(jsRequest.getString("prosecutionCaseIds[1]")));
        assertThat(jsonResponse.get("defendantIds[0]").toString(), is(jsRequest.getString("defendantIds[0]")));
        assertThat(jsonResponse.get("defendantIds[1]").toString(), is(jsRequest.getString("defendantIds[1]")));
        assertThat(jsonResponse.get("offenceIds[0]").toString(), is(jsRequest.getString("offenceIds[0]")));
        assertThat(jsonResponse.get("offenceIds[1]").toString(), is(jsRequest.getString("offenceIds[1]")));


    }

    public void verifyHearingMarkedAsDuplicateInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingMarkedAsDuplicateEvent);

        LOGGER.debug("jsonResponse from privateMessageConsumerHearingMarkedAsDuplicateEvent: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId").toString(), is(jsRequest.getString("hearingId")));
    }

    public void verifyHearingMarkedAsDuplicateForCaseInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);

        final JsonPath jsonResponse1 = QueueUtil.retrieveMessage(privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent);
        LOGGER.debug("jsonResponse1 from privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent: {}", jsonResponse1.prettify());


        final JsonPath jsonResponse2 = QueueUtil.retrieveMessage(privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent);
        LOGGER.debug("jsonResponse2 from privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent: {}", jsonResponse1.prettify());
        final List<String> responseCaseIds = Arrays.asList(jsonResponse1.get("caseId").toString(),
                jsonResponse2.get("caseId").toString());

        assertThat(jsonResponse1.get("hearingId").toString(), is(jsRequest.getString("hearingId")));
        assertThat(jsonResponse2.get("hearingId").toString(), is(jsRequest.getString("hearingId")));
        assertThat(responseCaseIds, hasItems(jsRequest.getString("prosecutionCaseIds[0]")));
        assertThat(responseCaseIds, hasItems(jsRequest.getString("prosecutionCaseIds[1]")));
    }

    @Override
    public void close() {
        try {
            publicEventHearingMarkedAsDuplicateEvent.close();
            publicMessageConsumerHearingMarkedAsDuplicateEvent.close();
            privateMessageConsumerHearingMarkedAsDuplicateEvent.close();
            privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void verifyDeletedFromHearingViewStore() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"),
                        hearingData.getCourtCentreId(), false));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.hearings", emptyCollection())
                        ));
    }
}
