package uk.gov.moj.cpp.listing.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class ReferenceDataQueryApi {

    @Inject
    private Requester requester;

    @Handles("listing.get.judges")
    public JsonEnvelope getJudges(final JsonEnvelope query) {
       return requester.request(query);
    }

    @Handles("listing.get.court-centres")
    public JsonEnvelope getCourtCentres(final JsonEnvelope query) {
       return requester.request(query);
    }

    @Handles("listing.get.court-rooms")
    public JsonEnvelope getCourtRooms(final JsonEnvelope query) {
       return requester.request(query);
    }

}
