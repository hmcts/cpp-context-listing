package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.moj.cpp.listing.common.splithearing.SplitHearingRejectedException;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

@Provider
public class SplitHearingRejectedExceptionMapper implements ExceptionMapper<SplitHearingRejectedException> {

    public static final String HEARING_NOT_SPLITTABLE = "HEARING_NOT_SPLITTABLE";

    private static final String ERROR_CODE = "errorCode";

    @Inject
    Logger logger;

    private static Optional<String> errorCodeFrom(final SplitHearingRejectedException exception) {
        if (exception.getErrorCode() != null) {
            return Optional.of(exception.getErrorCode());
        }
        return SC_CONFLICT == exception.getHttpStatus() ? Optional.of(HEARING_NOT_SPLITTABLE) : Optional.empty();
    }

    private static Optional<String> reasonFrom(final JsonObject responseBody) {
        if (responseBody == null) {
            return Optional.empty();
        }
        final Optional<String> message = getString(responseBody, "message");
        return message.isPresent() ? message : getString(responseBody, "error");
    }

    @Override
    public Response toResponse(final SplitHearingRejectedException exception) {
        logger.debug("split-hearing rejected by progression", exception);

        final JsonObjectBuilder builder = createObjectBuilder();
        errorCodeFrom(exception).ifPresent(code -> builder.add(ERROR_CODE, code));
        final JsonObject responseBody = exception.getResponseBody();
        final String message = reasonFrom(responseBody).orElseGet(exception::getMessage);
        if (message != null) {
            builder.add("message", message);
        }
        if (responseBody != null) {
            getString(responseBody, "id").ifPresent(id -> builder.add("id", id));
        }

        return status(exception.getHttpStatus())
                .entity(builder.build().toString())
                .type(APPLICATION_JSON)
                .build();
    }
}
