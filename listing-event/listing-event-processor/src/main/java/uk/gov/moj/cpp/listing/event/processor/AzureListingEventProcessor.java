package uk.gov.moj.cpp.listing.event.processor;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class AzureListingEventProcessor {

    private static final String PRIVATE_EVENT_NON_DEFAULT_DAYS_ASSIGNED = "listing.events.non-default-days-assigned-to-hearing";
    private static final String PRIVATE_EVENT_NON_DEFAULT_DAYS_CHANGED = "listing.events.non-default-days-changed-for-hearing";

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

    private void updateHearingSlots(final UUID hearingId, final List<NonDefaultDay> nonDefaultDays) {
        final List<NonDefaultDay> magsNonDefaultDays = getMagistratesNonDefaultDays(nonDefaultDays);

        if (!magsNonDefaultDays.isEmpty()) {
            final String updateSlotsPayload = jsonStringConverter.convertNonDefaultDaysToJson(hearingId, magsNonDefaultDays);

            hearingSlotsService.update(updateSlotsPayload);
        }
    }

    private List<NonDefaultDay> getMagistratesNonDefaultDays(final List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .filter(ndd -> ndd.getCourtScheduleId().isPresent())
                .collect(toList());
    }
}