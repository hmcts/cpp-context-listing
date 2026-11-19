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

/** Port of the JEE {@code CourtApplicationDeletedProcessor} (MBD refactor P2.4): delete-court-application-hearing with hearingId and applicationId. */
@Component
public class CourtApplicationDeletedProcessor extends InboundProcessor {

    static final String PUBLIC_PROGRESSION_EVENTS_COURT_APPLICATION_DELETED = "public.progression.events.court-application-deleted";
    static final String COMMAND_DELETE_COURT_APPLICATION_HEARING = "listing.command.delete-court-application-hearing";

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationDeletedProcessor.class);

    private final ProcessorSender sender;

    public CourtApplicationDeletedProcessor(final ProcessorQueue processorQueue, final ProcessorSender sender) {
        super(processorQueue);
        this.sender = sender;
        on(PUBLIC_PROGRESSION_EVENTS_COURT_APPLICATION_DELETED, this::handleCourtApplicationDeletedPublicEvent);
    }

    public void handleCourtApplicationDeletedPublicEvent(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received '{}' event with payload {}", PUBLIC_PROGRESSION_EVENTS_COURT_APPLICATION_DELETED, envelope.metadata());
        }

        final JsonObjectBuilder commandBuilder = createObjectBuilder();
        final JsonObject payload = envelope.payload();
        commandBuilder
                .add("hearingId", payload.getJsonString("hearingId"))
                .add("applicationId", payload.getJsonString("applicationId"));

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_DELETE_COURT_APPLICATION_HEARING),
                commandBuilder.build()));
    }
}
