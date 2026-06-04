package uk.gov.moj.cpp.listing.steps;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;

import java.util.List;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import io.restassured.path.json.JsonPath;

public class RemoveOffencesFromHearingSteps extends AbstractIT {
    public static final String PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING = "public.hearing.selected-offences-removed-from-existing-hearing";
    public static final String PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-allocated-hearing";

    private final JmsMessageProducerClient publicHearingEventOffencesRemovedFromHearing;
    private final JmsMessageConsumerClient publicSelectedOffenceRemovedFromHearing;
    private String hearingId;

    public RemoveOffencesFromHearingSteps() {
        publicHearingEventOffencesRemovedFromHearing = publicEvents.createPublicProducer();
        publicSelectedOffenceRemovedFromHearing = publicEvents.createPublicConsumer(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING);
    }

    public void whenRaisedOffencesRemovedPublicEvent(final String hearingId, final List<String> offences) {
        this.hearingId = hearingId;
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder().add("hearingId", hearingId);
        final JsonArrayBuilder arrayBuilder = JsonObjects.createArrayBuilder();
        offences.forEach(arrayBuilder::add);
        builder.add("offenceIds", arrayBuilder.build());

        sendMessage(
                publicHearingEventOffencesRemovedFromHearing,
                PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING,
                builder.build(),
                metadataOf(randomUUID(), PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING).withUserId(randomUUID().toString()).build());
    }

    public void verifyPublicListingOffencesRemovedFromAllocatedHearing() {
        final JsonPath jsonResponse = retrieveMessage(publicSelectedOffenceRemovedFromHearing,
                org.hamcrest.CoreMatchers.containsString(hearingId));
        assertThat(jsonResponse.get("hearingId"), is(hearingId));
        assertThat(jsonResponse.get("isResultFlow"), is(false));
    }

}
