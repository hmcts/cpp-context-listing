package uk.gov.moj.cpp.listing.common.progression;

import lombok.RequiredArgsConstructor;

import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import jakarta.json.JsonObject;


/**
 * Cache for progression service notes to avoid repeated calls to the progression service.
 * Similar to ReferenceDataCache, this provides caching for case and application notes.
 */
@Component
@RequiredArgsConstructor
public class ProgressionNotesCache {

    private final ProgressionNotesLoader progressionNotesLoader;

    private final ConcurrentHashMap<UUID, Optional<JsonObject>> caseNotesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Optional<JsonObject>> applicationNotesCache = new ConcurrentHashMap<>();

    /**
     * Gets case notes from cache or loads them if not cached.
     * 
     * @param caseId The case ID to get notes for
     * @param userId the calling user (CJSCPPUID on the progression query)
     * @return Optional containing the case notes JSON object, or empty if not found
     */
    public Optional<JsonObject> getCaseNotes(UUID caseId, UUID userId) {
        return caseNotesCache.computeIfAbsent(caseId, 
            key -> progressionNotesLoader.getCaseNotes(key, userId));
    }

    /**
     * Gets application notes from cache or loads them if not cached.
     * 
     * @param applicationId The application ID to get notes for
     * @param userId the calling user (CJSCPPUID on the progression query)
     * @return Optional containing the application notes JSON object, or empty if not found
     */
    public Optional<JsonObject> getApplicationNotes(UUID applicationId, UUID userId) {
        return applicationNotesCache.computeIfAbsent(applicationId, 
            key -> progressionNotesLoader.getApplicationNotes(key, userId));
    }

    /**
     * Gets notes based on record type. This is a convenience method that calls
     * the appropriate method based on the record type.
     * 
     * @param id The ID (case or application)
     * @param recordType The type of record (CASE or APPLICATION)
     * @param userId the calling user (CJSCPPUID on the progression query)
     * @return Optional containing the notes JSON object, or empty if not found
     */
    public Optional<JsonObject> getNotes(UUID id, CsvRecordType recordType, UUID userId) {
        if (CsvRecordType.APPLICATION.equals(recordType)) {
            return getApplicationNotes(id, userId);
        } else if (CsvRecordType.CASE.equals(recordType)) {
            return getCaseNotes(id, userId);
        } else {
            // For unknown record types, try case notes first, then application notes
            Optional<JsonObject> caseNotes = getCaseNotes(id, userId);
            if (caseNotes.isPresent()) {
                return caseNotes;
            }
            return getApplicationNotes(id, userId);
        }
    }

}
