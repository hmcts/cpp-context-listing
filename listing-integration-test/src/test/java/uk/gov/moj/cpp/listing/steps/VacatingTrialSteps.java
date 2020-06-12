package uk.gov.moj.cpp.listing.steps;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
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
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.io.IOException;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VacatingTrialSteps extends AbstractIT implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(VacatingTrialSteps.class);

    private static final String PUBLIC_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    private static final String EVENT_HEARING_VACATE_TRIAL = "listing.events.hearing-trial-vacated";
    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String FIELD_VACATE_TRIAL_REASON = "vacatedTrialReasonId";
    private static final String FIELD_HEARING_ID = "hearingId";

    private static HearingData hearingData;
    private final UUID reasonId = randomUUID();
    private final String hearingId;
    private final boolean isVacated = true;
    private String request;

    private MessageConsumer privateMessageConsumerHearingVacateTrial;
    private MessageProducer publicEventHearingTrialVacated;

    public VacatingTrialSteps(HearingsData hearingsData) {
        hearingData = hearingsData.getHearingData().get(0);
        hearingId = hearingData.getId().toString();
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        createMessageConsumer();
    }

    private void createMessageConsumer() {
        publicEventHearingTrialVacated = QueueUtil.publicEvents.createProducer();
        privateMessageConsumerHearingVacateTrial = privateEvents.createConsumer(EVENT_HEARING_VACATE_TRIAL);
    }

    public void whenPublicEventHearingTrialVacatedIsPublished() throws IOException {
        final String eventPayloadString = getStringFromResource("public.hearing.trial-vacated.json")
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

    public void whenPublicEventHearingTrialVacatedIsPublishedWithEmptyVacatedTrialReasonId() throws IOException {
        final String eventPayloadString = getStringFromResource("public.hearing.trial-vacated_empty-reasonid.json")
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

    public void verifyHearingVacatingTrialEvent() {
        JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingVacateTrial);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingVacateTrial: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get(FIELD_VACATE_TRIAL_REASON), is(reasonId.toString()));
        assertThat(jsonResponse.get(FIELD_HEARING_ID), is(hearingId));
    }

    public void verifyHearingVacatingTrialEventForEmptyReasonId() {
        JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingVacateTrial);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingVacateTrial: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get(FIELD_VACATE_TRIAL_REASON), nullValue());
        assertThat(jsonResponse.get(FIELD_HEARING_ID), is(hearingId));
    }

    @Override
    public void close() {
        try {
            privateMessageConsumerHearingVacateTrial.close();
            publicEventHearingTrialVacated.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerVacateTrial: {}", e.getMessage());
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
                                        is(isVacated)),
                                withJsonPath("$.vacatedTrialReasonId",
                                        is(reasonId.toString()))

                        )));
    }

    public void verifyVacatedTrialWtihEmptyReasonIdWhenQueryingFromAPI() {

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

    private static String getStringFromResource(final String path) throws IOException {
        return Resources.toString(getResource(path), defaultCharset());
    }

}
