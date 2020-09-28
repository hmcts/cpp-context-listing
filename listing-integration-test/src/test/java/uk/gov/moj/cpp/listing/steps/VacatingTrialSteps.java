package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
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
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.Utilities.DEFAULT_NOT_HAPPENED_TIMEOUT_IN_MILLIS;
import static uk.gov.moj.cpp.listing.utils.Utilities.listenForPrivateEvent;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;
import uk.gov.moj.cpp.listing.utils.Utilities;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VacatingTrialSteps extends AbstractIT implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(VacatingTrialSteps.class);

    private static final String PUBLIC_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    private static final String EVENT_LISTING_TRIAL_VACATED = "listing.events.trial-vacated";
    private static final String EVENT_HEARING_TRIAL_VACATED = "listing.events.hearing-trial-vacated";
    private static final String EVENT_HEARING_SLOTS_FREED = "listing.events.available-slots-for-hearing-freed";
    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    public static final String LISTING_COMMAND_VACATE_TRIAL = "listing.command.hearing-vacate-trial";
    public static final String MEDIA_TYPE_VACATE_TRIAL = "application/vnd.listing.command.vacate-trial+json";
    private static final String FIELD_VACATE_TRIAL_REASON = "vacatedTrialReasonId";
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String FIELD_ALLOCATED = "allocated";

    private final UUID reasonId = randomUUID();
    private final String hearingId;
    private String request;

    private MessageConsumer privateMessageConsumerHearingVacateTrial;
    private MessageConsumer privateMessageConsumerListingTrialVacated;
    private MessageConsumer privateMessageConsumerHearingSlotsFreed;
    private MessageProducer publicEventHearingTrialVacated;

    public VacatingTrialSteps(final HearingsData hearingsData) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        hearingId = hearingData.getId().toString();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        createMessageConsumer();
    }

    private void createMessageConsumer() {
        publicEventHearingTrialVacated = publicEvents.createProducer();
        privateMessageConsumerHearingVacateTrial = privateEvents.createConsumer(EVENT_HEARING_TRIAL_VACATED);
        privateMessageConsumerListingTrialVacated = privateEvents.createConsumer(EVENT_LISTING_TRIAL_VACATED);
        privateMessageConsumerHearingSlotsFreed = privateEvents.createConsumer(EVENT_HEARING_SLOTS_FREED);
    }

    public void whenPublicEventHearingTrialVacatedIsPublished() {
        final String eventPayloadString = getPayload("public.hearing.trial-vacated.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("REASON_ID", reasonId.toString());
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        QueueUtil.sendMessage(publicEventHearingTrialVacated,
                PUBLIC_HEARING_TRIAL_VACATED,
                jsonObject,
                metadataOf(randomUUID(), PUBLIC_HEARING_TRIAL_VACATED)
                        .withUserId(USER_ID_VALUE.toString())
                        .build());
        this.request = jsonObject.toString();
        LOGGER.info("Event published:\n\t \n\tPayload = {}\n\n loggedHeader {}", request, getLoggedInHeader());
    }

    public void whenPublicEventHearingTrialVacatedIsPublishedWithEmptyVacatedTrialReasonId() {
        final String eventPayloadString = getPayload("public.hearing.trial-vacated_empty-reasonid.json")
                .replaceAll("HEARING_ID", hearingId);
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        QueueUtil.sendMessage(publicEventHearingTrialVacated,
                PUBLIC_HEARING_TRIAL_VACATED,
                jsonObject,
                metadataOf(randomUUID(), PUBLIC_HEARING_TRIAL_VACATED)
                        .withUserId(USER_ID_VALUE.toString())
                        .build());
        this.request = jsonObject.toString();
        LOGGER.info("Event published:\n\t \n\tPayload = {}\n\n loggedHeader {}", request, getLoggedInHeader());
    }

    public void verifyHearingTrialVacatedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingVacateTrial);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingVacateTrial: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get(FIELD_VACATE_TRIAL_REASON), is(reasonId.toString()));
        assertThat(jsonResponse.get(FIELD_HEARING_ID), is(hearingId));
    }

    public void verifyListingTrialVacatedEvent(final boolean allocated) {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerListingTrialVacated);
        LOGGER.info("jsonResponse from privateMessageConsumerListingTrialVacated: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get(FIELD_VACATE_TRIAL_REASON), is(reasonId.toString()));
        assertThat(jsonResponse.get(FIELD_HEARING_ID), is(hearingId));
        assertThat(jsonResponse.get(FIELD_ALLOCATED), is(allocated));
    }

    public void verifyHearingVacatingTrialEventForEmptyReasonId() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingVacateTrial);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingVacateTrial: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get(FIELD_VACATE_TRIAL_REASON), nullValue());
        assertThat(jsonResponse.get(FIELD_HEARING_ID), is(hearingId));
    }

    @Override
    public void close() {
        try {
            privateMessageConsumerHearingVacateTrial.close();
            privateMessageConsumerListingTrialVacated.close();
            privateMessageConsumerHearingSlotsFreed.close();
            publicEventHearingTrialVacated.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing one of privateMessageConsumerVacateTrial, privateMessageConsumerHearingSlotsFreed or publicEventHearingTrialVacated producers/consumers: {}", e.getMessage());
        }
    }

    public void verifyVacatedTrialWhenQueryingFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id",
                                        is(hearingId)),
                                withJsonPath("$.isVacatedTrial",
                                        is(true)),
                                withJsonPath("$.vacatedTrialReasonId",
                                        is(reasonId.toString()))

                        )));
    }

    public void verifyVacatedTrialWithEmptyReasonIdWhenQueryingFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id",
                                        is(hearingId)),
                                withJsonPath("$.isVacatedTrial",
                                        is(false)),
                                withJsonPath("$.vacatedTrialReasonId",
                                        is(""))

                        )));
    }

    public void verifyAvailableSlotsForHearingFreedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingSlotsFreed);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingSlotsFreed: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get(FIELD_HEARING_ID), is(hearingId));
    }

    public void verifyAvailableSlotsForHearingFreedEventNotRaised() {
        try (final Utilities.EventListener hearingSlotsFreedListener = listenForPrivateEvent(EVENT_HEARING_SLOTS_FREED)
                .withFilter(isJson(withJsonPath("$.hearingId", is(hearingId))))) {
            hearingSlotsFreedListener.expectNoneWithin(DEFAULT_NOT_HAPPENED_TIMEOUT_IN_MILLIS);
        }
    }

    public void whenHearingIsVacated() {
        final JsonObject vacatePayload = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("vacatedTrialReasonId", reasonId.toString())
                .build();

        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_COMMAND_VACATE_TRIAL), hearingId));

        final Response response = restClient.postCommand(url, MEDIA_TYPE_VACATE_TRIAL, vacatePayload.toString(), getLoggedInHeader());
        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
    }
}
