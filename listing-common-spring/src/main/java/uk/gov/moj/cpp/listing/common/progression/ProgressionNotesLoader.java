package uk.gov.moj.cpp.listing.common.progression;

import lombok.RequiredArgsConstructor;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;


import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import jakarta.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader for progression service notes. Handles the actual calls to the progression service
 * for case and application notes.
 */
@Component
@RequiredArgsConstructor
public class ProgressionNotesLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionNotesLoader.class);

    private static final String PROGRESSION_QUERY_CASE_NOTES = "progression.query.case-notes";
    private static final String PROGRESSION_QUERY_APPLICATION_NOTES = "progression.query.application-notes";

    private final CppQueryClient cppQueryClient;




    /**
     * Loads case notes from the progression service.
     * 
     * @param caseId The case ID to get notes for
     * @param userId the calling user (CJSCPPUID on the progression query)
     * @return Optional containing the case notes JSON object, or empty if not found or error occurs
     */
    public Optional<JsonObject> getCaseNotes(final UUID caseId, final UUID userId) {
        final JsonObject query = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();

        LOGGER.info("Loading case notes for caseId: {}", caseId);

        try {
            // A 404 (no notes) made payloadAsJsonObject() throw on the framework's NULL payload,
            // which the catch below turned into Optional.empty(); the client's empty result is the same.
            final Optional<JsonObject> response = cppQueryClient.request(PROGRESSION_QUERY_CASE_NOTES, query, userId);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Case notes response for caseId {}: {}", caseId, response.isPresent());
            }
            return response;
        } catch (Exception e) {
            LOGGER.warn("Failed to load case notes for caseId: {}", caseId, e);
            return Optional.empty();
        }
    }

    /**
     * Loads application notes from the progression service.
     * 
     * @param applicationId The application ID to get notes for
     * @param userId the calling user (CJSCPPUID on the progression query)
     * @return Optional containing the application notes JSON object, or empty if not found or error occurs
     */
    public Optional<JsonObject> getApplicationNotes(final UUID applicationId, final UUID userId) {
        final JsonObject query = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .build();

        LOGGER.info("Loading application notes for applicationId: {}", applicationId);

        try {
            final Optional<JsonObject> response = cppQueryClient.request(PROGRESSION_QUERY_APPLICATION_NOTES, query, userId);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Application notes response for applicationId {}: {}", applicationId, response.isPresent());
            }
            return response;
        } catch (Exception e) {
            LOGGER.warn("Failed to load application notes for applicationId: {}", applicationId, e);
            return Optional.empty();
        }
    }
}
