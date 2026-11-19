package uk.gov.moj.cpp.listing.command.inbound;

import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The inbound handler of the JEE {@code UpdateExistingHearingEventProcessor} (line 124; MBD refactor
 * P2.4; its private-event handlers are {@code command.hearing.processor.UpdateExistingHearingEventProcessor}):
 * add-cases-to-hearing with the payload passed through.
 */
@Component
public class RelatedHearingUpdatedForAdhocHearingProcessor extends InboundProcessor {

    static final String PUBLIC_PROGRESSION_RELATED_HEARING_UPDATED_FOR_ADHOC_HEARING = "public.progression.related-hearing-updated-for-adhoc-hearing";
    static final String COMMAND_ADD_CASES_TO_HEARING = "listing.command.add-cases-to-hearing";

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedHearingUpdatedForAdhocHearingProcessor.class);

    private final ProcessorSender sender;

    public RelatedHearingUpdatedForAdhocHearingProcessor(final ProcessorQueue processorQueue, final ProcessorSender sender) {
        super(processorQueue);
        this.sender = sender;
        on(PUBLIC_PROGRESSION_RELATED_HEARING_UPDATED_FOR_ADHOC_HEARING, this::handleRelatedHearingUpdatedForAdhocHearing);
    }

    public void handleRelatedHearingUpdatedForAdhocHearing(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received '{}' event with payload {}", PUBLIC_PROGRESSION_RELATED_HEARING_UPDATED_FOR_ADHOC_HEARING, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_CASES_TO_HEARING), envelope.payload()));
    }
}
