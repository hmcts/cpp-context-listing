package uk.gov.moj.cpp.listing.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates inbound request bodies against the legacy RAML JSON schemas, restoring the framework
 * behaviour ({@code JsonSchemaValidationInterceptor} in rest-adapter-core): a {@code +json} body
 * is validated against the schema registered for its media type before the controller sees it.
 * Ported from courtscheduler.
 *
 * <p>Uses everit-json-schema, the library the framework used, so draft-04 semantics match (e.g.
 * {@code format: date} is not a draft-04 format and is ignored). Faithful to the old behaviour:
 * <ul>
 *   <li>only request bodies are validated — GET/HEAD/OPTIONS/TRACE pass through;</li>
 *   <li>a media type without a schema passes through unvalidated (e.g.
 *       {@code application/vnd.listing.duplicate-unallocated-hearing+json}, declared {@code !!null}
 *       in RAML);</li>
 *   <li>a failure returns 400 with the framework error shape: {@code "error"} plus, as listing's
 *       {@code BadRequestExceptionMapper} did, {@code "validationErrors"}.</li>
 * </ul>
 *
 * <p>Listing differences from courtscheduler: the media type → schema mapping comes from the
 * OpenAPI spec ({@link RequestSchemaRegistry}) rather than {@code <token>.json}, and remote
 * {@code $ref}s resolve through {@link LegacySchemaCatalog}.
 *
 * <p>Order: {@code HIGHEST_PRECEDENCE + 20}, i.e. after {@code AuthzActionGuardFilter} (+10) and
 * <b>before</b> cp-auth-rules-filter (+30). This follows §5 of the refactor prompt and the legacy
 * framework, where the JAX-RS {@code JsonSchemaValidationInterceptor} ran when the body was read,
 * before the access-control interceptor: a schema-invalid body gets 400 even from a caller who
 * would then be denied. Courtscheduler runs it after authz (+100); recorded in ADR 007. The
 * cached-body wrapper is visible to the auth and audit filters and the controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestSchemaValidationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSchemaValidationFilter.class);
    private static final String JSON_SUFFIX = "+json";

    private final ObjectMapper objectMapper;
    private final RequestSchemaRegistry registry;

    public RequestSchemaValidationFilter(final ObjectMapper objectMapper, final RequestSchemaRegistry registry) {
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String method = request.getMethod();
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method) || "TRACE".equals(method);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        final Optional<Schema> schema = schemaFor(request);
        if (schema.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        final byte[] body = request.getInputStream().readAllBytes();
        final CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);

        if (body.length == 0) {
            // Empty body: let the controller decide, as courtscheduler does.
            chain.doFilter(wrapped, response);
            return;
        }

        final Object json;
        try {
            json = new JSONTokener(new String(body, StandardCharsets.UTF_8)).nextValue();
        } catch (final JSONException parseEx) {
            writeBadRequest(response, "Request body is not valid JSON", null);
            return;
        }

        try {
            schema.get().validate(json);
        } catch (final ValidationException ve) {
            final String joined = String.join("; ", ve.getAllMessages());
            LOG.debug("Request body for {} failed its schema: {}", request.getContentType(), joined);
            writeBadRequest(response, "JSON schema validation has failed: " + joined, ve);
            return;
        }

        chain.doFilter(wrapped, response);
    }

    private Optional<Schema> schemaFor(final HttpServletRequest request) {
        final String contentType = request.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return Optional.empty();
        }
        final MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (final RuntimeException invalid) {
            return Optional.empty();
        }
        if (!mediaType.getSubtype().endsWith(JSON_SUFFIX)) {
            return Optional.empty();
        }
        return registry.schemaFor(mediaType.getType() + "/" + mediaType.getSubtype());
    }

    private void writeBadRequest(final HttpServletResponse response, final String message,
                                 final ValidationException cause) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        if (cause != null) {
            body.put("validationErrors", ValidationErrorsRenderer.render(cause));
        }
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
