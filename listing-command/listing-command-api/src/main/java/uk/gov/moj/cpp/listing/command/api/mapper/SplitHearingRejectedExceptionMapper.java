package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;

import uk.gov.moj.cpp.listing.common.split.SplitHearingRejectedException;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

@Provider
public class SplitHearingRejectedExceptionMapper implements ExceptionMapper<SplitHearingRejectedException> {

    @Inject
    Logger logger;

    @Override
    public Response toResponse(final SplitHearingRejectedException exception) {
        logger.debug("split-hearing rejected by progression", exception);

        final JsonObject body = exception.getResponseBody() == null
                ? createObjectBuilder().build()
                : exception.getResponseBody();

        return status(exception.getHttpStatus())
                .entity(body.toString())
                .type(APPLICATION_JSON)
                .build();
    }
}
