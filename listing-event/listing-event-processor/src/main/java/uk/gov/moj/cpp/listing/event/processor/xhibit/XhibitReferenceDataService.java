package uk.gov.moj.cpp.listing.event.processor.xhibit;

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

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @SuppressWarnings("squid:S1172") // Use envelope when fully implemented
    public CourtLocation getCourtDetails(final Envelope envelope, final UUID courtCentreId) {
        // TODO Implemented when SCRD-512 is ready
        return new CourtLocation("000", "DUMMYCOURTNAME", "DUMMY", "DUMMYSITECODE", "CROWN_COURT");
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1172"})    // Remove when implemented
    public int getCourtRoomNumber(final Envelope envelope, final UUID courtRoomId) {

//        final JsonObject queryParameters = createObjectBuilder().add("id", courtRoomId.toString()).build();
//
//        final JsonEnvelope response = requester.request(envelop(queryParameters).withName("referencedata.query.courtroom").withMetadataFrom(envelope));

        // TODO Implemented when SCRD-567 is ready
        return 0;
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1172"})    // Remove when implemented
    public JsonObject getJudiciary(final Envelope envelope, final UUID judiciaryId) {

        //        final JsonObject queryParameters = createObjectBuilder().add("id", courtRoomId.toString()).build();
//
//        final JsonEnvelope response = requester.request(envelop(queryParameters).withName("referencedata.query.courtroom").withMetadataFrom(envelope));

        return Json.createObjectBuilder().build();  // TODO Implement
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1172"})    // Remove when implemented
    public JsonObject getJudge(final JsonEnvelope envelope, final UUID judgeId) {
        return Json.createObjectBuilder().build();  // TODO Implement
    }
}
