package uk.gov.moj.cpp.listing.steps;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;


import com.jayway.restassured.path.json.JsonPath;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

public class RemoveOffencesFromHearingSteps extends AbstractIT implements AutoCloseable{
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveOffencesFromHearingSteps.class);
    public static final String PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING = "public.hearing.selected-offences-removed-from-existing-hearing";
    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI = "public.listing.updated-hearing-in-staging-hmi";

    private final MessageProducer publicHearingEventOffencesRemovedFromHearing;
    private final MessageConsumer publicMessageConsumerHmiHearingUpdated;
    private String hearingId;

    public RemoveOffencesFromHearingSteps() {
        publicHearingEventOffencesRemovedFromHearing = publicEvents.createProducer();
        publicMessageConsumerHmiHearingUpdated = publicEvents.createConsumer(PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI);
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

    public void verifySelectedOffenceRemovedFromHearing(final String offenceId){

    }

    public void verifyHmiPublicEventForUpdateHearing() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHmiHearingUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHmiHearingUpdated: {}", jsonResponse.prettify());
        Assert.assertThat(jsonResponse.get("hearing.id"), is(hearingId));
    }

    @Override
    public void close() {
        try {
            publicHearingEventOffencesRemovedFromHearing.close();
            publicMessageConsumerHmiHearingUpdated.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


}
