package uk.gov.moj.cpp.listing.common.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
class CourtSchedulerSearchServiceTest {

    private static final String BASE_URI = "http://test-uri";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

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
    private CourtSchedulerSearchService courtSchedulerSearchService;

    @BeforeEach
    void setUp() {
        courtSchedulerSearchService.baseUri = BASE_URI;
    }

    @Test
    void searchAvailableJudiciaries_shouldCallCourtSchedulerEndpoint() throws Exception {
        final Map<String, String> params = new HashMap<>();
        params.put("search", "ai");
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(TEST_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(Response.Status.OK.getStatusCode());
            when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
            when(stringToJsonObjectConverter.convert(any())).thenReturn(mock(javax.json.JsonObject.class));

            final Response response = courtSchedulerSearchService.searchAvailableJudiciaries(params);

            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            verify(httpClient).execute(httpGetCaptor.capture());
            assertThat(httpGetCaptor.getValue().getURI().toString(),
                    is(BASE_URI + "/judiciaries/search-available?search=ai"));
        }
    }
}
