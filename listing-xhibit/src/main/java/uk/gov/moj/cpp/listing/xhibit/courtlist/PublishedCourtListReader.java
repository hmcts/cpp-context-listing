package uk.gov.moj.cpp.listing.xhibit.courtlist;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.springframework.stereotype.Component;

/**
 * The court-list JSON the export pipeline reads (port of the JEE processor's {@code ListingService}).
 * The JEE processor called the query view in-VM ({@code listing.courtlist}); here it is a direct
 * call to the same, ported, {@link HearingQueryView#retrieveCourtList} (§5).
 */
@Component
public class PublishedCourtListReader {

    private final HearingQueryView hearingQueryView;

    public PublishedCourtListReader(final HearingQueryView hearingQueryView) {
        this.hearingQueryView = hearingQueryView;
    }

    /** The court list built live from the viewstore ({@code published} absent). */
    public JsonObject getUnpublishedCourtListForCourtCentre(final PublishCourtListRequestParameters parameters) {
        return hearingQueryView.retrieveCourtList(Json.createObjectBuilder()
                .add("courtCentreId", parameters.getCourtCentreId().toString())
                .add("startDate", parameters.getStartDate().toString())
                .add("publishCourtListType", parameters.getPublishCourtListType().name())
                .add("endDate", parameters.getEndDate().toString())
                .build());
    }

    /** The court list last stored in {@code published_court_list} ({@code published=true}). */
    public JsonObject getPublishedCourtListForCourtCentre(final UUID courtCentreId,
                                                          final PublishCourtListType publishCourtListType,
                                                          final LocalDate startDate) {
        return hearingQueryView.retrieveCourtList(Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", startDate.toString())
                .add("publishCourtListType", publishCourtListType.name())
                .add("published", true)
                .build());
    }
}
