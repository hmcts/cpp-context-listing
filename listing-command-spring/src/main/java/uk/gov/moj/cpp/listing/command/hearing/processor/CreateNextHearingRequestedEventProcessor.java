package uk.gov.moj.cpp.listing.command.hearing.processor;

import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.json.JsonObject;

import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

/**
 * Port of the JEE {@code CreateNextHearingRequestedEventProcessor} (MBD refactor P2.3.2, ADR 015), logic unchanged.
 */
@Component
public class CreateNextHearingRequestedEventProcessor implements ProcessorPort {

    @Override
    public void register(final EventHandlers processors) {
        processors
                .onJson(uk.gov.justice.listing.events.CreateNextHearingRequested.class, this::createNextHearingRequestedEvent);
    }


    private static final String PRIVATE_EVENT_CREATE_NEXT_HEARING_REQUESTED = "listing.events.create-next-hearing-requested";
    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    public static final String PUBLIC_EVENT_CREATE_NEXT_HEARING_REQUESTED = "public.listing.create-next-hearing-requested";

    @Autowired
    private ProcessorSender sender;

    private static final Logger logger = LoggerFactory.getLogger(CreateNextHearingRequestedEventProcessor.class);


    public void createNextHearingRequestedEvent(final Envelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CREATE_NEXT_HEARING_REQUESTED, envelope.metadata());
        }
        final JsonObject payload = envelope.payload();
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_CREATE_NEXT_HEARING_REQUESTED),
                payload));
    }

}
