package uk.gov.moj.cpp.listing.common.tracing;

/**
 * Names shared by the request tracing filter and outbound clients
 * (context/logging-standards.md, "Required MDC fields").
 */
public final class Tracing {

    /** MDC key and HTTP header carrying the correlation id across services. */
    public static final String CORRELATION_ID = "correlationId";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRACEPARENT_HEADER = "traceparent";

    /** MDC key for the per-request id. */
    public static final String REQUEST_ID = "requestId";

    private Tracing() {
    }
}
