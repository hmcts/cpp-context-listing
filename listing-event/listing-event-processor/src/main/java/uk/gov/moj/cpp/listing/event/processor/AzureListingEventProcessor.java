package uk.gov.moj.cpp.listing.event.processor;

import static java.net.URLEncoder.encode;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.api.resource.DefaultQueryApiUpdateHearingSlotsResource;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotCriteriaConverter;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class AzureListingEventProcessor {

    private static final String PUBLIC_EVENT_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventProcessor.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Inject
    private SlotCriteriaConverter slotCriteriaConverter;

    @Inject
    private DefaultQueryApiUpdateHearingSlotsResource defaultQueryApiUpdateHearingSlotsResource;


    @Handles(PUBLIC_EVENT_HEARING_CONFIRMED)
    public void handleHearingConfirmedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_HEARING_CONFIRMED, envelope.toObfuscatedDebugString());
        }

        //update slots in Azure with hearing confirmation details
        try {
            updateSlotSchedule(envelope);
        } catch (UnsupportedEncodingException e) {
            LOGGER.info("Unsupported Encoding Exception", e);
        }
    }

    private void updateSlotSchedule(final JsonEnvelope envelope) throws UnsupportedEncodingException {
        final HearingConfirmed hearingConfirmed = getHearingConfirmed(envelope);

        final String slotDetailFromHearingConfirmed = slotCriteriaConverter.getSlotDetailFromHearingConfirmed(envelope, hearingConfirmed);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Update slot in Azure scheduling with slot details '{}'", slotDetailFromHearingConfirmed);
        }

        if(isNotEmpty(slotDetailFromHearingConfirmed)){
            defaultQueryApiUpdateHearingSlotsResource.updateHearingSlots(encode(slotDetailFromHearingConfirmed, "UTF-8"));
        }
    }

    private HearingConfirmed getHearingConfirmed(final JsonEnvelope envelope) {
        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class);
        return hearingConfirmed;
    }
}
