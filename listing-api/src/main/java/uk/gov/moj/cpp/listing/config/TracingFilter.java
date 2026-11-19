package uk.gov.moj.cpp.listing.config;

import static uk.gov.moj.cpp.listing.common.tracing.Tracing.CORRELATION_ID;
import static uk.gov.moj.cpp.listing.common.tracing.Tracing.CORRELATION_ID_HEADER;
import static uk.gov.moj.cpp.listing.common.tracing.Tracing.REQUEST_ID;
import static uk.gov.moj.cpp.listing.common.tracing.Tracing.TRACEPARENT_HEADER;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts {@code correlationId} and {@code requestId} in the MDC for every request and clears them
 * afterwards (context/logging-standards.md, "Required MDC fields").
 *
 * <p>The correlation id is the inbound {@code X-Correlation-Id}, else the trace id of a W3C
 * {@code traceparent}, else a new UUID. Outbound calls propagate it (see
 * {@code CppRestClientConfig}). The request id is always new. No response header is added, so the
 * HTTP contract is unchanged (§4).
 *
 * <p>Runs first, so every other filter's log lines carry the ids.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TracingFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private static final Pattern TRACEPARENT = Pattern.compile("^[0-9a-f]{2}-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}$");

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        MDC.put(CORRELATION_ID, correlationIdOf(request));
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID);
            MDC.remove(REQUEST_ID);
        }
    }

    static String correlationIdOf(final HttpServletRequest request) {
        final String header = request.getHeader(CORRELATION_ID_HEADER);
        // only accept a plain token: the value is logged, so never echo arbitrary input
        if (header != null && SAFE_ID.matcher(header.trim()).matches()) {
            return header.trim();
        }
        final String traceparent = request.getHeader(TRACEPARENT_HEADER);
        if (traceparent != null) {
            final Matcher matcher = TRACEPARENT.matcher(traceparent.trim());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return UUID.randomUUID().toString();
    }
}
