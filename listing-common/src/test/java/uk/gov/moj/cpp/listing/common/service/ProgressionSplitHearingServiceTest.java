package uk.gov.moj.cpp.listing.common.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.common.service.ProgressionSplitHearingService.PROGRESSION_SPLIT_HEARING_TYPE;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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

    private static final String BASE_URI = "http://progression-uri";
    private static final String HEARING_ID = UUID.randomUUID().toString();

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
    @Mock
    private Metadata metadata;

    @InjectMocks
    private ProgressionSplitHearingService service;

    private final JsonObject payload = createObjectBuilder().add("splitDate", "2026-10-01").build();

    @BeforeEach
    void setUp() {
        service.baseUri = BASE_URI;
    }

    @Test
    void shouldForwardPayloadAndAllMetadataHeadersAndReturnProgressionBody() throws Exception {
        final UUID causationOne = UUID.randomUUID();
        final UUID causationTwo = UUID.randomUUID();
        when(metadata.userId()).thenReturn(of("user-1"));
        when(metadata.sessionId()).thenReturn(of("session-1"));
        when(metadata.clientCorrelationId()).thenReturn(of("correlation-1"));
        when(metadata.causation()).thenReturn(List.of(causationOne, causationTwo));
        final JsonObject responseBody = createObjectBuilder().add("status", "ok").build();
        when(stringToJsonObjectConverter.convert("{\"status\":\"ok\"}")).thenReturn(responseBody);

        final Response response = forward(202, "{\"status\":\"ok\"}");

        assertThat(response.getStatus(), is(202));
        assertThat(response.getEntity(), is(responseBody));

        final HttpPost post = capturePost();
        assertThat(post.getURI().toString(), is(BASE_URI + "/hearing/" + HEARING_ID + "/split"));
        assertThat(post.getFirstHeader("Content-Type").getValue(), is(PROGRESSION_SPLIT_HEARING_TYPE));
        assertThat(post.getFirstHeader("CJSCPPUID").getValue(), is("user-1"));
        assertThat(post.getFirstHeader("CPPSID").getValue(), is("session-1"));
        assertThat(post.getFirstHeader("CPPCLIENTCORRELATIONID").getValue(), is("correlation-1"));
        assertThat(post.getFirstHeader("CPPCAUSATION").getValue(), is(causationOne + "," + causationTwo));
        assertThat(new String(((StringEntity) post.getEntity()).getContent().readAllBytes()), is(payload.toString()));
    }

    @Test
    void shouldOmitOptionalHeadersWhenMetadataHasNone() throws Exception {
        when(metadata.userId()).thenReturn(empty());
        when(metadata.sessionId()).thenReturn(empty());
        when(metadata.clientCorrelationId()).thenReturn(empty());
        when(metadata.causation()).thenReturn(emptyList());

        forward(202, "");

        final HttpPost post = capturePost();
        assertThat(post.getFirstHeader("CJSCPPUID"), is(nullValue()));
        assertThat(post.getFirstHeader("CPPSID"), is(nullValue()));
        assertThat(post.getFirstHeader("CPPCLIENTCORRELATIONID"), is(nullValue()));
        assertThat(post.getFirstHeader("CPPCAUSATION"), is(nullValue()));
    }

    @Test
    void shouldReturnProgressionStatusUnchangedWithNullEntityWhenBodyBlank() throws Exception {
        stubEmptyMetadata();

        final Response response = forward(409, "  ");

        assertThat(response.getStatus(), is(409));
        assertThat(response.getEntity(), is(nullValue()));
    }

    @Test
    void shouldReturnNullEntityWhenProgressionSendsNoEntity() throws Exception {
        stubEmptyMetadata();

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            stubClient(mockedStatic);
            when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(404);
            when(httpResponse.getEntity()).thenReturn(null);

            final Response response = service.splitHearing(HEARING_ID, payload, metadata);

            assertThat(response.getStatus(), is(404));
            assertThat(response.getEntity(), is(nullValue()));
        }
    }

    @Test
    void shouldReturn500WhenProgressionCannotBeReached() throws Exception {
        stubEmptyMetadata();

        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            stubClient(mockedStatic);
            when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException("connection refused"));

            final Response response = service.splitHearing(HEARING_ID, payload, metadata);

            assertThat(response.getStatus(), is(500));
            assertThat(response.getEntity(), is(nullValue()));
        }
    }

    private Response forward(final int status, final String body) throws Exception {
        try (MockedStatic<HttpClientBuilder> mockedStatic = Mockito.mockStatic(HttpClientBuilder.class)) {
            stubClient(mockedStatic);
            when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(status);
            final HttpEntity entity = new StringEntity(body);
            when(httpResponse.getEntity()).thenReturn(entity);

            return service.splitHearing(HEARING_ID, payload, metadata);
        }
    }

    private void stubClient(final MockedStatic<HttpClientBuilder> mockedStatic) {
        mockedStatic.when(HttpClientBuilder::create).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient);
    }

    private void stubEmptyMetadata() {
        when(metadata.userId()).thenReturn(empty());
        when(metadata.sessionId()).thenReturn(empty());
        when(metadata.clientCorrelationId()).thenReturn(empty());
        when(metadata.causation()).thenReturn(emptyList());
    }

    private HttpPost capturePost() throws Exception {
        final ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(captor.capture());
        return captor.getValue();
    }
}
