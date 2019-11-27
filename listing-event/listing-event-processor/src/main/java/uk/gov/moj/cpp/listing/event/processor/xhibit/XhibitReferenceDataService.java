package uk.gov.moj.cpp.listing.event.processor.xhibit;

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
import javax.json.Json;
import javax.json.JsonObject;

public class XhibitReferenceDataService {

    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final String REFERENCEDATA_QUERY_JUDGE = "referencedata.get.judge";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @SuppressWarnings("squid:S1172") // Use envelope when fully implemented
    public CourtLocation getCourtDetails(final Envelope envelope, final UUID courtCentreId) {
        // TODO Implemented when SCRD-512 is ready
        return new CourtLocation("000", "DUMMYCOURTNAME", "DUMMY", "DUMMYSITECODE", "CROWN_COURT");
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1172"})    // Remove when implemented
    public int getCourtRoomNumber(final JsonEnvelope envelope, final UUID courtRoomId) {

//        final JsonObject queryParameters = createObjectBuilder().add("id", courtRoomId.toString()).build();
//
//        final JsonEnvelope response = requester.request(envelop(queryParameters).withName("referencedata.query.courtroom").withMetadataFrom(envelope));

        // TODO Implemented when SCRD-567 is ready
        return 0;
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

    @SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1172"})    // Remove when implemented
    public JsonObject getXhibitHearingType(final Envelope envelope, final UUID cppHearingTypeId) {
        return Json.createObjectBuilder()
                .add("hearingCode", "XXX")   // TODO SCSL-85
                .add("hearingDescription", "XHIBIT_HEARING_DESCRIPTION")
                .build();
    }
}
