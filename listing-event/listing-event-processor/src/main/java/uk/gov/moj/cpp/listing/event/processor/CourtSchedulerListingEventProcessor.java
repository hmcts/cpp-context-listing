package uk.gov.moj.cpp.listing.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:S1188"})
public class CourtSchedulerListingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulerListingEventProcessor.class);

    private static final String PRIVATE_EVENT_AVAILABLE_SLOTS_FOR_HEARING_FREED = "listing.events.available-slots-for-hearing-freed";

    @Inject
    private HearingSlotsService hearingSlotsService;

    @Handles(PRIVATE_EVENT_AVAILABLE_SLOTS_FOR_HEARING_FREED)
    public void freeAvailableHearingSlots(final Envelope<AvailableSlotsForHearingFreed> event) {
        final UUID hearingId = event.payload().getHearingId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.events.available-slots-for-hearing-freed' received on hearingId {}", hearingId);
        }
        hearingSlotsService.delete(hearingId);
    }
}