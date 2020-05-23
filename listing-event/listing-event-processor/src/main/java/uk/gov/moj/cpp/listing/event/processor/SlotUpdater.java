package uk.gov.moj.cpp.listing.event.processor;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.listing.courts.JurisdictionType.MAGISTRATES;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import javax.inject.Inject;

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

    public void updateSlot(final JsonEnvelope envelope, final ConfirmedHearing confirmedHearing, final boolean isSlotUpdated, final boolean isForAdjournmentHearing) {

        LOGGER.debug("Processing slot for '{}' with payload {}", CONFIRMED_HEARING, confirmedHearing);

        LOGGER.info("Is update slot service already executed {}", isSlotUpdated);

        if (isTrue(isSlotUpdated)) {
            LOGGER.info("Azure update slot service is applicable only if isSlotUpdated is false. " +
                    "isSlotUpdated = {} ", isSlotUpdated);
        } else {
            callHearingSlotServiceToUpdate(envelope, confirmedHearing, isForAdjournmentHearing);
        }
    }

    private void callHearingSlotServiceToUpdate(final JsonEnvelope envelope, final ConfirmedHearing confirmedHearing, final boolean isForAdjournmentHearing) {
        if (isMagistrates(confirmedHearing)) {
            final String updateSlotsPayload = jsonStringConverter.getSlotDetailFromHearingConfirmed(envelope, confirmedHearing, isForAdjournmentHearing);

            LOGGER.info("Calling Azure update slot service with following request {}", updateSlotsPayload);

            if (isNotEmpty(updateSlotsPayload)) {
                hearingSlotsService.update(updateSlotsPayload);
            }
        } else {
            LOGGER.info("Azure update slot service is applicable only when judiciary type is MAGISTRATES. " +
                    "Judiciary type provided is {}", confirmedHearing.getJurisdictionType());
        }
    }

    private boolean isMagistrates(final ConfirmedHearing confirmedHearing) {
        return MAGISTRATES == confirmedHearing.getJurisdictionType();
    }
}
