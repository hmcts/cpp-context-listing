package uk.gov.moj.cpp.listing.event.processor;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.data.SlotDetail;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlotUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlotUpdater.class);
    private static final String CONFIRMED_HEARING = "ConfirmedHearing";

    @Inject
    private SlotsToJsonStringConverter jsonStringConverter;

    @Inject
    private HearingSlotsService hearingSlotsService;


    private SlotUpdater() {
    }

    public Optional<List<SlotDetail>> updateSlot(final JsonEnvelope envelope, final ConfirmedHearing confirmedHearing, final boolean isSlotUpdated, final boolean isForAdjournmentHearing, final List<HearingDay> hearingDays) {

        LOGGER.debug("Processing slot for '{}' with payload {}", CONFIRMED_HEARING, confirmedHearing);

        LOGGER.info("Is update slot service already executed {}", isSlotUpdated);

        if (isTrue(isSlotUpdated)) {
            LOGGER.info("Azure update slot service is applicable only if isSlotUpdated is false. " +
                    "isSlotUpdated = {} ", isSlotUpdated);
        } else {
            return callHearingSlotServiceToUpdate(envelope, confirmedHearing, isForAdjournmentHearing, hearingDays);
        }
        return Optional.empty();
    }

    private Optional<List<SlotDetail>> callHearingSlotServiceToUpdate(final JsonEnvelope envelope, final ConfirmedHearing confirmedHearing, final boolean isForAdjournmentHearing, final List<HearingDay> hearingDays) {
        if (isMagistrates(confirmedHearing)) {
            final List<SlotDetail> slotDetails = jsonStringConverter.getSlotDetailFromHearingConfirmed(envelope, confirmedHearing, isForAdjournmentHearing, hearingDays);
            if (CollectionUtils.isNotEmpty(slotDetails)) {
                final JsonObject updateSlotsPayload = createObjectBuilder()
                        .add("hearingSlots", SlotsToJsonStringConverter.buildJsonArrayBuilder(slotDetails).build())
                        .build();
                LOGGER.info("Calling Azure update slot service with following request {}", updateSlotsPayload);
                hearingSlotsService.update(updateSlotsPayload);
            }
            return Optional.of(slotDetails);
        } else {
            LOGGER.info("Azure update slot service is applicable only when judiciary type is MAGISTRATES. " +
                    "Judiciary type provided is {}", confirmedHearing.getJurisdictionType());
        }
        return Optional.empty();
    }

    private boolean isMagistrates(final ConfirmedHearing confirmedHearing) {
        return MAGISTRATES == confirmedHearing.getJurisdictionType();
    }
}
