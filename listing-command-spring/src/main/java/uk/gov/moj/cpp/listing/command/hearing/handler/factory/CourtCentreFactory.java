package uk.gov.moj.cpp.listing.command.hearing.handler.factory;

import uk.gov.moj.cpp.listing.command.hearing.handler.service.ReferenceDataService;

import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** JEE {@code CourtCentreFactory} of the command handler ({@code referencedata.query.courtroom}, as admin). */
@Component("hearingHandlerCourtCentreFactory")
public class CourtCentreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtCentreFactory.class);

    private final ReferenceDataService referenceDataService;

    public CourtCentreFactory(final ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    public JsonObject getOrganisationUnit(final UUID courtCentreId) {
        final JsonObject jsonObject = referenceDataService.getCourtCentreById(courtCentreId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("courtCentreEnvelope response: {}", jsonObject);
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
