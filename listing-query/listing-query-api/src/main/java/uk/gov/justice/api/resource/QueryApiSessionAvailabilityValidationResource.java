package uk.gov.justice.api.resource;

import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("sessionAvailabilityValidation")
public interface QueryApiSessionAvailabilityValidationResource {

    @POST
    @Consumes("application/vnd.listing.validate.session.availability+json")
    @Produces({
            "application/vnd.listing.validate.session.availability.response+json",
            "application/json"
    })
    Response validateSessionAvailability(JsonObject requestPayload);
}
