package uk.gov.moj.cpp.listing.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingSequencedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingSequencedEventProcessor.class);

    private static final String EVENT_PAYLOAD_INFO_STRING = "Received '{}' event with payload {}";
    private static final String LISTING_EVENTS_HEARING_SEQUENCED = "listing.events.hearing-days-sequenced";
    public static final String PUBLIC_LISTING_EVENTS_HEARING_SEQUENCED = "public.listing.hearing-days-sequenced";

    @Inject
    private Sender sender;

    @Handles(LISTING_EVENTS_HEARING_SEQUENCED)
    public void handleHearingSequencedEvent(JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_HEARING_SEQUENCED, jsonEnvelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom
                (metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_EVENTS_HEARING_SEQUENCED),
                        jsonEnvelope.payloadAsJsonObject())
        );
    }
}
