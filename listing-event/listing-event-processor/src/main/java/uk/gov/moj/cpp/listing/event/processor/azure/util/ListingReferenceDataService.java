package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class ListingReferenceDataService {
    private static final String REFERENCE_DATA_GET_COURTROOM = "referencedata.query.courtroom";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonEnvelope getPayLoadForCourtRoom(final JsonEnvelope jsonEnvelope, final String courtCentreId) {
        final JsonObject ouCodeQueryParameter = Json.createObjectBuilder()
                .add("id", courtCentreId)
                .build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(ouCodeQueryParameter)
                .withName(REFERENCE_DATA_GET_COURTROOM)
                .withMetadataFrom(jsonEnvelope);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));
    }

    public int retrieveCourtRoomId(final JsonObject responseForCourtRoom, final UUID roomId, final UUID courtCentreId) {
        final JsonObject jsonObject = responseForCourtRoom.getJsonArray("courtrooms").getValuesAs(JsonObject.class)
                .stream().filter(c -> UUID.fromString(c.getString("id")).equals(roomId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(format("Cannot find court room with id %s in courtCentre %s", roomId, courtCentreId)));

        return jsonObject.getInt("courtroomId");
    }
}
