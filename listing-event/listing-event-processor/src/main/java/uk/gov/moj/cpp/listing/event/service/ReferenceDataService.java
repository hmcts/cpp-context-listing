package uk.gov.moj.cpp.listing.event.service;

import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_GET_JUDGE = "referencedata.get.judge";
    private static final String REFERENCEDATA_GET_COURT_CENTRE = "referencedata.get.court-centre";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonEnvelope getJudgeById(final UUID judgeId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", judgeId.toString()).build();
        LOGGER.debug(format("'referencedata.get.judge' received with payload %s", payload));
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_JUDGE).apply(payload);
        return requester.request(request);
    }
    
    public JsonEnvelope getCourtCentreById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.debug(format("'referencedata.get.court-centre' received with payload %s", payload));
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_COURT_CENTRE).apply(payload);
        return requester.request(request);
    }
}
