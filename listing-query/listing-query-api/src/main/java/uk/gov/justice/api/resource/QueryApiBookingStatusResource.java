package uk.gov.justice.api.resource;

import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("bookingStatus")
public interface QueryApiBookingStatusResource {

    @POST
    @Consumes("application/vnd.listing.query.booking.status+json")
    @Produces({
            "application/vnd.listing.query.booking.status.response+json",
            "application/json"
    })
    Response checkBookingStatus(JsonObject requestPayload);
}
