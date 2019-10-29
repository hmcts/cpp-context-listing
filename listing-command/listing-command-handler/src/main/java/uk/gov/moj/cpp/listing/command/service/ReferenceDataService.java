package uk.gov.moj.cpp.listing.command.service;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public JsonEnvelope getHearingTypes(final JsonEnvelope event) {
        LOGGER.info("'referencedata.query.hearing-types' request");
        return requester.request(enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_HEARING_TYPES).apply(createObjectBuilder().build()));
    }
}
