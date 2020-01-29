package uk.gov.moj.cpp.listing.event.processor;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.listing.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class AzureListingEventProcessor {

    private static final String PUBLIC_EVENT_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PRIVATE_EVENT_NON_DEFAULT_DAYS_ASSIGNED = "listing.events.non-default-days-assigned-to-hearing";
    private static final String PRIVATE_EVENT_NON_DEFAULT_DAYS_CHANGED = "listing.events.non-default-days-changed-for-hearing";

    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventProcessor.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

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

    @Handles(PUBLIC_EVENT_HEARING_CONFIRMED)
    public void handleHearingConfirmedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_HEARING_CONFIRMED, envelope.toObfuscatedDebugString());
        }

        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class);

        if(isMagistratesAndSlotNotAlreadyUpdated(hearingConfirmed)){
            final String updateSlotsPayload = jsonStringConverter.getSlotDetailFromHearingConfirmed(envelope, hearingConfirmed);

            if (isNotEmpty(updateSlotsPayload)) {
                hearingSlotsService.update(updateSlotsPayload);
            }
        }
        else{
            if (LOGGER.isInfoEnabled()){
                LOGGER.info(format("Azure update slot service is applicable only to judiciary type MAGISTRATES. Judiciary type provided is %s.", hearingConfirmed.getConfirmedHearing().getJurisdictionType()));
            }
        }
    }

    private boolean isMagistratesAndSlotNotAlreadyUpdated(final HearingConfirmed hearingConfirmed) {
        return MAGISTRATES==hearingConfirmed.getConfirmedHearing().getJurisdictionType();
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
