package uk.gov.moj.cpp.listing.common.progression;

import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import uk.gov.justice.services.messaging.JsonEnvelope;

/**
 * Cache for progression service notes to avoid repeated calls to the progression service.
 * Similar to ReferenceDataCache, this provides caching for case and application notes.
 */
@ApplicationScoped
public class ProgressionNotesCache {

    @Inject
    private ProgressionNotesLoader progressionNotesLoader;

    private final ConcurrentHashMap<UUID, Optional<JsonObject>> caseNotesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Optional<JsonObject>> applicationNotesCache = new ConcurrentHashMap<>();

    /**
     * Gets case notes from cache or loads them if not cached.
     * 
     * @param caseId The case ID to get notes for
     * @param originalRequest The original client request envelope to preserve metadata
     * @return Optional containing the case notes JSON object, or empty if not found
     */
    public Optional<JsonObject> getCaseNotes(UUID caseId, JsonEnvelope originalRequest) {
        return caseNotesCache.computeIfAbsent(caseId, 
            key -> progressionNotesLoader.getCaseNotes(key, originalRequest));
    }

    /**
     * Gets application notes from cache or loads them if not cached.
     * 
     * @param applicationId The application ID to get notes for
     * @param originalRequest The original client request envelope to preserve metadata
     * @return Optional containing the application notes JSON object, or empty if not found
     */
    public Optional<JsonObject> getApplicationNotes(UUID applicationId, JsonEnvelope originalRequest) {
        return applicationNotesCache.computeIfAbsent(applicationId, 
            key -> progressionNotesLoader.getApplicationNotes(key, originalRequest));
    }

    /**
     * Gets notes based on record type. This is a convenience method that calls
     * the appropriate method based on the record type.
     * 
     * @param id The ID (case or application)
     * @param recordType The type of record (CASE or APPLICATION)
     * @param originalRequest The original client request envelope to preserve metadata
     * @return Optional containing the notes JSON object, or empty if not found
     */
    public Optional<JsonObject> getNotes(UUID id, CsvRecordType recordType, JsonEnvelope originalRequest) {
        if (CsvRecordType.APPLICATION.equals(recordType)) {
            return getApplicationNotes(id, originalRequest);
        } else if (CsvRecordType.CASE.equals(recordType)) {
            return getCaseNotes(id, originalRequest);
        } else {
            // For unknown record types, try case notes first, then application notes
            Optional<JsonObject> caseNotes = getCaseNotes(id, originalRequest);
            if (caseNotes.isPresent()) {
                return caseNotes;
            }
            return getApplicationNotes(id, originalRequest);
        }
    }

}
