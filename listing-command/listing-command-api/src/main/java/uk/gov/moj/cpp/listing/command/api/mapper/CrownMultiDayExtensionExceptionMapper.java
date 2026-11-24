package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;

import uk.gov.moj.cpp.listing.common.crownfallback.CrownMultiDayExtensionException;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

@Provider
public class CrownMultiDayExtensionExceptionMapper implements ExceptionMapper<CrownMultiDayExtensionException> {

    @Inject
    Logger logger;

    @Override
    public Response toResponse(final CrownMultiDayExtensionException exception) {
        logger.debug("CROWN extend-multiday rejected by courtscheduler", exception);

        final JsonObjectBuilder builder = createObjectBuilder();
        if (exception.getErrorCode() != null) {
            builder.add("errorCode", exception.getErrorCode());
        }
        if (!exception.getUnavailableDates().isEmpty()) {
            final JsonArrayBuilder dates = createArrayBuilder();
            exception.getUnavailableDates().forEach(dates::add);
            builder.add("unavailableDates", dates);
        }

        return status(exception.getHttpStatus())
                .entity(builder.build().toString())
                .type(APPLICATION_JSON)
                .build();
    }
}
