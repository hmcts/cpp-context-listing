package uk.gov.moj.cpp.listing.command.service;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";
    private static final String REFERENCEDATA_QUERY_COURT_CENTRES = "referencedata.query.courtrooms";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public JsonEnvelope getHearingTypes(final JsonEnvelope event) {
        LOGGER.info("'referencedata.query.hearing-types' request");

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(createObjectBuilder().build())
                .withName(REFERENCEDATA_QUERY_HEARING_TYPES)
                .withMetadataFrom(event);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    public JsonEnvelope getAllCrownCourtCentres(final JsonEnvelope eventEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Attempting to get all of the crown court centres...");
        }

        final JsonObject payload = createObjectBuilder()
                .add("oucodeL1Code", "C")
                .build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(REFERENCEDATA_QUERY_COURT_CENTRES)
                .withMetadataFrom(eventEnvelope);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    @VisibleForTesting
    void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}
