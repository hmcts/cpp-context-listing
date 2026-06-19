package uk.gov.moj.cpp.listing.command.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.service.HearingService;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;

public class HearingFactory {

    @Inject
    private HearingService hearingService;
    
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingFactory.class);

    public Hearing getHearingById(UUID hearingId, JsonEnvelope envelope) {

        final JsonEnvelope hearingEnvelope = hearingService.getHearingById(hearingId, envelope);
        final JsonObject jsonObject = hearingEnvelope.payloadAsJsonObject();
        final Hearing hearing = jsonObjectToObjectConverter.convert(jsonObject, Hearing.class);

        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("hearingEnvelope response: {}", hearingEnvelope.toObfuscatedDebugString());
        }
        return hearing;
    }
}
