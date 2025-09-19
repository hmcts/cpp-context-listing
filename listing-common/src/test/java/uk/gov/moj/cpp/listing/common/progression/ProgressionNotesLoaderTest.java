package uk.gov.moj.cpp.listing.common.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for ProgressionNotesLoader
 */
@ExtendWith(MockitoExtension.class)
class ProgressionNotesLoaderTest {

    @Mock
    private Requester requester;

    @Mock
    private UtcClock utcClock;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private ProgressionNotesLoader progressionNotesLoader;

    private UUID caseId;
    private UUID applicationId;
    private ZonedDateTime now;
    private JsonObject caseNotesResponse;
    private JsonObject applicationNotesResponse;
    private JsonEnvelope originalRequest;

    @BeforeEach
    void setUp() {
        caseId = randomUUID();
        applicationId = randomUUID();
        now = ZonedDateTime.now();
        
        caseNotesResponse = Json.createObjectBuilder()
                .add("caseNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Test case note").add("isPinned", true))
                )
                .build();
        
        applicationNotesResponse = Json.createObjectBuilder()
                .add("applicationNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Test application note").add("isPinned", false))
                )
                .build();
        
        originalRequest = createMockRequest();
    }

    @Test
    void shouldLoadCaseNotesSuccessfully() {
        // Given
        JsonEnvelope mockRequestEnvelope = org.mockito.Mockito.mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class))).thenReturn(payload -> mockRequestEnvelope);
        
        JsonEnvelope mockResponse = createMockResponse(caseNotesResponse);
        when(requester.request(any(JsonEnvelope.class))).thenReturn(mockResponse);

        // When
        Optional<JsonObject> result = progressionNotesLoader.getCaseNotes(caseId, originalRequest);

        // Then
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(caseNotesResponse));
        
        verify(enveloper).withMetadataFrom(originalRequest, "progression.query.case-notes");
        verify(requester).request(any(JsonEnvelope.class));
    }

    @Test
    void shouldLoadApplicationNotesSuccessfully() {
        // Given
        JsonEnvelope mockRequestEnvelope = org.mockito.Mockito.mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class))).thenReturn(payload -> mockRequestEnvelope);
        
        JsonEnvelope mockResponse = createMockResponse(applicationNotesResponse);
        when(requester.request(any(JsonEnvelope.class))).thenReturn(mockResponse);

        // When
        Optional<JsonObject> result = progressionNotesLoader.getApplicationNotes(applicationId, originalRequest);

        // Then
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(applicationNotesResponse));
        
        verify(enveloper).withMetadataFrom(originalRequest, "progression.query.application-notes");
        verify(requester).request(any(JsonEnvelope.class));
    }

    @Test
    void shouldReturnEmptyWhenRequesterThrowsException() {
        // Given
        JsonEnvelope mockRequestEnvelope = org.mockito.Mockito.mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class))).thenReturn(payload -> mockRequestEnvelope);
        
        when(requester.request(any(JsonEnvelope.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        Optional<JsonObject> result = progressionNotesLoader.getCaseNotes(caseId, originalRequest);

        // Then
        assertThat(result.isPresent(), is(false));
        
        verify(enveloper).withMetadataFrom(originalRequest, "progression.query.case-notes");
        verify(requester).request(any(JsonEnvelope.class));
    }

    @Test
    void shouldReturnEmptyWhenApplicationNotesRequesterThrowsException() {
        // Given
        JsonEnvelope mockRequestEnvelope = org.mockito.Mockito.mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class))).thenReturn(payload -> mockRequestEnvelope);
        
        when(requester.request(any(JsonEnvelope.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        Optional<JsonObject> result = progressionNotesLoader.getApplicationNotes(applicationId, originalRequest);

        // Then
        assertThat(result.isPresent(), is(false));
        
        verify(enveloper).withMetadataFrom(originalRequest, "progression.query.application-notes");
        verify(requester).request(any(JsonEnvelope.class));
    }

    private JsonEnvelope createMockRequest() {
        JsonEnvelope mockEnvelope = org.mockito.Mockito.mock(JsonEnvelope.class);
        return mockEnvelope;
    }

    private JsonEnvelope createMockResponse(JsonObject payload) {
        JsonEnvelope mockEnvelope = org.mockito.Mockito.mock(JsonEnvelope.class);
        when(mockEnvelope.payloadAsJsonObject()).thenReturn(payload);
        return mockEnvelope;
    }
}
