package uk.gov.moj.cpp.listing.command.api.courtcentre;

import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.convertHoursAndMinutesToMinutes;

import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.service.ReferenceDataService;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtCentreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtCentreFactory.class);

    @Inject
    private ReferenceDataService referenceDataService;

    public Map<UUID, CourtCentreDetails> getCourtCentreDetailsById(Set<UUID> courtCenterIdList, JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentresById(courtCenterIdList, envelope);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("courtCentreEnvelope response: {}", courtCentreEnvelope.toObfuscatedDebugString());
        }
        final Map<UUID, CourtCentreDetails> courtCentreDetailsMap = new HashMap<>();
        final JsonArray respJsonArray = courtCentreEnvelope.payloadAsJsonObject().getJsonArray("organisationunits");
        respJsonArray.forEach(e-> {
            final JsonObject respJsonObj = (JsonObject) e;
            final CourtCentreDetails courtCentreDetails = createCourtCentreDetailsOf(fromString(respJsonObj.getString("id")), respJsonObj);
            courtCentreDetailsMap.put(courtCentreDetails.getId(), courtCentreDetails);
        });

        return courtCentreDetailsMap;
    }

    public CourtCentreDetails getCourtCentre(UUID courtCentreId, JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentreById(courtCentreId, envelope);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("courtCentreEnvelope response: {}", courtCentreEnvelope.toObfuscatedDebugString());
        }
        final JsonObject respJsonObj = courtCentreEnvelope.payloadAsJsonObject();
        return createCourtCentreDetailsOf(courtCentreId, respJsonObj);
    }

    private CourtCentreDetails createCourtCentreDetailsOf(UUID courtCentreId, JsonObject jsonObject) {
        final String defaultDurationHoursMins = getJsonObjectString("defaultDurationHrs", jsonObject, courtCentreId);
        final Integer defaultDurationMinutes =
                convertHoursAndMinutesToMinutes(defaultDurationHoursMins)
                        .orElse(0);

        final String defaultStartTimeStr = getJsonObjectString("defaultStartTime", jsonObject, courtCentreId);
        final LocalTime defaultStartTime = LocalTime.parse(defaultStartTimeStr);
        final String name = getJsonObjectString("oucodeL3Name", jsonObject, courtCentreId);

        return CourtCentreDetails.courtCentreDetails()
                .withDefaultStartTime(defaultStartTime)
                .withDefaultDuration(defaultDurationMinutes)
                .withId(courtCentreId)
                .withName(name)
                .build();
    }

    private String getJsonObjectString(String searchString, JsonObject jsonObject, UUID courtCentreId) {
        final String jsonObjectStringValue = jsonObject.getString(searchString);
        if (jsonObjectStringValue == null || jsonObjectStringValue.isEmpty()) {
            throw new IllegalArgumentException(searchString + " not found for courtCentreId:" + courtCentreId);
        }
        return jsonObjectStringValue;
    }
}
