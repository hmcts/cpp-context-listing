package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.query.view.dto.ProsecutionCase;

import java.util.Optional;
import java.util.UUID;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class.getName());

    private static final String PROGRESSION_CASE_DETAILS = "progression.query.prosecutioncase";
    private static final String PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN = "progression.query.case-exist-by-caseurn";
    private static final String PROGRESSION_QUERY_CASE_NOTES = "progression.query.case-notes";
    private static final String PROGRESSION_QUERY_APPLICATION_NOTES = "progression.query.application-notes";
    private static final String CASE_ID = "caseId";


    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private UtcClock utcClock;

    public ProsecutionCase getProsecutionCaseDetails(final UUID caseId) {
        final JsonObject query = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder()
                        .createdAt(utcClock.now())
                        .withName(PROGRESSION_CASE_DETAILS)
                        .withId(randomUUID())
                        .build(),
                query);

        return requester.requestAsAdmin(jsonEnvelope, ProsecutionCase.class).payload();
    }

    public Optional<JsonObject> caseExistsByCaseUrn(final JsonEnvelope envelope, final String caseUrn) {

        final JsonObject requestParameter = createObjectBuilder().add("caseUrn", caseUrn).build();

        LOGGER.info("search for case detail with caseUrn {} ", caseUrn);

        final JsonEnvelope response = requester.request(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN), requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("search for case detail response {}", response.toObfuscatedDebugString());
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public uk.gov.justice.core.courts.ProsecutionCase getProsecutionCaseByCaseId(final JsonEnvelope envelope, final String caseId) {
        final Metadata metadataWithActionName = metadataFrom(envelope.metadata()).withName(PROGRESSION_CASE_DETAILS).build();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, requestParameter);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return jsonObjectToObjectConverter.convert(response.payload().getJsonObject("prosecutionCase"), uk.gov.justice.core.courts.ProsecutionCase.class);
    }

}
