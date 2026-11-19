package uk.gov.moj.cpp.listing.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;

class CorrelationIdPropagationTest {

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void addsTheMdcCorrelationIdToOutboundCalls() throws Exception {
        MDC.put("correlationId", "abc-123");
        final MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://x"));
        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        CppRestClientConfig.propagateCorrelationId().intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("abc-123");
        verify(execution).execute(any(), any());
    }

    @Test
    void addsNothingOutsideARequest() throws Exception {
        final MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://x"));

        CppRestClientConfig.propagateCorrelationId().intercept(request, new byte[0], mock(ClientHttpRequestExecution.class));

        assertThat(request.getHeaders().getFirst("X-Correlation-Id")).isNull();
    }
}
