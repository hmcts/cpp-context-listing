package uk.gov.justice.api.resource;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("updateAvailableHearingSlots ")
public interface UpdateHearingSlotsResource {
    String SLOT_DETAILS = "slotDetails";

    @PUT
    @Produces("application/vnd.listing.update.available.hearing.slots+json")
    Response updateHearingSlots(@QueryParam(SLOT_DETAILS) String slotDetails);
}