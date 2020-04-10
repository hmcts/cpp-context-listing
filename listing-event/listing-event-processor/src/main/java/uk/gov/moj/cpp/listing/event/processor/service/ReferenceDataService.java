package uk.gov.moj.cpp.listing.event.processor.service;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNIT = "referencedata.query.organisation-unit";

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public OrganisationUnit getOrganizationUnitById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_ORGANISATION_UNIT).apply(payload);
        JsonEnvelope response = requester.request(request);
        LOGGER.debug("'referencedata.query.organisation-unit' response with payload {}", response.payloadAsJsonObject());
        return jsonObjectConverter.convert(response.payloadAsJsonObject(), OrganisationUnit.class);
    }
}
