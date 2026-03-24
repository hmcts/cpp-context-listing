package uk.gov.justice.api.resource;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiSessionAvailabilityValidationResource implements QueryApiSessionAvailabilityValidationResource {

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Override
    public Response validateSessionAvailability(final JsonObject requestPayload) {
        return courtSchedulerServiceAdapter.validateSessionAvailability(requestPayload);
    }
}
