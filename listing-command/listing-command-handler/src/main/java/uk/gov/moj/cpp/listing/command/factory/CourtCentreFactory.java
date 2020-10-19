package uk.gov.moj.cpp.listing.command.factory;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtCentreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtCentreFactory.class);

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ReferenceDataCache referenceDataCache;

    public JsonObject getOrganisationUnit(final UUID courtCentreId, final JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentreById(courtCentreId, envelope);
        final JsonObject jsonObject = courtCentreEnvelope.payloadAsJsonObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("courtCentreEnvelope response: {}", courtCentreEnvelope.toObfuscatedDebugString());
        }
        return jsonObject;
    }

    public Optional<Integer> getCourtRoomNumber(final JsonObject courtCentreObj, final String s) {
        return courtCentreObj.getJsonArray("courtrooms")
                .getValuesAs(JsonObject.class).stream()
                .filter(courtRoom -> s.equals(courtRoom.getString("id")))
                .map(courtRoom -> courtRoom.getInt("courtroomId"))
                .map(Optional::of)
                .findFirst().orElse(Optional.empty());
    }
}
