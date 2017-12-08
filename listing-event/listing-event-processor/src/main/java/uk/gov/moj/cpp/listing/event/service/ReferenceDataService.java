package uk.gov.moj.cpp.listing.event.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonEnvelope getJudgeById(final UUID judgeId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", judgeId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "referencedata.get.judge").apply(payload);
        return requester.request(request);
    }
    
    public JsonEnvelope getCourtCentreById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, "referencedata.get.court-centre").apply(payload);
        return requester.request(request);
    }
}
