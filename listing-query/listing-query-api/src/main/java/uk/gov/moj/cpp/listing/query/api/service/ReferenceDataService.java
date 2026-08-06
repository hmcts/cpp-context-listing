package uk.gov.moj.cpp.listing.query.api.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
@ApplicationScoped
public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final  String IS_WELSH = "isWelsh";
    private static final String HEARING_TYPES_ARRAY = "hearingTypes";
    private static final String HEARING_TYPE_ID = "id";
    private static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";
    private static final String WELSH_HEARING_DESCRIPTION = "welshHearingDescription";
    private static final String REFERENCEDATA_QUERY_PROSECUTOR = "referencedata.query.prosecutor";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public JsonEnvelope getCourtCentreById(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.info("'referencedata.query.courtroom' request with payload {}", payload);

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(REFERENCEDATA_QUERY_COURTROOM)
                .withMetadataFrom(event);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    public JsonEnvelope getJudiciariesByIdList(final List<UUID> judiciaryIds, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("ids", judiciaryIds.stream().map(UUID::toString).collect(Collectors.joining(","))).build();
        LOGGER.info("'referencedata.query.judiciaries' request with payload {}", payload);

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(REFERENCEDATA_QUERY_JUDICIARIES)
                .withMetadataFrom(event);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    public JsonEnvelope getProsecutorById(final String prosecutorId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", prosecutorId).build();
        LOGGER.info("'referencedata.query.prosecutor' request with payload {}", payload);

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(REFERENCEDATA_QUERY_PROSECUTOR)
                .withMetadataFrom(event);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    public Optional<Boolean> isHearingLanguageWelsh(final JsonEnvelope envelope, final String courtCentreId) {
        final JsonEnvelope courtCentreEnvelope = getCourtCentreById(UUID.fromString(courtCentreId), envelope);
        final JsonObject jsonObject = courtCentreEnvelope.payloadAsJsonObject();
        final Boolean welsh = jsonObject.getBoolean(IS_WELSH, false);
        return Optional.ofNullable(welsh);
    }

    public Map<String, String> getHearingTypesIdWelshDescriptionMap(JsonEnvelope envelope) {
        LOGGER.info("'referencedata.query.hearing-types' request");

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(createObjectBuilder().build())
                .withName(REFERENCEDATA_QUERY_HEARING_TYPES)
                .withMetadataFrom(envelope);

        final JsonEnvelope hearingTypesEnvelope =    requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
        final JsonObject jsonObject = hearingTypesEnvelope.payloadAsJsonObject();
        final Map<String, String> hearingTypesMap = new HashMap<>();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("hearingTypes envelope response: {}", jsonObject);
        }
        final JsonArray hearingTypes = jsonObject.getJsonArray(HEARING_TYPES_ARRAY);
        hearingTypes.getValuesAs(JsonObject.class).stream().forEach(hearingType ->
                hearingTypesMap.put(hearingType.getString(HEARING_TYPE_ID), hearingType.getString(WELSH_HEARING_DESCRIPTION)));
        return hearingTypesMap;
    }

}
