package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:S1188"})
public class CourtSchedulerListingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulerListingEventProcessor.class);

    private static final String PRIVATE_EVENT_NON_DEFAULT_DAYS_ASSIGNED = "listing.events.non-default-days-assigned-to-hearing";
    private static final String PRIVATE_EVENT_NON_DEFAULT_DAYS_CHANGED = "listing.events.non-default-days-changed-for-hearing";
    private static final String     PRIVATE_EVENT_AVAILABLE_SLOTS_FOR_HEARING_FREED = "listing.events.available-slots-for-hearing-freed";

    @Inject
    private SlotsToJsonStringConverter jsonStringConverter;

    @Inject
    private HearingSlotsService hearingSlotsService;

    @Handles(PRIVATE_EVENT_NON_DEFAULT_DAYS_ASSIGNED)
    public void nonDefaultDaysAssignedForHearing(final Envelope<NonDefaultDaysAssignedToHearing> event) {
        final NonDefaultDaysAssignedToHearing hearing = event.payload();

        updateHearingSlots(hearing.getHearingId(), hearing.getNonDefaultDays());
    }

    @Handles(PRIVATE_EVENT_NON_DEFAULT_DAYS_CHANGED)
    public void nonDefaultDaysChangedForHearing(final Envelope<NonDefaultDaysChangedForHearing> event) {
        final NonDefaultDaysChangedForHearing hearing = event.payload();

        updateHearingSlots(hearing.getHearingId(), hearing.getNonDefaultDays());
    }

    @Handles(PRIVATE_EVENT_AVAILABLE_SLOTS_FOR_HEARING_FREED)
    public void freeAvailableHearingSlots(final Envelope<AvailableSlotsForHearingFreed> event) {
        final UUID hearingId = event.payload().getHearingId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.events.available-slots-for-hearing-freed' received on hearingId {}", hearingId);
        }
        hearingSlotsService.delete(hearingId);
    }

    private void updateHearingSlots(final UUID hearingId, final List<NonDefaultDay> nonDefaultDays) {
        final List<NonDefaultDay> magsNonDefaultDays = getMagistratesNonDefaultDays(nonDefaultDays);

        if (!magsNonDefaultDays.isEmpty()) {
            final JsonArrayBuilder slotsArray = jsonStringConverter.convertNonDefaultDaysToJson(hearingId, magsNonDefaultDays);
            final JsonObject updateSlotsPayload = createObjectBuilder().add("hearingSlots", slotsArray).build();

            LOGGER.info("Calling hearingSlotsService.update for NonDefaultDays (Mags) with payload {}", updateSlotsPayload);
            hearingSlotsService.update(updateSlotsPayload);
        }
    }

    private List<NonDefaultDay> getMagistratesNonDefaultDays(final List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .filter(ndd -> nonNull(ndd.getCourtScheduleId())|| isSlotDetailPresent(ndd))
                .collect(toList());
    }

    private boolean isSlotDetailPresent(final NonDefaultDay ndd) {
        return nonNull(ndd.getOucode()) && nonNull(ndd.getCourtRoomId()) && nonNull(ndd.getSession());
    }
}