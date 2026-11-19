package uk.gov.moj.cpp.listing.command.hearing.handler.service;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import java.util.UUID;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The command handler's reference-data queries (MBD refactor P2.3.2): JEE {@code ReferenceDataService}
 * of listing-command-handler. Both were {@code requestAsAdmin} (system user, D19).
 */
@Component("hearingHandlerReferenceDataService")
public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    static final String REFERENCEDATA_QUERY_ALL_HEARING_TYPES = "referencedata.query.all-hearing-types";
    static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";

    private final CppQueryClient cppQueryClient;

    public ReferenceDataService(final CppQueryClient cppQueryClient) {
        this.cppQueryClient = cppQueryClient;
    }

    public JsonObject getHearingTypes() {
        LOGGER.info("'referencedata.query.hearing-types' request");
        return cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_ALL_HEARING_TYPES, createObjectBuilder().build())
                .orElseThrow(() -> new IllegalStateException("No response from " + REFERENCEDATA_QUERY_ALL_HEARING_TYPES));
    }

    public JsonObject getCourtCentreById(final UUID courtCentreId) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.info("'referencedata.query.courtroom' request with payload {}", payload);
        return cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_COURTROOM, payload)
                .orElseThrow(() -> new IllegalStateException("No response from " + REFERENCEDATA_QUERY_COURTROOM));
    }
}
