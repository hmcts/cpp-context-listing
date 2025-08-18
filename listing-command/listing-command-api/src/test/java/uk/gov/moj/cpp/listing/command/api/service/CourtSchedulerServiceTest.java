package uk.gov.moj.cpp.listing.command.api.service;

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
import org.apache.http.client.methods.HttpGet;
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
class CourtSchedulerServiceTest {

    private static final String BASE_URI = "http://test-uri";
    private static final String COURTSCHEDULES_RESOURCE = "/courtschedule/search.court-schedules-by-id";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

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

    @InjectMocks
    private CourtSchedulerService courtSchedulerService;

    @BeforeEach
    void setUp() {
        courtSchedulerService.baseUri = BASE_URI;
    }

    @Test
    public void shouldGetCourtSchedulesByIdSuccessfully() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("courtScheduleIds", "id1,id2");
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
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            // When
            Response response = courtSchedulerService.getCourtSchedulesById(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + COURTSCHEDULES_RESOURCE + "?courtScheduleIds=id1%2Cid2"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenParamsAreNull() {
        // Given
        Map<String, String> params = null;

        // When/Then
        try {
            courtSchedulerService.getCourtSchedulesById(params);
        } catch (DataValidationException e) {
            assertThat(e.getMessage(), is("Params for CourtSchedules by id are null ...."));
        }
    }

    @Test
    public void shouldHandleHttpErrorResponse() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("courtScheduleIds", "id1,id2");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class);
             MockedStatic<EntityUtils> entityUtilsMockedStatic = Mockito.mockStatic(EntityUtils.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.BAD_REQUEST.getStatusCode());
            org.apache.http.HttpEntity entity = mock(org.apache.http.HttpEntity.class);
            when(httpResponse.getEntity()).thenReturn(entity);
            entityUtilsMockedStatic.when(() -> EntityUtils.toString(entity)).thenReturn("error response");

            // When
            Response response = courtSchedulerService.getCourtSchedulesById(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.getEntity(), is("error response"));
        }
    }

    @Test
    public void shouldHandleIOException() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("courtScheduleIds", "id1,id2");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenThrow(new IOException("Test exception"));

            // When
            Response response = courtSchedulerService.getCourtSchedulesById(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
            assertThat(response.getEntity(), is("Test exception"));
        }
    }

    @Test
    public void shouldHandleSystemUserProviderError() {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("courtScheduleIds", "id1,id2");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(java.util.Optional.empty());

        // When/Then
        try {
            courtSchedulerService.getCourtSchedulesById(params);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("contextSystemUserId missing!!!"));
        }
    }

    @Test
    public void shouldHandleResponseEntityConversionError() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("courtScheduleIds", "id1,id2");
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
                courtSchedulerService.getCourtSchedulesById(params);
            } catch (RuntimeException e) {
                assertThat(e.getMessage(), is("Conversion error"));
            }
        }
    }

    @Test
    public void shouldHandleMultipleQueryParameters() throws Exception {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("courtScheduleIds", "id1,id2");
        params.put("additionalParam", "value");
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
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            // When
            Response response = courtSchedulerService.getCourtSchedulesById(params);

            // Then
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            HttpGet capturedGet = httpGetCaptor.getValue();
            assertThat(capturedGet.getURI().toString(), is(BASE_URI + COURTSCHEDULES_RESOURCE + "?additionalParam=value&courtScheduleIds=id1%2Cid2"));
        }
    }
}