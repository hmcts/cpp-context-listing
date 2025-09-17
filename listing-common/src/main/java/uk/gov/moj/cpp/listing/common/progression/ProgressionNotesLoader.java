package uk.gov.moj.cpp.listing.common.progression;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import uk.gov.justice.services.core.enveloper.Enveloper;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.common.util.UtcClock;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader for progression service notes. Handles the actual calls to the progression service
 * for case and application notes.
 */
@ApplicationScoped
public class ProgressionNotesLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionNotesLoader.class);

    private static final String PROGRESSION_QUERY_CASE_NOTES = "progression.query.case-notes";
    private static final String PROGRESSION_QUERY_APPLICATION_NOTES = "progression.query.application-notes";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private UtcClock utcClock;

    @Inject
    private Enveloper enveloper;

    /**
     * Loads case notes from the progression service.
     * 
     * @param caseId The case ID to get notes for
     * @param originalRequest The original client request envelope to preserve metadata
     * @return Optional containing the case notes JSON object, or empty if not found or error occurs
     */
    public Optional<JsonObject> getCaseNotes(final UUID caseId, final JsonEnvelope originalRequest) {
        final JsonObject query = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();

        LOGGER.info("Loading case notes for caseId: {}", caseId);

        final JsonEnvelope jsonEnvelope = enveloper.withMetadataFrom(originalRequest, PROGRESSION_QUERY_CASE_NOTES).apply(query);

        try {
            final JsonEnvelope response = requester.request(jsonEnvelope);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Case notes response for caseId {}: {}", caseId, response.toObfuscatedDebugString());
            }
            return Optional.of(response.payloadAsJsonObject());
        } catch (Exception e) {
            LOGGER.warn("Failed to load case notes for caseId: {}", caseId, e);
            return Optional.empty();
        }
    }

    /**
     * Loads application notes from the progression service.
     * 
     * @param applicationId The application ID to get notes for
     * @param originalRequest The original client request envelope to preserve metadata
     * @return Optional containing the application notes JSON object, or empty if not found or error occurs
     */
    public Optional<JsonObject> getApplicationNotes(final UUID applicationId, final JsonEnvelope originalRequest) {
        final JsonObject query = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .build();

        LOGGER.info("Loading application notes for applicationId: {}", applicationId);

        final JsonEnvelope jsonEnvelope = enveloper.withMetadataFrom(originalRequest, PROGRESSION_QUERY_APPLICATION_NOTES).apply(query);

        try {
            final JsonEnvelope response = requester.request(jsonEnvelope);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Application notes response for applicationId {}: {}", applicationId, response.toObfuscatedDebugString());
            }
            return Optional.of(response.payloadAsJsonObject());
        } catch (Exception e) {
            LOGGER.warn("Failed to load application notes for applicationId: {}", applicationId, e);
            return Optional.empty();
        }
    }
}
