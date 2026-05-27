package uk.gov.justice.api.resource;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiCourtScheduleDraftStatusResource implements QueryApiCourtScheduleDraftStatusResource {

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Override
    public Response checkDraftStatus(final JsonObject requestPayload) {
        final JsonObject responseBody = courtSchedulerServiceAdapter.getCourtScheduleDraftStatus(requestPayload);
        return Response.ok(responseBody).build();
    }
}
