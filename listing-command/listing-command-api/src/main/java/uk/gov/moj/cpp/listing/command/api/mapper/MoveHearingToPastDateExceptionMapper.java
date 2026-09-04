package uk.gov.moj.cpp.listing.command.api.mapper;

import static jakarta.json.Json.createObjectBuilder;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.status;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;

@Provider
public class MoveHearingToPastDateExceptionMapper implements ExceptionMapper<MoveHearingToPastDateException> {

    @Inject
    Logger logger;

    @Override
    public Response toResponse(final MoveHearingToPastDateException exception) {
        logger.debug("move-hearing-to-past-date rejected", exception);

        final JsonObjectBuilder builder = createObjectBuilder();
        if (exception.getErrorCode() != null) {
            builder.add("errorCode", exception.getErrorCode());
        }
        final JsonObject responseBody = exception.getResponseBody();
        final String message = responseBody == null
                ? exception.getMessage()
                : getString(responseBody, "message").orElse(exception.getMessage());
        if (message != null) {
            builder.add("message", message);
        }

        return status(exception.getHttpStatus())
                .entity(builder.build().toString())
                .type(APPLICATION_JSON)
                .build();
    }
}
