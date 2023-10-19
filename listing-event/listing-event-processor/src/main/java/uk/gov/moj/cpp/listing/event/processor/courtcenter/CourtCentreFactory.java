package uk.gov.moj.cpp.listing.event.processor.courtcenter;

import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.convertHoursAndMinutesToMinutes;


import java.time.LocalTime;
import java.util.UUID;
import javax.inject.Inject;
import javax.json.JsonObject;
import org.slf4j.Logger;

import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.service.ReferenceDataService;

@SuppressWarnings("squid:S1312")
public class CourtCentreFactory {
    @Inject
    private Logger logger;

    @Inject
    private ReferenceDataService referenceDataService;

    public CourtCentreDetails getCourtCentre(UUID courtCentreId, JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentreById(courtCentreId, envelope);
        final JsonObject jsonObject = courtCentreEnvelope.payloadAsJsonObject();
        if (logger.isInfoEnabled()) {
            logger.info("courtCentreEnvelope response: {}", courtCentreEnvelope.toObfuscatedDebugString());
        }

        final String defaultDurationHoursMins = getJsonObjectString("defaultDurationHrs", jsonObject, courtCentreId);
        final Integer defaultDurationMinutes =
                convertHoursAndMinutesToMinutes(defaultDurationHoursMins)
                        .orElse(0);

        final String defaultStartTimeStr = getJsonObjectString("defaultStartTime", jsonObject, courtCentreId);
        final LocalTime defaultStartTime = LocalTime.parse(defaultStartTimeStr);


        return CourtCentreDetails.courtCentreDetails()
                .withDefaultStartTime(defaultStartTime)
                .withDefaultDuration(defaultDurationMinutes)
                .withId(courtCentreId)
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
