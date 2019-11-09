package uk.gov.moj.cpp.listing.event.processor.xhibit;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;

import java.util.UUID;

public class XhibitReferenceDataService {

    @SuppressWarnings("squid:S1172") // Use envelope when fully implemented
    public CourtLocation getCourtDetails(final Envelope envelope, final UUID courtCentreId) {
        // TODO Implemented when SCRD-512 is ready
        return new CourtLocation("000", "DUMMYCOURTNAME", "DUMMY", "DUMMYSITECODE");
    }
}
