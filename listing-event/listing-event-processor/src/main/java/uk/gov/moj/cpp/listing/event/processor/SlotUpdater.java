package uk.gov.moj.cpp.listing.event.processor;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.listing.courts.JurisdictionType.MAGISTRATES;

import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlotUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlotUpdater.class);
    private static final String HEARING_CONFIRMED = "HearingConfirmed";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private SlotsToJsonStringConverter jsonStringConverter;

    @Inject
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Inject
    private HearingSlotsService hearingSlotsService;


    private SlotUpdater() {
    }

    public void updateSlot(final JsonEnvelope envelope) {
        final HearingAllocatedForListing hearingAllocatedForListing = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingAllocatedForListing.class);
        final HearingConfirmed hearingConfirmed = getHearingConfirmed(hearingAllocatedForListing, envelope);

        LOGGER.info("Processing slot for '{}' with payload {}", HEARING_CONFIRMED, hearingConfirmed);

        if (isMagistratesAndSlotNotAlreadyUpdated(hearingAllocatedForListing, hearingConfirmed)) {
            final String updateSlotsPayload = jsonStringConverter.getSlotDetailFromHearingConfirmed(envelope, hearingConfirmed);

            if (isNotEmpty(updateSlotsPayload)) {
                hearingSlotsService.update(updateSlotsPayload);
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(format("Azure update slot service is applicable only to judiciary type MAGISTRATES. Judiciary type provided is %s.", hearingConfirmed.getConfirmedHearing().getJurisdictionType()));
            }
        }
    }

    private boolean isMagistratesAndSlotNotAlreadyUpdated(final HearingAllocatedForListing hearingAllocatedForListing, final HearingConfirmed hearingConfirmed) {
        return MAGISTRATES == hearingConfirmed.getConfirmedHearing().getJurisdictionType() && !checkIfUpdateSlot(hearingAllocatedForListing);
    }

    @SuppressWarnings({"squid:S3655"})
    private boolean checkIfUpdateSlot(final HearingAllocatedForListing hearingAllocatedForListing) {
        if (hearingAllocatedForListing.getUpdateSlot().isPresent()) {
            final boolean isUpdateSlot = hearingAllocatedForListing.getUpdateSlot().get();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(format("is update slot service already executed = %s", isUpdateSlot));
            }
            return isUpdateSlot;
        }

        return false;
    }

    private HearingConfirmed getHearingConfirmed(final HearingAllocatedForListing hearingAllocatedForListing, final JsonEnvelope envelope) {
        return hearingConfirmedFactory.create(hearingAllocatedForListing, envelope);
    }
}