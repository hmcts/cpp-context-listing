package uk.gov.moj.cpp.listing.query.api.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final  String IS_WELSH = "isWelsh";

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

    public Optional<Boolean> isHearingLanguageWelsh(final JsonEnvelope envelope, final String courtCentreId) {
        final JsonEnvelope courtCentreEnvelope = getCourtCentreById(UUID.fromString(courtCentreId), envelope);
        final JsonObject jsonObject = courtCentreEnvelope.payloadAsJsonObject();
        final Boolean welsh = jsonObject.getBoolean(IS_WELSH, false);
        return Optional.ofNullable(welsh);
    }


}
