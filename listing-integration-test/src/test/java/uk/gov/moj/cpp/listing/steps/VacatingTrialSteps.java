package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

public class VacatingTrialSteps extends AbstractIT {

    private static final String PUBLIC_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    public static final String LISTING_COMMAND_VACATE_TRIAL = "listing.command.hearing-vacate-trial";
    public static final String MEDIA_TYPE_VACATE_TRIAL = "application/vnd.listing.command.vacate-trial+json";

    private final UUID reasonId = randomUUID();
    private final String hearingId;

    private JmsMessageProducerClient publicEventHearingTrialVacated;

    public VacatingTrialSteps(final HearingsData hearingsData) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        hearingId = hearingData.getId().toString();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        createMessageConsumer();
    }

    private void createMessageConsumer() {
        publicEventHearingTrialVacated = publicEvents.createPublicProducer();
    }

    public void whenPublicEventHearingTrialVacatedIsPublished() {
        final String eventPayloadString = getPayload("public.hearing.trial-vacated.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("REASON_ID", reasonId.toString());
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        sendMessage(publicEventHearingTrialVacated,
                PUBLIC_HEARING_TRIAL_VACATED,
                jsonObject,
                metadataOf(randomUUID(), PUBLIC_HEARING_TRIAL_VACATED)
                        .withUserId(USER_ID_VALUE.toString())
                        .build());
    }

    public void whenPublicEventHearingTrialVacatedIsPublishedWithEmptyVacatedTrialReasonId() {
        final String eventPayloadString = getPayload("public.hearing.trial-vacated_empty-reasonid.json")
                .replaceAll("HEARING_ID", hearingId);
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        sendMessage(publicEventHearingTrialVacated,
                PUBLIC_HEARING_TRIAL_VACATED,
                jsonObject,
                metadataOf(randomUUID(), PUBLIC_HEARING_TRIAL_VACATED)
                        .withUserId(USER_ID_VALUE.toString())
                        .build());
    }

    public void verifyVacatedTrialWhenQueryingFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()).build())
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

    public void whenHearingIsVacatedFromWithinListing() {
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
