package uk.gov.moj.cpp.listing.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TracingFilterTest {

    private final TracingFilter filter = new TracingFilter();

    @Test
    void usesTheInboundCorrelationIdAndANewRequestIdThenClearsTheMdc() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "abc-123");
        final Map<String, String> seen = new HashMap<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.putAll(MDC.getCopyOfContextMap()));

        assertThat(seen).containsEntry("correlationId", "abc-123");
        assertThat(UUID.fromString(seen.get("requestId"))).isNotNull();
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void fallsBackToTheTraceparentTraceId() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        assertThat(TracingFilter.correlationIdOf(request)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void generatesOneWhenAbsentOrUnsafe() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "bad value\n{\"x\":1}");

        final String id = TracingFilter.correlationIdOf(request);

        assertThat(UUID.fromString(id)).isNotNull();
    }
}
