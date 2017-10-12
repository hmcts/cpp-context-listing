package uk.gov.moj.cpp.listing.query.api;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class HearingQueryApi {
    private static final String UNALLOCATED = "unallocated";
    private static final String ALLOCATED = "allocated";
    private static final String TYPE = "type";

    @Inject
    private Requester requester;

    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {
        final String type = query.payloadAsJsonObject().getString(TYPE);

        if(!ALLOCATED.equalsIgnoreCase(type) && !UNALLOCATED.equalsIgnoreCase(type)) {
            throw new BadRequestException("Type query param is not valid: " + type +
                    ". Type should be " + "either " + ALLOCATED + " or " + UNALLOCATED);
        }
        return requester.request(query);
    }
}