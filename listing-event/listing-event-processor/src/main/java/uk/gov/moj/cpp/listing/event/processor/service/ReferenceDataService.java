package uk.gov.moj.cpp.listing.event.processor.service;


import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNIT = "referencedata.query.organisation-unit";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public OrganisationUnit getOrganizationUnitById(final UUID courtCentreId, final JsonEnvelope event) {
        return getOrganizationUnitByIdWithAdmin(courtCentreId, event);
    }

    public OrganisationUnit getOrganizationUnitByIdWithAdmin(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_ORGANISATION_UNIT).apply(payload);
        final Envelope<OrganisationUnit> response = requester.requestAsAdmin(request, OrganisationUnit.class);

        if (Objects.isNull(response) || Objects.isNull(response.payload()) ) {
            throw new InvalidReferenceDataException("Cannot find organisationunit for courtCenter : " +  courtCentreId);
        }

        LOGGER.debug("'referencedata.query.organisation-unit' response with payload {}", objectToJsonObjectConverter.convert(response.payload()));

        return response.payload();
    }

    public JsonEnvelope getCourtCentreById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.info("'referencedata.query.courtroom' request with payload {}", payload);

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(REFERENCEDATA_QUERY_COURTROOM)
                .withMetadataFrom(event);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    public String getOucodeFromEnvelope(final UUID courtCentreId, final JsonEnvelope event){
        final JsonEnvelope jsonEnvelope = getCourtCentreById(courtCentreId, event);
        final JsonObject organisationUnitJsonObject = jsonEnvelope.payloadAsJsonObject();
        return organisationUnitJsonObject.getString("oucode");
    }
}
