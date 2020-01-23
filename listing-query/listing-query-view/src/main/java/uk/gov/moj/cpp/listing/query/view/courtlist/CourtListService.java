package uk.gov.moj.cpp.listing.query.view.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.RangeSearchQuery;

import java.time.LocalDate;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@ApplicationScoped
public class CourtListService {

    @Inject
    private RangeSearchQueryRequestFactory rangeSearchQueryRequestFactory;

    @Inject
    private RangeSearchConverter rangeSearchConverter;

    @Inject
    private RangeSearchQuery rangeSearchQuery;

    @Inject
    private XhibitReferenceDataService xhibitReferenceDataService;

    public JsonObject retrieveUnPublishedCourtList(final UUID courtCentreId,
                                                   final PublishCourtListType publishCourtListType,
                                                   final LocalDate startDate,
                                                   final JsonEnvelope envelope) {

        final JsonEnvelope rangeSearchQueryEnvelope = rangeSearchQueryRequestFactory.buildRangeSearchQueryEnvelope(
                courtCentreId,
                publishCourtListType,
                startDate,
                envelope
        );

        final JsonEnvelope rangeSearchResponse = rangeSearchQuery.rangeSearchHearings(rangeSearchQueryEnvelope);

        return rangeSearchConverter.generateCourtListQueryPayload(envelope, courtCentreId, rangeSearchResponse.payloadAsJsonObject());
    }

    public JsonObject emptyCourtList(final JsonEnvelope envelope, final UUID courtCentreId) {
        return Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", Json.createArrayBuilder().add(
                        Json.createObjectBuilder()
                                .add("crestCourtSite", xhibitReferenceDataService.getCrestCourtSiteJson(envelope, courtCentreId))
                                .add("sittings", Json.createArrayBuilder().build())
                                .build()
                        ).build()
                ).build();
    }
}
