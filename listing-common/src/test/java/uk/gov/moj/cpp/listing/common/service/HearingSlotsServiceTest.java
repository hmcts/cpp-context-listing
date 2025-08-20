package uk.gov.moj.cpp.listing.common.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingSlotsServiceTest {

    private static final String BASE_URI = "http://test-uri";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_HEARING_ID = UUID.randomUUID();

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Mock
    private HttpClientBuilder httpClientBuilder;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private StatusLine statusLine;
    @Captor
    private ArgumentCaptor<HttpGet> httpGetCaptor;
    @Captor
    private ArgumentCaptor<HttpPut> httpPutCaptor;
    @Captor
    private ArgumentCaptor<HttpDelete> httpDeleteCaptor;

    @InjectMocks
    private HearingSlotsService hearingSlotsService;

    @BeforeEach
    void setUp() {
        hearingSlotsService.baseUri = BASE_URI;
    }

    @Test
    public void shouldSearchSuccessfully() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            // When
            Response response = hearingSlotsService.search(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + "/hearingslots?key=value"));
        }
    }

    @Test
    public void shouldDeleteSuccessfully() throws Exception {
        // Given
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpDelete.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());

            // When
            hearingSlotsService.delete(TEST_HEARING_ID);

            // Then
            verify(httpClient).execute(httpDeleteCaptor.capture());
            HttpDelete capturedDelete = httpDeleteCaptor.getValue();
            assertThat(capturedDelete.getURI().toString(), is(BASE_URI + "/hearingslots/" + TEST_HEARING_ID));
        }
    }

    @Test
    public void shouldGetCourtSchedulerHearingIdsSuccessfully() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            // When
            Response response = hearingSlotsService.getCourtSchedulerHearingIds(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + "/hearingslots?key=value"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenSearchParamsAreNull() {
        // Given
        Map<String, String> params = null;

        // When/Then
        try {
            hearingSlotsService.search(params);
        } catch (DataValidationException e) {
            assertThat(e.getMessage(), is("Params for search application/vnd.courtscheduler.get.hearing.slots+json is null ...."));
        }
    }

    @Test
    public void shouldHandleHttpErrorResponse() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.BAD_REQUEST.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));

            // When
            Response response = hearingSlotsService.search(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + "/hearingslots?key=value"));
        }
    }

    @Test
    public void shouldHandleIOException() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenThrow(new IOException("Test exception"));

            // When
            Response response = hearingSlotsService.search(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void shouldGetCourtSchedulesByIdSuccessfully() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            // When
            Response response = hearingSlotsService.getCourtSchedulesById(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + "/courtschedule/search.court-schedules-by-id?key=value"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenGetCourtSchedulesByIdParamsAreNull() {
        // Given
        Map<String, String> params = null;

        // When/Then
        try {
            hearingSlotsService.getCourtSchedulesById(params);
        } catch (DataValidationException e) {
            assertThat(e.getMessage(), is("Params for search application/vnd.courtscheduler.search.courtschedules.by.id+json is null ...."));
        }
    }

    @Test
    public void shouldHandleGetCourtSchedulesByIdError() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.BAD_REQUEST.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));

            // When
            Response response = hearingSlotsService.getCourtSchedulesById(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void shouldHandleDeleteError() throws Exception {
        // Given
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpDelete.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.BAD_REQUEST.getStatusCode());

            // When
            hearingSlotsService.delete(TEST_HEARING_ID);

            // Then
            verify(httpClient).execute(httpDeleteCaptor.capture());
            HttpDelete capturedDelete = httpDeleteCaptor.getValue();
            assertThat(capturedDelete.getURI().toString(), is(BASE_URI + "/hearingslots/" + TEST_HEARING_ID));
        }
    }

    @Test
    public void shouldHandleDeleteIOException() throws Exception {
        // Given
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpDelete.class))).thenThrow(new IOException("Test exception"));

            // When
            hearingSlotsService.delete(TEST_HEARING_ID);

            // Then
            verify(httpClient).execute(httpDeleteCaptor.capture());
            HttpDelete capturedDelete = httpDeleteCaptor.getValue();
            assertThat(capturedDelete.getURI().toString(), is(BASE_URI + "/hearingslots/" + TEST_HEARING_ID));
        }
    }

    @Test
    public void shouldHandleResponseEntityConversionError() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class);
             MockedStatic<EntityUtils> entityUtilsMockedStatic = Mockito.mockStatic(EntityUtils.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());
            org.apache.http.HttpEntity entity = mock(org.apache.http.HttpEntity.class);
            when(httpResponse.getEntity()).thenReturn(entity);
            entityUtilsMockedStatic.when(() -> EntityUtils.toString(entity)).thenReturn("test response");
            when(stringToJsonObjectConverter.convert(any())).thenThrow(new RuntimeException("Conversion error"));

            // When/Then
            try {
                hearingSlotsService.search(params);
            } catch (RuntimeException e) {
                assertThat(e.getMessage(), is("Conversion error"));
            }
        }
    }

    @Test
    public void shouldHandleMultipleQueryParameters() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            // When
            Response response = hearingSlotsService.search(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + "/hearingslots?key1=value1&key2=value2"));
        }
    }

    @Test
    public void shouldHandleSystemUserProviderError() {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.empty());

        // When/Then
        try {
            hearingSlotsService.search(params);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("contextSystemUserId missing!!!"));
        }
    }


    @Test
    public void shouldSearchAndBookSlotsSuccessfully() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            // When
            Response response = hearingSlotsService.searchBookSlots(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + "/searchlist/hearingslots?key=value"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenSearchAndBookParamsAreNull() {
        // Given
        Map<String, String> params = null;

        // When/Then
        try {
            hearingSlotsService.searchBookSlots(params);
        } catch (DataValidationException e) {
            assertThat(e.getMessage(), is("Params for search application/vnd.courtscheduler.search.book.hearing.slots+json is null ...."));
        }
    }
}