package uk.gov.moj.cpp.listing.common.client;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import uk.gov.moj.cpp.listing.common.tracing.Tracing;
import org.springframework.web.client.RestClient;

/**
 * The outbound {@link RestClient} (§6): JDK {@link HttpClient} with configured timeouts. Not
 * RestTemplate, which adds {@code Content-Length: 0} that the JDK client rejects (courtscheduler).
 */
@Configuration
@EnableConfigurationProperties(ListingClientsProperties.class)
public class CppRestClientConfig {

    @Bean
    public RestClient cppRestClient(final ListingClientsProperties properties) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
                .build();
        final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(propagateCorrelationId())
                .build();
    }

    /**
     * Propagates the request's correlation id on outbound calls (context/logging-standards.md).
     * Only added when the MDC has one and the caller has not set the header itself.
     */
    static ClientHttpRequestInterceptor propagateCorrelationId() {
        return (request, body, execution) -> {
            final String correlationId = MDC.get(Tracing.CORRELATION_ID);
            if (correlationId != null && !request.getHeaders().containsHeader(Tracing.CORRELATION_ID_HEADER)) {
                request.getHeaders().set(Tracing.CORRELATION_ID_HEADER, correlationId);
            }
            return execution.execute(request, body);
        };
    }
}
