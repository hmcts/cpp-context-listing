package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class XhibitReferenceDataService {

    private static final String REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS = "referencedata.query.cp-xhibit-court-mappings";
    private static final String REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS = "referencedata.query.cp-xhibit-courtroom-mappings";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";
    private static final String CREST_COURT_SITE_CODE = "crestCourtSiteCode";
    private static final String DEFAULT_CREST_COURT_SITE_CODE = "A";

    @Inject
    @ServiceComponent(Component.QUERY_VIEW)
    private Requester requester;

    public List<JsonObject> getCrestCourtSitesForCourtCentre(final UUID courtCentreId) {

        final JsonObject queryParameters = createObjectBuilder().add("ouId", courtCentreId.toString()).build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("cpXhibitCourtMappings").getValuesAs(JsonObject.class);
    }

    public Optional<JsonObject> getCourtRoom(final Envelope envelope, final UUID courtCentreId, final UUID courtRoomUUID) {

        final JsonObject cpCourtRoom = getCpCourtRoom(envelope, courtCentreId, courtRoomUUID);

        final String courtRoomId = cpCourtRoom.getJsonNumber("courtroomId").toString();

        final JsonObject queryParameters = createObjectBuilder().add("ouId", courtCentreId.toString()).build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        final JsonEnvelope response = requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()));

        return response.payload().equals(JsonValue.NULL) ? Optional.empty()
                : response.payloadAsJsonObject().getJsonArray("cpXhibitCourtRoomMappings").getValuesAs(JsonObject.class)
                .stream().filter(c -> c.getJsonNumber("courtRoomId").toString().equals(courtRoomId))
                .findFirst();
    }

    public String getDefaultCrestCourtSiteCode(final UUID courtCentreId) {

        return getCrestCourtSitesForCourtCentre(courtCentreId)
                .stream()
                .map(courtSite -> courtSite.getString(CREST_COURT_SITE_CODE))
                .sorted()
                .findFirst().orElse(DEFAULT_CREST_COURT_SITE_CODE);
    }

    private JsonObject getCpCourtRoom(final Envelope envelope, final UUID courtCentreId, final UUID courtRoomUUID) {
        final JsonObject queryParameters = createObjectBuilder().add("id", courtCentreId.toString()).build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_COURTROOM)
                .withMetadataFrom(envelope);

        return requester.requestAsAdmin(envelopeFrom(requestEnvelope.metadata(), requestEnvelope.payload()))
                .payloadAsJsonObject().getJsonArray("courtrooms").getValuesAs(JsonObject.class)
                .stream().filter(c -> UUID.fromString(c.getString("id")).equals(courtRoomUUID))
                .findFirst().orElseThrow(() -> new RuntimeException("Cannot find court room uuid " + courtRoomUUID.toString()));
    }
}
