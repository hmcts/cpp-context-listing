package uk.gov.justice.api.resource;

import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("courtScheduleDraftStatus")
public interface QueryApiCourtScheduleDraftStatusResource {

    @POST
    @Consumes("application/vnd.listing.query.court.schedule.draft.status+json")
    @Produces({
            "application/vnd.listing.query.court.schedule.draft.status.response+json",
            "application/json"
    })
    Response checkDraftStatus(JsonObject requestPayload);
}
