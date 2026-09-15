package uk.gov.moj.cpp.listing.common.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressionSplitHearingServiceTest {

    private static final UUID SYSTEM_USER_ID = UUID.randomUUID();
    private static final String HEARING_ID = "b14ba162-3f21-4c8e-9a77-1d2e5c8b4a90";

    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Mock
    private HttpClientBuilder httpClientBuilder;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private org.apache.http.client.methods.CloseableHttpResponse httpResponse;
    @Mock
    private StatusLine statusLine;

    @InjectMocks
    private ProgressionSplitHearingService service;

    private JsonObject splitRequest;

    @BeforeEach
    void setUp() {
        service.baseUri = "http://localhost:8080/progression-command-api/command/api/rest/progression";
        splitRequest = Json.createObjectBuilder()
                .add("listNewHearing", Json.createObjectBuilder().add("estimatedMinutes", 1080))
                .add("sendNotificationToParties", false)
                .build();
    }

    private MockedStatic<HttpClientBuilder> givenProgressionResponds(final int status, final String body)
            throws Exception {
        final MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class);
        mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(status);

        final HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new java.io.ByteArrayInputStream(body.getBytes()));
        when(httpResponse.getEntity()).thenReturn(entity);
        return mockedStatic;
    }

    @Test
    void shouldPostToTheSplitResourceForTheSourceHearing() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
        when(stringToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());

        try (MockedStatic<HttpClientBuilder> ignored = givenProgressionResponds(202, "{}")) {
            final Response response = service.split(HEARING_ID, splitRequest);

            assertThat(response.getStatus(), is(202));

            final ArgumentCaptor<HttpPost> post = ArgumentCaptor.forClass(HttpPost.class);
            Mockito.verify(httpClient).execute(post.capture());
            assertThat(post.getValue().getURI().getPath().endsWith("/hearing/" + HEARING_ID + "/split"), is(true));
            assertThat(post.getValue().getFirstHeader("Content-Type").getValue(),
                    is("application/vnd.progression.split-hearing+json"));
            assertThat(post.getValue().getFirstHeader("CJSCPPUID").getValue(), is(SYSTEM_USER_ID.toString()));
        }
    }

    // Progression owns the decision; listing must not translate its rejections.
    @Test
    void shouldReturnProgressionRejectionStatusUnchanged() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
        when(stringToJsonObjectConverter.convert(any())).thenReturn(
                Json.createObjectBuilder().add("error", "not a subset").build());

        try (MockedStatic<HttpClientBuilder> ignored = givenProgressionResponds(400, "{\"error\":\"not a subset\"}")) {
            final Response response = service.split(HEARING_ID, splitRequest);

            assertThat(response.getStatus(), is(400));
            assertThat(((JsonObject) response.getEntity()).getString("error"), is("not a subset"));
        }
    }

    @Test
    void shouldReturnAnEmptyObjectWhenProgressionSendsNoBody() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));

        try (MockedStatic<HttpClientBuilder> ignored = givenProgressionResponds(202, "")) {
            final Response response = service.split(HEARING_ID, splitRequest);

            assertThat(response.getStatus(), is(202));
            assertThat(((JsonObject) response.getEntity()).isEmpty(), is(true));
        }
    }

    // A transport failure must not surface as a split that silently did nothing.
    @Test
    void shouldReturn500WhenTheCallToProgressionFails() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
            when(httpClientBuilder.build()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpPost.class))).thenThrow(new java.io.IOException("connection refused"));

            final Response response = service.split(HEARING_ID, splitRequest);

            assertThat(response.getStatus(), is(500));
        }
    }

    @Test
    void shouldFailLoudlyWhenNoSystemUserIsAvailable() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.split(HEARING_ID, splitRequest));
    }
}
