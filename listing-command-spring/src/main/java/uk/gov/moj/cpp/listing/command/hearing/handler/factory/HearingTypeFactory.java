package uk.gov.moj.cpp.listing.command.hearing.handler.factory;

import uk.gov.moj.cpp.listing.command.hearing.handler.service.ReferenceDataService;

import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** JEE {@code HearingTypeFactory} of the command handler ({@code referencedata.query.all-hearing-types}, as admin). */
@Component("hearingHandlerHearingTypeFactory")
public class HearingTypeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingTypeFactory.class);
    private static final String HEARING_TYPES_ARRAY = "hearingTypes";
    private static final String HEARING_TYPE_ID = "id";
    private static final String HEARING_TYPE_DEFAULT_DURATION_MIN = "defaultDurationMin";

    private final ReferenceDataService referenceDataService;

    public HearingTypeFactory(final ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    public Map<String, Integer> getHearingTypesIdDurationMap() {
        final JsonObject jsonObject = referenceDataService.getHearingTypes();
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
