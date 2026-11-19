package uk.gov.moj.cpp.listing.controllers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * Renders errors that bypass {@link GlobalExceptionHandler} — responses produced with
 * {@code HttpServletResponse#sendError} and forwarded to {@code /error}, notably cp-auth-rules-filter's
 * 401 (no {@code CJSCPPUID}) and 403 (access denied) — in the legacy {@code {"error":"<msg>"}} shape
 * instead of Boot's {@code {timestamp,status,error,path}} (§4 invariant 7). The message is the one
 * passed to {@code sendError} (e.g. "Access denied"), or the status reason phrase.
 *
 * <p>Listing addition; courtscheduler leaves Boot's default body for these.
 */
@Component
public class LegacyErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(final WebRequest webRequest, final ErrorAttributeOptions options) {
        final Map<String, Object> defaults = super.getErrorAttributes(webRequest,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.STATUS,
                        ErrorAttributeOptions.Include.ERROR));
        final Object message = defaults.get("message");
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message instanceof String text && !text.isBlank() ? text : defaults.getOrDefault("error", ""));
        return body;
    }
}
