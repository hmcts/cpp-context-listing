package uk.gov.moj.cpp.listing.steps;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveOffencesFromHearingSteps extends AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveOffencesFromHearingSteps.class);
    public static final String PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING = "public.hearing.selected-offences-removed-from-existing-hearing";
    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI = "public.listing.updated-hearing-in-staging-hmi";
    public static final String PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-allocated-hearing";

    private final JmsMessageProducerClient publicHearingEventOffencesRemovedFromHearing;
    private final JmsMessageConsumerClient publicMessageConsumerHmiHearingUpdated;
    private final JmsMessageConsumerClient publicSelectedOffenceRemovedFromHearing;
    private String hearingId;

    public RemoveOffencesFromHearingSteps() {
        publicHearingEventOffencesRemovedFromHearing = publicEvents.createPublicProducer();
        publicMessageConsumerHmiHearingUpdated = publicEvents.createPublicConsumer(PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI);
        publicSelectedOffenceRemovedFromHearing = publicEvents.createPublicConsumer(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING);
    }

    public void whenRaisedOffencesRemovedPublicEvent(final String hearingId, final List<String> offences ){
        this.hearingId = hearingId;
        final JsonObjectBuilder builder = Json.createObjectBuilder().add("hearingId", hearingId);
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        offences.forEach(id -> arrayBuilder.add(id));
        builder.add("offenceIds",arrayBuilder.build());

        QueueUtil.sendMessage(
                publicHearingEventOffencesRemovedFromHearing,
                PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING,
                builder.build(),
                metadataOf(randomUUID(), PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING).withUserId(randomUUID().toString()).build());
    }

    public void verifyPublicListingOffencesRemovedFromAllocatedHearing(){
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicSelectedOffenceRemovedFromHearing);
        Assert.assertThat(jsonResponse.get("hearingId"), is(hearingId));
    }

    public void verifyHmiPublicEventForUpdateHearing() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHmiHearingUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHmiHearingUpdated: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearing.id"), is(hearingId));
    }

}
