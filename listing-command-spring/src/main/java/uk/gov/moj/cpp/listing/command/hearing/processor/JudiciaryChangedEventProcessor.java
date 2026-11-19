package uk.gov.moj.cpp.listing.command.hearing.processor;

import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import org.springframework.stereotype.Component;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of the JEE {@code JudiciaryChangedEventProcessor} (MBD refactor P2.3.2, ADR 015), logic unchanged.
 */
@Component
public class JudiciaryChangedEventProcessor implements ProcessorPort {

    @Override
    public void register(final EventHandlers processors) {
        processors
                .onJson(uk.gov.justice.listing.events.JudiciaryChangedForHearingsStatus.class, this::handlesJudiciaryChangedForHearingsStatus);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JudiciaryChangedEventProcessor.class);
    private static final String EVENT_PAYLOAD_INFO_STRING = "Received '{}' event with payload {}";
    private static final String LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS = "listing.events.judiciary-changed-for-hearings-status";
    public static final String PUBLIC_LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS = "public.listing.judiciary-changed-for-hearings-status";

    @Autowired
    private ProcessorSender sender;

    public void handlesJudiciaryChangedForHearingsStatus(Envelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS, jsonEnvelope.metadata());
        }

        this.sender.send(envelopeFrom
                (metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS),
                        jsonEnvelope.payload())
        );

    }
}
