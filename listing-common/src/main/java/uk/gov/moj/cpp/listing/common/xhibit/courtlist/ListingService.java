package uk.gov.moj.cpp.listing.common.xhibit.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

/**
 * Reads the court list that the Xhibit and PDDA documents are generated from. It is an interface so that this
 * module does not depend on the query view; the implementation sits next to the query view.
 */
public interface ListingService {

    JsonObject getUnpublishedCourtListForCourtCentre(JsonEnvelope envelope,
                                                     PublishCourtListRequestParameters publishCourtListRequestParameters);

    JsonObject getPublishedCourtListForCourtCentre(JsonEnvelope envelope,
                                                   UUID courtCentreId,
                                                   PublishCourtListType publishCourtListType,
                                                   LocalDate startDate);
}
