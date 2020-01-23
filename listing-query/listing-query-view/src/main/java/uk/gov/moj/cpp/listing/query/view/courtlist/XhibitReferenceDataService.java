package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class XhibitReferenceDataService {

    private static final String REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS = "referencedata.query.cp-xhibit-court-mappings";
    private static final String REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS = "referencedata.query.cp-xhibit-courtroom-mappings";
    private static final String CREST_COURT_SITE_CODE = "crestCourtSiteCode";
    private static final String DEFAULT_CREST_COURT_SITE_CODE = "A";

    @Inject
    @ServiceComponent(Component.QUERY_VIEW)
    private Requester requester;

    public List<JsonObject> getCrestCourtSitesForCourtCentre(final Envelope envelope, final UUID courtCentreId) {

        final JsonObject queryParameters = createObjectBuilder().add("ouId", courtCentreId.toString()).build();

        return requester.request(envelop(queryParameters).withName(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS)
                .withMetadataFrom(envelope))
                .payloadAsJsonObject().getJsonArray("cpXhibitCourtMappings").getValuesAs(JsonObject.class);
    }

    public Optional<JsonObject> getCourtRoom(final Envelope envelope, final UUID courtCentreId, final UUID courtRoomId) {

        final JsonObject queryParameters = createObjectBuilder().add("ouId", courtCentreId.toString()).build();

        return requester.request(envelop(queryParameters).withName(REFERENCEDATA_QUERY_CP_XHIBIT_COURTROOM_MAPPINGS)
                .withMetadataFrom(envelope))
                .payloadAsJsonObject().getJsonArray("cpXhibitCourtRoomMappings").getValuesAs(JsonObject.class)
                .stream().filter(c -> UUID.fromString(c.getString("id")).equals(courtRoomId))
                .findFirst();
    }

    public  String getDefaultCrestCourtSiteCode(final Envelope envelope, final UUID courtCentreId) {

        return getCrestCourtSitesForCourtCentre(envelope, courtCentreId)
                .stream()
                .map(courtSite -> courtSite.getString(CREST_COURT_SITE_CODE))
                .sorted()
                .findFirst().orElse(DEFAULT_CREST_COURT_SITE_CODE);
    }

    public JsonObject getCrestCourtSiteJson(final JsonEnvelope envelope, final UUID courtCentreId) {

        final String crestCourtSiteCode = getDefaultCrestCourtSiteCode(envelope, courtCentreId);

        return getCrestCourtSitesForCourtCentre(envelope, courtCentreId)
                .stream().filter(courtSite -> crestCourtSiteCode.equals(courtSite.getString(CREST_COURT_SITE_CODE)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find site"));
    }
}
