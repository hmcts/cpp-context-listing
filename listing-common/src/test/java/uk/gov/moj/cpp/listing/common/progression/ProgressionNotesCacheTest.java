package uk.gov.moj.cpp.listing.common.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for ProgressionNotesCache
 */
@ExtendWith(MockitoExtension.class)
class ProgressionNotesCacheTest {

    @Mock
    private ProgressionNotesLoader progressionNotesLoader;

    @InjectMocks
    private ProgressionNotesCache progressionNotesCache;

    private UUID caseId;
    private UUID applicationId;
    private JsonObject caseNotes;
    private JsonObject applicationNotes;
    private JsonEnvelope originalRequest;

    @BeforeEach
    void setUp() {
        caseId = randomUUID();
        applicationId = randomUUID();
        
        caseNotes = Json.createObjectBuilder()
                .add("caseNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Test case note").add("isPinned", true))
                )
                .build();
        
        applicationNotes = Json.createObjectBuilder()
                .add("applicationNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Test application note").add("isPinned", false))
                )
                .build();
        
        originalRequest = org.mockito.Mockito.mock(JsonEnvelope.class);
    }

    @Test
    void shouldGetCaseNotesFromCache() {
        // Given
        when(progressionNotesLoader.getCaseNotes(caseId, originalRequest)).thenReturn(Optional.of(caseNotes));

        // When
        Optional<JsonObject> result1 = progressionNotesCache.getCaseNotes(caseId, originalRequest);
        Optional<JsonObject> result2 = progressionNotesCache.getCaseNotes(caseId, originalRequest);

        // Then
        assertThat(result1.isPresent(), is(true));
        assertThat(result1.get(), is(caseNotes));
        assertThat(result2.isPresent(), is(true));
        assertThat(result2.get(), is(caseNotes));
        
        // Verify loader was called only once due to caching
        verify(progressionNotesLoader).getCaseNotes(caseId, originalRequest);
    }

    @Test
    void shouldGetApplicationNotesFromCache() {
        // Given
        when(progressionNotesLoader.getApplicationNotes(applicationId, originalRequest)).thenReturn(Optional.of(applicationNotes));

        // When
        Optional<JsonObject> result1 = progressionNotesCache.getApplicationNotes(applicationId, originalRequest);
        Optional<JsonObject> result2 = progressionNotesCache.getApplicationNotes(applicationId, originalRequest);

        // Then
        assertThat(result1.isPresent(), is(true));
        assertThat(result1.get(), is(applicationNotes));
        assertThat(result2.isPresent(), is(true));
        assertThat(result2.get(), is(applicationNotes));
        
        // Verify loader was called only once due to caching
        verify(progressionNotesLoader).getApplicationNotes(applicationId, originalRequest);
    }

    @Test
    void shouldGetNotesBasedOnRecordType() {
        // Given
        when(progressionNotesLoader.getCaseNotes(caseId, originalRequest)).thenReturn(Optional.of(caseNotes));
        when(progressionNotesLoader.getApplicationNotes(applicationId, originalRequest)).thenReturn(Optional.of(applicationNotes));

        // When
        Optional<JsonObject> caseResult = progressionNotesCache.getNotes(caseId, CsvRecordType.CASE, originalRequest);
        Optional<JsonObject> applicationResult = progressionNotesCache.getNotes(applicationId, CsvRecordType.APPLICATION, originalRequest);

        // Then
        assertThat(caseResult.isPresent(), is(true));
        assertThat(caseResult.get(), is(caseNotes));
        assertThat(applicationResult.isPresent(), is(true));
        assertThat(applicationResult.get(), is(applicationNotes));
        
        verify(progressionNotesLoader).getCaseNotes(caseId, originalRequest);
        verify(progressionNotesLoader).getApplicationNotes(applicationId, originalRequest);
    }

    @Test
    void shouldHandleUnknownRecordTypeByTryingCaseNotesFirst() {
        // Given
        when(progressionNotesLoader.getCaseNotes(caseId, originalRequest)).thenReturn(Optional.of(caseNotes));

        // When
        Optional<JsonObject> result = progressionNotesCache.getNotes(caseId, null, originalRequest);

        // Then
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(caseNotes));
        
        verify(progressionNotesLoader).getCaseNotes(caseId, originalRequest);
    }

    @Test
    void shouldHandleUnknownRecordTypeByTryingApplicationNotesWhenCaseNotesEmpty() {
        // Given
        when(progressionNotesLoader.getCaseNotes(caseId, originalRequest)).thenReturn(Optional.empty());
        when(progressionNotesLoader.getApplicationNotes(caseId, originalRequest)).thenReturn(Optional.of(applicationNotes));

        // When
        Optional<JsonObject> result = progressionNotesCache.getNotes(caseId, null, originalRequest);

        // Then
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(applicationNotes));
        
        verify(progressionNotesLoader).getCaseNotes(caseId, originalRequest);
        verify(progressionNotesLoader).getApplicationNotes(caseId, originalRequest);
    }

    @Test
    void shouldReturnEmptyWhenLoaderReturnsEmpty() {
        // Given
        when(progressionNotesLoader.getCaseNotes(caseId, originalRequest)).thenReturn(Optional.empty());

        // When
        Optional<JsonObject> result = progressionNotesCache.getCaseNotes(caseId, originalRequest);

        // Then
        assertThat(result.isPresent(), is(false));
        verify(progressionNotesLoader).getCaseNotes(caseId, originalRequest);
    }
}
