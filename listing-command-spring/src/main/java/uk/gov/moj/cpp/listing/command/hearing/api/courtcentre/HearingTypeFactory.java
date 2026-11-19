package uk.gov.moj.cpp.listing.command.hearing.api.courtcentre;

import org.springframework.stereotype.Component;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.api.service.ReferenceDataService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("hearingApiHearingTypeFactory")
public class HearingTypeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingTypeFactory.class);
    private static final String HEARING_TYPES_ARRAY = "hearingTypes";
    private static final String HEARING_TYPE_ID = "id";
    private static final String HEARING_TYPE_DEFAULT_DURATION_MIN = "defaultDurationMin";
    private static final String HEARING_TYPE_TRIAL_FLAG = "trialTypeFlag";

    @Autowired
    private ReferenceDataService referenceDataService;

    public Map<String, Integer> getHearingTypesIdDurationMap(Envelope envelope) {
        final Envelope hearingTypesEnvelope = referenceDataService.getHearingTypes(envelope);
        final JsonObject jsonObject = hearingTypesEnvelope.payload();
        final Map<String, Integer> hearingTypesMap = new HashMap<>();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("hearingTypes envelope response: {}", jsonObject);
        }
        final JsonArray hearingTypes = jsonObject.getJsonArray(HEARING_TYPES_ARRAY);
        hearingTypes.getValuesAs(JsonObject.class).forEach(hearingType ->
                hearingTypesMap.put(hearingType.getString(HEARING_TYPE_ID), hearingType.getInt(HEARING_TYPE_DEFAULT_DURATION_MIN)));
        return hearingTypesMap;
    }

    public Set<String> getTrialHearingTypeIds(final Envelope envelope) {
        final JsonObject jsonObject = referenceDataService.getHearingTypes(envelope).payload();
        final Set<String> trialHearingTypeIds = new HashSet<>();
        jsonObject.getJsonArray(HEARING_TYPES_ARRAY).getValuesAs(JsonObject.class).forEach(hearingType -> {
            if (hearingType.getBoolean(HEARING_TYPE_TRIAL_FLAG, false)) {
                trialHearingTypeIds.add(hearingType.getString(HEARING_TYPE_ID));
            }
        });
        return trialHearingTypeIds;
    }
}
