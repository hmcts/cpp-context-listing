package uk.gov.moj.cpp.listing.command.inbound;

import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Port of the JEE {@code CaseRemovedFromGroupCasesEventProcessor} (MBD refactor P2.4): the payload passed through. */
@Component
public class CaseRemovedFromGroupCasesEventProcessor extends InboundProcessor {

    static final String PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES = "public.progression.case-removed-from-group-cases";
    static final String COMMAND_REMOVE_CASE_FROM_GROUP_CASES = "listing.command.remove-case-from-group-cases";

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseRemovedFromGroupCasesEventProcessor.class);

    private final ProcessorSender sender;

    public CaseRemovedFromGroupCasesEventProcessor(final ProcessorQueue processorQueue, final ProcessorSender sender) {
        super(processorQueue);
        this.sender = sender;
        on(PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES, this::processPublicProgressionCaseRemovedFromGroupCases);
    }

    public void processPublicProgressionCaseRemovedFromGroupCases(final Envelope envelope) {
        LOGGER.info("{} event received with payload {}", PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES, envelope.payload());

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_REMOVE_CASE_FROM_GROUP_CASES),
                envelope.payload()));
    }
}
