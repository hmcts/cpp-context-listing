package uk.gov.moj.cpp.listing.command.hearing.processor;


import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import org.springframework.stereotype.Component;
import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of the JEE {@code CourtSchedulerListingEventProcessor} (MBD refactor P2.3.2, ADR 015): on
 * {@code available-slots-for-hearing-freed}, release the hearing's sessions in courtscheduler
 * ({@code DELETE /sessions/{hearingId}}).
 *
 * <p>This runs in the command's transaction (processor queue), but it is an external write: a
 * failure is logged at ERROR and never fails the command. {@code HearingSlotsService.delete}
 * already logs a non-202 answer or a transport error without throwing (as the JEE service did); any
 * other runtime failure is caught here too. In JEE the step ran after commit and the outbox relay
 * retried it then dropped it, without affecting the command either. Difference: if the command
 * transaction later rolls back, the sessions have already been released.
 */
@Component
@SuppressWarnings({"squid:S1188"})
public class CourtSchedulerListingEventProcessor implements ProcessorPort {

    @Override
    public void register(final EventHandlers processors) {
        processors
                .on(uk.gov.justice.listing.events.AvailableSlotsForHearingFreed.class, this::freeAvailableHearingSlots);
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulerListingEventProcessor.class);

    private static final String PRIVATE_EVENT_AVAILABLE_SLOTS_FOR_HEARING_FREED = "listing.events.available-slots-for-hearing-freed";

    @Autowired
    private HearingSlotsService hearingSlotsService;

    public void freeAvailableHearingSlots(final RaisedEvent<AvailableSlotsForHearingFreed> event) {
        final UUID hearingId = event.payload().getHearingId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.events.available-slots-for-hearing-freed' received on hearingId {}", hearingId);
        }
        try {
            hearingSlotsService.delete(hearingId);
        } catch (final RuntimeException e) {
            LOGGER.error("Could not release the courtscheduler sessions of hearing {}; the command goes ahead", hearingId, e);
        }
    }
}