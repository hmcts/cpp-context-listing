package uk.gov.moj.cpp.listing.event.processor;

import org.slf4j.Logger;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class CreateNextHearingRequestedEventProcessor {

    private static final String PRIVATE_EVENT_CREATE_NEXT_HEARING_REQUESTED = "listing.events.create-next-hearing-requested";
    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    public static final String PUBLIC_EVENT_CREATE_NEXT_HEARING_REQUESTED = "public.listing.create-next-hearing-requested";

    @Inject
    private Sender sender;

    @Inject
    private Logger logger;


    @Handles(PRIVATE_EVENT_CREATE_NEXT_HEARING_REQUESTED)
    public void createNextHearingRequestedEvent(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CREATE_NEXT_HEARING_REQUESTED, envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_CREATE_NEXT_HEARING_REQUESTED),
                payload));
    }

}
