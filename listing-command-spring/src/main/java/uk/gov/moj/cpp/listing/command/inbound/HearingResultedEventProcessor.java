package uk.gov.moj.cpp.listing.command.inbound;

import static jakarta.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Port of the JEE {@code HearingResultedEventProcessor} (MBD refactor P2.4): set-hearing-resulted-status
 * for the resulted hearing, except an SJP hearing. A payload without {@code hearing} fails, as in JEE
 * (the message is redelivered, then dead-lettered).
 */
@Component
public class HearingResultedEventProcessor extends InboundProcessor {

    static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    static final String COMMAND_SET_HEARING_RESULTED_STATUS = "listing.command.set-hearing-resulted-status";

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventProcessor.class);

    private final ProcessorSender sender;

    public HearingResultedEventProcessor(final ProcessorQueue processorQueue, final ProcessorSender sender) {
        super(processorQueue);
        this.sender = sender;
        on(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, this::handlePublicHearingResulted);
    }

    public void handlePublicHearingResulted(final Envelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'public.events.hearing.hearing-resulted' event with payload: {}", event.metadata());
        }

        final JsonObject eventPayload = event.payload();
        final JsonObject hearingJson = eventPayload.getJsonObject("hearing");

        if (hearingJson.getBoolean("isSJPHearing", false)) {
            return;
        }

        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder()
                .add("hearingId", hearingJson.getString("id"));

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(COMMAND_SET_HEARING_RESULTED_STATUS),
                commandPayloadBuilder.build()));
    }
}
