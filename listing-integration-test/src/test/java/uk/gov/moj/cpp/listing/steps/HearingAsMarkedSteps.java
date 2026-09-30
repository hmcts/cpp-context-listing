package uk.gov.moj.cpp.listing.steps;

import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingAsMarkedSteps extends AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAsMarkedSteps.class);

    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String LISTING_COMMAND_DUPLICATE_UNALLOCATED_HEARING = "listing.mark-unallocated-hearing-as-duplicate";
    private static final String MEDIA_TYPE_DUPLICATE_UNALLOCATED_HEARING = "application/vnd.listing.duplicate-unallocated-hearing+json";
    public static final String PUBLIC_LISTING_DELETED_HEARING_IN_STAGING_HMI = "public.listing.deleted-hearing-in-staging-hmi";


    private final JmsMessageProducerClient publicEventHearingMarkedAsDuplicateEvent;
    private final JmsMessageConsumerClient publicMessageConsumerHearingMarkedAsDuplicateEvent;
    private final JmsMessageConsumerClient publicMessageConsumerHmiHearingDeleted;

    private String request;

    private final HearingData hearingData;

    public HearingAsMarkedSteps(final HearingData hearingData) {
        this.hearingData = hearingData;

        publicMessageConsumerHearingMarkedAsDuplicateEvent = publicEvents.createPublicConsumer(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT);
        publicEventHearingMarkedAsDuplicateEvent = QueueUtil.publicEvents.createPublicProducer();
        publicMessageConsumerHmiHearingDeleted = publicEvents.createPublicConsumer(PUBLIC_LISTING_DELETED_HEARING_IN_STAGING_HMI);
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenHearingMarkedAsDuplicatePublicEventIsPublished() {
        final String eventPayloadString = getPayload("public.hearing.marked-as-duplicate.json")
                .replaceAll("HEARING_ID", hearingData.getId().toString())
                .replaceAll("CASE_ID_1", hearingData.getListedCases().get(0).getCaseId().toString())
                .replaceAll("DEFENDANT_ID_1", hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())
                .replaceAll("OFFENCE_ID_1", hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())
                .replaceAll("CASE_ID_2", hearingData.getListedCases().get(1).getCaseId().toString())
                .replaceAll("DEFENDANT_ID_2", hearingData.getListedCases().get(1).getDefendants().get(1).getDefendantId().toString())
                .replaceAll("OFFENCE_ID_2", hearingData.getListedCases().get(1).getDefendants().get(1).getOffences().get(1).getOffenceId().toString());

        JsonObject hearingMarkedAsDuplicateObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        QueueUtil.sendMessage(
                publicEventHearingMarkedAsDuplicateEvent,
                PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT,
                hearingMarkedAsDuplicateObject,
                metadataOf(randomUUID(), PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT).withUserId(randomUUID().toString()).build());

        request = hearingMarkedAsDuplicateObject.toString();
    }

    public void whenUnallocatedHearingMarkedAsDuplicateCommandIsSent() {
        final String duplicateUnallocatedHearingUrl = String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_COMMAND_DUPLICATE_UNALLOCATED_HEARING), hearingData.getId()));

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {}", duplicateUnallocatedHearingUrl, MEDIA_TYPE_DUPLICATE_UNALLOCATED_HEARING);

        restClient.postCommand(duplicateUnallocatedHearingUrl, MEDIA_TYPE_DUPLICATE_UNALLOCATED_HEARING, null, getLoggedInHeader());
    }

    public void verifyHearingMarkedAsDuplicatePublicEventInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingMarkedAsDuplicateEvent,
                org.hamcrest.CoreMatchers.containsString(jsRequest.getString("hearingId")));
        LOGGER.info("jsonResponse from publicMessageConsumerHearingMarkedAsDuplicateEvent: {}", jsonResponse.prettify());


        assertThat(jsonResponse.get("hearingId").toString(), is(jsRequest.getString("hearingId")));
        assertThat(jsonResponse.get("prosecutionCaseIds[0]").toString(), is(jsRequest.getString("prosecutionCaseIds[0]")));
        assertThat(jsonResponse.get("prosecutionCaseIds[1]").toString(), is(jsRequest.getString("prosecutionCaseIds[1]")));
        assertThat(jsonResponse.get("defendantIds[0]").toString(), is(jsRequest.getString("defendantIds[0]")));
        assertThat(jsonResponse.get("defendantIds[1]").toString(), is(jsRequest.getString("defendantIds[1]")));
        assertThat(jsonResponse.get("offenceIds[0]").toString(), is(jsRequest.getString("offenceIds[0]")));
        assertThat(jsonResponse.get("offenceIds[1]").toString(), is(jsRequest.getString("offenceIds[1]")));
    }

    public void verifyHmiPublicEventForDeleteHearing() {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHmiHearingDeleted,
                org.hamcrest.CoreMatchers.containsString(hearingData.getId().toString()));
        LOGGER.info("jsonResponse from publicMessageConsumerHmiHearingUpdated: {}", jsonResponse.prettify());
        assertThat(jsonResponse.getString("hearingId"), is(hearingData.getId().toString()));
        assertThat(jsonResponse.getString("cancellationReasonCode"), is("CNCL"));
        assertThat(jsonResponse.getList("caseAndApplicationIds").size(), is(3));
    }

}
