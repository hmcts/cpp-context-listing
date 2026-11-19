package uk.gov.moj.cpp.listing.envelope;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Response-side counterpart of the framework's REST response strategies. Ported from
 * courtscheduler's advice, with one listing-specific difference (recorded in ADR 007):
 *
 * <p><b>Listing's legacy wire shape is payload-only.</b> The generated listing query resources used
 * {@code OkStatusEnvelopePayloadEntityResponseStrategy}, which writes {@code envelope.payloadAsJsonObject()}
 * as the body (no {@code _metadata}) and the envelope id as the {@code CPPID} response header.
 * Courtscheduler's UI-facing endpoints returned the whole envelope instead. So for listing:
 * <ul>
 *   <li>{@code listing.envelope.enabled} defaults to {@code false}: 2xx JSON bodies are written
 *       as-is, as WildFly did;</li>
 *   <li>every 2xx JSON response carries a {@code CPPID} header (a fresh UUID, the legacy envelope id);</li>
 *   <li>setting {@code listing.envelope.enabled=true} wraps 2xx JSON bodies in the
 *       {@code _metadata} envelope (courtscheduler behaviour) — kept only as a switch, not the default.</li>
 * </ul>
 * Error bodies (4xx/5xx) are never wrapped. Non-JSON bodies (e.g. {@code text/csv}) and methods or
 * classes annotated with {@link SkipEnvelope} are left alone.
 */
@RestControllerAdvice
public class EnvelopeResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    static final String CPPID = "CPPID";
    private static final String CJSCPPUID = "CJSCPPUID";
    private static final String VENDOR_PREFIX = "vnd.";
    private static final String JSON_SUFFIX = "+json";

    @Value("${listing.envelope.enabled:false}")
    private boolean envelopeEnabled;

    @Override
    public boolean supports(final MethodParameter returnType,
                            final Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethodAnnotation(SkipEnvelope.class) == null
                && (returnType.getContainingClass() == null
                    || returnType.getContainingClass().getAnnotation(SkipEnvelope.class) == null);
    }

    @Override
    public Object beforeBodyWrite(final Object body,
                                  final MethodParameter returnType,
                                  final MediaType selectedContentType,
                                  final Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  final ServerHttpRequest request,
                                  final ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        final MediaType contentType = selectedContentType == null ? MediaType.APPLICATION_JSON : selectedContentType;
        if (!isJsonLike(contentType)) {
            return body;
        }
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        // Error responses (from GlobalExceptionHandler) stay bare {"error":"<msg>"} bodies.
        if (response instanceof ServletServerHttpResponse servletResp
                && servletResp.getServletResponse().getStatus() >= 400) {
            return body;
        }
        final String envelopeId = UUID.randomUUID().toString();
        response.getHeaders().set(CPPID, envelopeId);
        if (!envelopeEnabled) {
            return body;
        }
        final HttpServletRequest http = servletRequest.getServletRequest();
        return JsonEnvelopeWrapper.wrap(body, envelopeId, vendorToken(contentType), http.getHeader(CJSCPPUID));
    }

    private static String vendorToken(final MediaType type) {
        final String subtype = type.getSubtype();
        if (subtype != null && subtype.startsWith(VENDOR_PREFIX) && subtype.endsWith(JSON_SUFFIX)) {
            return subtype.substring(VENDOR_PREFIX.length(), subtype.length() - JSON_SUFFIX.length());
        }
        return null;
    }

    private static boolean isJsonLike(final MediaType type) {
        return MediaType.APPLICATION_JSON.includes(type)
                || (type.getSubtype() != null && type.getSubtype().endsWith("+json"));
    }
}
