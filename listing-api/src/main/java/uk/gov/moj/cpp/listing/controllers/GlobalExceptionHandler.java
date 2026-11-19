package uk.gov.moj.cpp.listing.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Reproduces the framework's error response shape (ported from courtscheduler, §4.7):
 * every 4xx/5xx body is {@code {"error":"<msg>"}}, several messages joined with {@code "; "}.
 * The key is {@code error}, not {@code errorMessage} (§11 pitfall 5).
 *
 * <p>Listing additions over courtscheduler, so that framework/JAX-RS statuses are not turned into
 * 500 by the catch-all handler:
 * <ul>
 *   <li>unknown path → 404, wrong method → 405, unsupported Content-Type → 415 (JAX-RS parity);</li>
 *   <li>unknown {@code Accept} → 406 (§11 pitfall 6, as courtscheduler).</li>
 * </ul>
 * Courtscheduler's domain-specific handlers (its validators, converters, persistence and 422
 * exceptions) are not ported: listing has no such types yet. They are added with the code that
 * throws them. P2.1 adds the query-side ones: not found (404), bad request (400) and a remote
 * query's 403.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(final ValidationFailedException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getErrorMessages());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleArgumentNotValid(final MethodArgumentNotValidException ex) {
        final List<String> messages = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage() == null ? e.toString() : e.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, messages);
    }

    /** Missing or unparseable required parameters / unparseable JSON body → 400 (Spring's default would be 500 here). */
    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            com.fasterxml.jackson.core.JsonParseException.class,
            com.fasterxml.jackson.databind.JsonMappingException.class,
            NumberFormatException.class,
            jakarta.json.JsonException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequestInput(final Exception ex) {
        return error(HttpStatus.BAD_REQUEST, List.of(String.valueOf(ex.getMessage())));
    }

    /** Method validation (jakarta.validation on @RequestParam) failures → 400. */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            final jakarta.validation.ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
    }

    /** {@code @ModelAttribute}-bound parameter binding failures → 400. */
    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(
            final org.springframework.validation.BindException ex) {
        final List<String> messages = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage() == null ? e.toString() : e.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, messages);
    }

    /** Query view "not found" (the JAX-RS NotFoundException of HearingQueryView) → 404. */
    @ExceptionHandler(uk.gov.moj.cpp.listing.query.view.exception.QueryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleQueryNotFound(final RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, List.of(String.valueOf(ex.getMessage())));
    }

    /** The framework's BadRequestException thrown by query code → 400. */
    @ExceptionHandler(uk.gov.moj.cpp.listing.query.view.exception.QueryBadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleQueryBadRequest(final RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, List.of(String.valueOf(ex.getMessage())));
    }

    /** A remote query answered 403 (the framework's AccessControlViolationException) → 403. */
    @ExceptionHandler(uk.gov.moj.cpp.listing.common.client.RemoteAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleRemoteAccessDenied(final RuntimeException ex) {
        return error(HttpStatus.FORBIDDEN, List.of(String.valueOf(ex.getMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(final IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, List.of(String.valueOf(ex.getMessage())));
    }

    /** Upstream CPP service failures → 502 Bad Gateway (rather than masking them as 500). */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleUpstream(final RestClientResponseException ex) {
        LOG.warn("Upstream CPP service call failed with {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return error(HttpStatus.BAD_GATEWAY,
                List.of("Upstream service responded with " + ex.getStatusCode().value()));
    }

    /**
     * An {@code Accept} that matches no producible media type → 406 Not Acceptable, as WildFly/JAX-RS
     * did. Without this the catch-all would map it to 500 (§11 pitfall 6).
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> handleNotAcceptable(final HttpMediaTypeNotAcceptableException ex) {
        return error(HttpStatus.NOT_ACCEPTABLE, List.of(ex.getMessage()));
    }

    /** A {@code Content-Type} the endpoint does not consume → 415, as JAX-RS did. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(final HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, List.of(ex.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(final HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, List.of(ex.getMessage()));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(final Exception ex) {
        return error(HttpStatus.NOT_FOUND, List.of(ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(final ResponseStatusException ex) {
        return error(HttpStatus.valueOf(ex.getStatusCode().value()),
                List.of(ex.getReason() == null ? ex.getMessage() : ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(final Exception ex) {
        LOG.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, List.of("Internal Server Error"));
    }

    private static ResponseEntity<Map<String, Object>> error(final HttpStatus status, final List<String> messages) {
        // Always application/json: the request's Accept may be a vendor type the error body
        // cannot be written as (that would itself fail and turn a 406 into a 500).
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(errorBody(messages));
    }

    /**
     * Legacy error body shape: {@code {"error":"<msg>"}}, a single-key map. Several messages are
     * joined with {@code "; "}.
     */
    static Map<String, Object> errorBody(final List<String> messages) {
        final Map<String, Object> body = new LinkedHashMap<>();
        if (messages.isEmpty()) {
            body.put("error", "");
        } else if (messages.size() == 1) {
            body.put("error", messages.get(0));
        } else {
            body.put("error", String.join("; ", messages));
        }
        return body;
    }
}
