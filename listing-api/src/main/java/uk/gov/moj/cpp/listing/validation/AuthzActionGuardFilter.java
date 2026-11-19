package uk.gov.moj.cpp.listing.validation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stops a client from choosing the Drools {@code Action.name} that authorises its own request.
 *
 * <p>{@code cp-auth-rules-filter:1.0.9} ({@code RequestActionResolver}) resolves the action from, in
 * order: the Content-Type vendor token, the <em>first</em> vendor token in Accept, the
 * {@code CPP-ACTION} header, then {@code "<METHOD> <PATH>"}. Two inputs in that chain are
 * client-controlled without being tied to the endpoint Spring MVC dispatches to:
 * <ol>
 *   <li>The {@code CPP-ACTION} header. For a request without a vendor media type it beats the
 *       path fallback, so a caller could name any action it holds and reach another handler. No
 *       client is required to send it; the legacy framework derived the action server-side from
 *       the media type. This filter hides it from the rest of the chain.</li>
 *   <li>Several vendor types in Accept. The resolver takes the first one, while Spring negotiates
 *       against any of them, so {@code Accept: <view-gated type>, <type the handler produces>} would
 *       authorise as the first and dispatch on the second. When Content-Type carries no vendor token
 *       (so Accept decides the action), more than one distinct vendor type in Accept is rejected
 *       with 406.</li>
 * </ol>
 *
 * <p>Listing note: every listing query shares Accept-negotiated vendor types (e.g.
 * {@code listing.search.hearing} vs {@code listing.search.hearings} carry different group lists),
 * so the second case matters here exactly as in courtscheduler.
 *
 * <p>Runs before the auth filter (default order {@code HIGHEST_PRECEDENCE + 30}). Ported unchanged
 * from courtscheduler.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthzActionGuardFilter extends OncePerRequestFilter {

    static final String ACTION_HEADER = "CPP-ACTION";

    private static final Pattern VND_TOKEN =
            Pattern.compile("(?i)\\bapplication/vnd\\.([a-z0-9][a-z0-9._-]*)(?:\\+[^\\s;,]+)?\\b");

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        if (!hasVendorToken(request.getContentType()) && distinctVendorTokens(acceptHeader(request)).size() > 1) {
            response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Accept must not list more than one vendor media type\"}");
            return;
        }
        final HttpServletRequest forwarded = request.getHeader(ACTION_HEADER) == null
                ? request
                : new ActionHeaderHidingRequest(request);
        chain.doFilter(forwarded, response);
    }

    private static String acceptHeader(final HttpServletRequest request) {
        final Enumeration<String> values = request.getHeaders("Accept");
        return values == null ? null : String.join(",", Collections.list(values));
    }

    private static boolean hasVendorToken(final String header) {
        return header != null && VND_TOKEN.matcher(header).find();
    }

    static Set<String> distinctVendorTokens(final String header) {
        final Set<String> tokens = new LinkedHashSet<>();
        if (header != null) {
            final Matcher matcher = VND_TOKEN.matcher(header);
            while (matcher.find()) {
                tokens.add(matcher.group(1).toLowerCase(Locale.ROOT));
            }
        }
        return tokens;
    }

    /** Presents the request as if no {@code CPP-ACTION} header had been sent. */
    private static final class ActionHeaderHidingRequest extends HttpServletRequestWrapper {

        ActionHeaderHidingRequest(final HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(final String name) {
            return ACTION_HEADER.equalsIgnoreCase(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(final String name) {
            return ACTION_HEADER.equalsIgnoreCase(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !ACTION_HEADER.equalsIgnoreCase(name))
                    .toList());
        }
    }
}
