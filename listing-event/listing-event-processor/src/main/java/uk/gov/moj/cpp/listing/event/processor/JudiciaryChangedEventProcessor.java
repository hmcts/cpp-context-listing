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
public class JudiciaryChangedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JudiciaryChangedEventProcessor.class);
    private static final String EVENT_PAYLOAD_INFO_STRING = "Received '{}' event with payload {}";
    private static final String LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS = "listing.events.judiciary-changed-for-hearings-status";
    public static final String PUBLIC_LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS = "public.listing.judiciary-changed-for-hearings-status";

    @Inject
    private Sender sender;

    @Handles(LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS)
    public void handlesJudiciaryChangedForHearingsStatus(JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS, jsonEnvelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom
                (metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS),
                        jsonEnvelope.payloadAsJsonObject())
        );

    }
}
