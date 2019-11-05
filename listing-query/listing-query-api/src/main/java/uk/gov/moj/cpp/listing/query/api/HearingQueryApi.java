package uk.gov.moj.cpp.listing.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class HearingQueryApi {

    @Inject
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("listing.range.search.hearings")
    public JsonEnvelope rangeSsearchHearings(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("listing.search.court.list")
    public JsonEnvelope searchHearingsForCourtList(final JsonEnvelope query){
        return requester.request(query);
    }

    @Handles("listing.court.list.publish.status")
    public JsonEnvelope publishCourtListStatus(final JsonEnvelope query) {
        return requester.request(query);
    }
}
