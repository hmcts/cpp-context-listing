package uk.gov.moj.cpp.listing.command.api.service;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_SINGLE_COURTROOM = "referencedata.query.courtroom";
    private static final String REFERENCEDATA_QUERY_MULTI_COURTROOMS = "referencedata.query.courtrooms";
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNIT = "referencedata.query.organisation-unit";

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public JsonEnvelope getCourtCentresById(Set<UUID> courtCentreIdList, JsonEnvelope event) {
        final String courtCenterIdsStr = courtCentreIdList.stream().map(UUID::toString).collect(Collectors.joining(","));
        final JsonObject payload = createObjectBuilder().add("courtCentreId", courtCenterIdsStr.toString()).build();
        LOGGER.info("'referencedata.query.courtrooms' request with payload {}", payload);
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(REFERENCEDATA_QUERY_MULTI_COURTROOMS)
                .withMetadataFrom(event);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    public JsonEnvelope getCourtCentreById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.info("'referencedata.query.courtroom' request with payload {}", payload);

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(REFERENCEDATA_QUERY_SINGLE_COURTROOM)
                .withMetadataFrom(event);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));

    }

    public OrganisationUnit getOrganizationUnitById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_ORGANISATION_UNIT).apply(payload);
        JsonEnvelope response = requester.request(request);
        LOGGER.debug("'referencedata.query.organisation-unit' response with payload {}", response.payloadAsJsonObject());
        return jsonObjectConverter.convert(response.payloadAsJsonObject(), OrganisationUnit.class);
    }
}
