package uk.gov.moj.cpp.listing.validation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Resolves the Spring MVC handler for a listing API request <em>before</em> the auth filter, and
 * answers routing failures the way WildFly/JAX-RS did: unknown path → 404, wrong method → 405,
 * unsupported {@code Content-Type} → 415, {@code Accept} matching nothing → 406, each with the
 * {@code {"error":"<msg>"}} body.
 *
 * <p>Why (listing-specific, not in courtscheduler): in the legacy app JAX-RS matched the resource
 * method before the framework's access-control interceptor ran, so a request with an unknown
 * {@code Accept} got 406 whoever sent it. cp-auth-rules-filter runs as a servlet filter, ahead of
 * Spring MVC routing, with {@code deny-when-no-rules: true}: such a request resolves to an action
 * with no DRL rule (an unknown vendor token, or the {@code "<METHOD> <PATH>"} fallback) and would
 * get 403 instead of 406 (§4 invariant 7, §11 pitfall 6). Courtscheduler only proves the 406 in a
 * MockMvc test without the auth filter.
 *
 * <p>Only paths under the two API roots are checked; actuator and error paths are untouched.
 * Order {@code HIGHEST_PRECEDENCE + 15}: after {@code AuthzActionGuardFilter} (+10), before the
 * schema filter (+20) and the auth filter (+30). JAX-RS also matched before reading the body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class RequestRoutingGuardFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestRoutingGuardFilter.class);
    static final List<String> API_ROOTS = List.of("/command/api/rest/listing/", "/query/api/rest/listing/");

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMapping;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    public RequestRoutingGuardFilter(final ObjectProvider<RequestMappingHandlerMapping> handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = urlPathHelper.getPathWithinApplication(request);
        return "OPTIONS".equals(request.getMethod()) || API_ROOTS.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        final HttpStatus failure = routingFailure(request);
        if (failure == null) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(failure.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + message(failure) + "\"}");
    }

    private HttpStatus routingFailure(final HttpServletRequest request) {
        final RequestMappingHandlerMapping mapping = handlerMapping.getIfAvailable();
        if (mapping == null) {
            return null;
        }
        final Object previousPath = request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
        try {
            ServletRequestPathUtils.parseAndCache(request);
            final HandlerExecutionChain handler = mapping.getHandler(request);
            return handler == null ? HttpStatus.NOT_FOUND : null;
        } catch (final HttpMediaTypeNotAcceptableException e) {
            return HttpStatus.NOT_ACCEPTABLE;
        } catch (final HttpMediaTypeNotSupportedException e) {
            return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        } catch (final HttpRequestMethodNotSupportedException e) {
            return HttpStatus.METHOD_NOT_ALLOWED;
        } catch (final Exception e) {
            // Anything else (e.g. a bad request parameter condition) is left to Spring MVC.
            LOG.debug("Routing pre-check failed for {} {}: {}", request.getMethod(), request.getRequestURI(), e.toString());
            return null;
        } finally {
            if (previousPath == null) {
                ServletRequestPathUtils.clearParsedRequestPath(request);
            } else {
                request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, previousPath);
            }
        }
    }

    private static String message(final HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "No resource found for this path";
            case METHOD_NOT_ALLOWED -> "Request method not supported for this path";
            case UNSUPPORTED_MEDIA_TYPE -> "Content-Type not supported for this path";
            case NOT_ACCEPTABLE -> "No acceptable representation for the Accept header";
            default -> status.getReasonPhrase();
        };
    }
}
