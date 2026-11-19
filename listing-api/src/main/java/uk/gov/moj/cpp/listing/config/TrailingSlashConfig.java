package uk.gov.moj.cpp.listing.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

/**
 * Parity (§4.1/§4.2, found by the legacy IT run on the Spring stack, P2.3.0): RESTEasy on WildFly
 * served {@code /hearings/?allocated=...} and {@code /hearings/range-search/?...} like the paths
 * without the trailing slash, and callers (the legacy ITs' endpoint.properties among them) use both
 * forms. Spring MVC 7 matches trailing slashes strictly (404), so this filter removes a trailing
 * slash from the request path before anything else sees it (routing guard, schema validation,
 * authz, handler mapping). Runs right after {@link TracingFilter}.
 */
@Configuration
public class TrailingSlashConfig {

    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        final FilterRegistrationBean<UrlHandlerFilter> registration =
                new FilterRegistrationBean<>(UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("trailingSlashFilter");
        return registration;
    }
}
