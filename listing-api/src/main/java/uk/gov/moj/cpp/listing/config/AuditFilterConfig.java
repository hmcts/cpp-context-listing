package uk.gov.moj.cpp.listing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@code cp-audit-filter-springboot} (replaces the framework's audit interceptor). The
 * library needs the OpenAPI spec path even when HTTP audit is disabled. Ported from courtscheduler.
 */
@Configuration
public class AuditFilterConfig {

    @Bean
    public String auditOpenApiRestSpec(
            @Value("${audit.http.openapi-rest-spec:openapi/listing-api.openapi.yml}")
            final String openApiRestSpec) {
        return openApiRestSpec;
    }

    @Bean
    public Boolean auditHttpEnabled(@Value("${audit.http.enabled:false}") final boolean enabled) {
        return enabled;
    }
}
