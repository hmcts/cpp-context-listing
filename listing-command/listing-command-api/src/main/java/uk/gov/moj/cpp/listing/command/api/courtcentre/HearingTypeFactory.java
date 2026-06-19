package uk.gov.moj.cpp.listing.command.api.courtcentre;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.service.ReferenceDataService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingTypeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingTypeFactory.class);
    private static final String HEARING_TYPES_ARRAY = "hearingTypes";
    private static final String HEARING_TYPE_ID = "id";
    private static final String HEARING_TYPE_DEFAULT_DURATION_MIN = "defaultDurationMin";

    @Inject
    private ReferenceDataService referenceDataService;

    public Map<String, Integer> getHearingTypesIdDurationMap(JsonEnvelope envelope) {
        final JsonEnvelope hearingTypesEnvelope = referenceDataService.getHearingTypes(envelope);
        final JsonObject jsonObject = hearingTypesEnvelope.payloadAsJsonObject();
        final Map<String, Integer> hearingTypesMap = new HashMap<>();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("hearingTypes envelope response: {}", jsonObject);
        }
        final JsonArray hearingTypes = jsonObject.getJsonArray(HEARING_TYPES_ARRAY);
        hearingTypes.getValuesAs(JsonObject.class).forEach(hearingType ->
                hearingTypesMap.put(hearingType.getString(HEARING_TYPE_ID), hearingType.getInt(HEARING_TYPE_DEFAULT_DURATION_MIN)));
        return hearingTypesMap;
    }
}
