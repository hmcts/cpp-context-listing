package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class XhibitReferenceDataService {

    private static final String REFERENCEDATA_QUERY_XHIBIT_COURTROOM_MAPPINGS = "referencedata.query.cp-xhibit-courtroom-mappings";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final String REFERENCEDATA_QUERY_JUDGE = "referencedata.get.judge";
    private static final String REFERENCE_DATA_HEARING_TYPES = "referencedata.query.hearing-types";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @SuppressWarnings("squid:S1172") // Use envelope when fully implemented
    public CourtLocation getCourtDetails(final Envelope envelope, final UUID courtCentreId) {
        // TODO Implemented when SCRD-512 is ready
        return new CourtLocation("000", "DUMMYCOURTNAME", "DUMMY", "DUMMYSITECODE", "CROWN_COURT");
    }

    public int getCourtRoomNumber(final JsonEnvelope envelope, final UUID courtCentreId, final UUID courtRoomId) {

        final JsonObject queryParameters = createObjectBuilder().add("id", courtCentreId.toString()).build();

        final JsonObject courtRoom = requester.request(envelop(queryParameters).withName(REFERENCEDATA_QUERY_COURTROOM)
                .withMetadataFrom(envelope))
                .payloadAsJsonObject().getJsonArray("courtrooms").getValuesAs(JsonObject.class)
                .stream().filter(c -> UUID.fromString(c.getString("id")).equals(courtRoomId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(format("Cannot find court room with id %s in courtCentre %s", courtRoomId, courtCentreId)));

        return courtRoom.getInt("courtroomId");
    }

    public JsonObject getJudiciary(final JsonEnvelope envelope, final UUID judiciaryId) {

        final JsonObject queryParameters = createObjectBuilder().add("ids", judiciaryId.toString()).build();

        return requester.request(envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_JUDICIARIES)
                .withMetadataFrom(envelope))
                .payloadAsJsonObject().getJsonArray("judiciaries")
                .getValuesAs(JsonObject.class).get(0);

    }

    public JsonObject getJudge(final JsonEnvelope envelope, final UUID judgeId) {

        final JsonObject queryParameters = createObjectBuilder().add("id", judgeId.toString()).build();

        return requester.request(envelop(queryParameters)
                .withName(REFERENCEDATA_QUERY_JUDGE)
                .withMetadataFrom(envelope))
                .payloadAsJsonObject();

    }

    public JsonObject getXhibitHearingType(final JsonEnvelope envelope, final UUID cppHearingTypeId) {

        return requester.request(envelop(createObjectBuilder().build())
                .withName(REFERENCE_DATA_HEARING_TYPES)
                .withMetadataFrom(envelope))
                .payloadAsJsonObject()
                .getJsonArray("hearingTypes").getValuesAs(JsonObject.class).stream()
                .filter(h -> UUID.fromString(h.getString("id")).equals(cppHearingTypeId))
                .findFirst().orElseThrow(() -> new RuntimeException(format("Cannot find hearing type %s", cppHearingTypeId)));

    }

}
