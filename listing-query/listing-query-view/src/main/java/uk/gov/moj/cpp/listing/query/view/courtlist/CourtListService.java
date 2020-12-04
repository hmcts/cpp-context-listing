package uk.gov.moj.cpp.listing.query.view.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.RangeSearchQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
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
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    public JsonObject retrieveUnPublishedCourtList(final UUID courtCentreId,
                                                   final PublishCourtListType publishCourtListType,
                                                   final LocalDate startDate,
                                                   final String endDate, final JsonEnvelope envelope) {

        final JsonEnvelope rangeSearchQueryEnvelope = rangeSearchQueryRequestFactory.buildRangeSearchQueryEnvelope(
                courtCentreId,
                publishCourtListType,
                startDate,
                envelope
        );

        final JsonEnvelope rangeSearchResponse = rangeSearchQuery.rangeSearchHearings(rangeSearchQueryEnvelope);

        return rangeSearchConverter.generateCourtListQueryPayload(courtCentreId, rangeSearchResponse.payloadAsJsonObject(), startDate, endDate);
    }

    public JsonObject emptyCourtList(final UUID courtCentreId) {

        final List<JsonObject> courtSites = commonXhibitReferenceDataService.getCrestCourtSitesForCrownCourtCentre(courtCentreId);

        final JsonArrayBuilder courtListsBuilder = Json.createArrayBuilder();

        courtSites.forEach(courtSiteJson -> courtListsBuilder.add(Json.createObjectBuilder()
                .add("crestCourtSite", courtSiteJson)
                .add("sittings", Json.createArrayBuilder().build())
                .build())
        );

        return Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", courtListsBuilder)
                .build();
    }
}
