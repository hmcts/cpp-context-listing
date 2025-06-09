package uk.gov.moj.cpp.listing.event.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventProcessor.class.getName());

    @Inject
    private Sender sender;

    @Handles("public.events.hearing.hearing-resulted")
    public void handlePublicHearingResulted(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'public.events.hearing.hearing-resulted' event with payload: {}", event.toObfuscatedDebugString());
        }

        final JsonObject eventPayload = event.payloadAsJsonObject();
        final JsonObject hearingJson = eventPayload.getJsonObject("hearing");

        if (hearingJson.getBoolean("isSJPHearing", false)) {
            return;
        }

        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder()
                .add("hearingId", hearingJson.getString("id"));

        this.sender.send(envelopeFrom
                (metadataFrom(event.metadata()).withName("listing.command.set-hearing-resulted-status"),
                        commandPayloadBuilder.build()));

    }
}
