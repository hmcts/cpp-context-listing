package uk.gov.moj.cpp.listing.query.view.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.RangeSearchQuery;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ApplicationScoped
public class CourtListService {

    private static final Set<PublishCourtListType> EX_PARTE_FILTERED_LIST_TYPES = Set.of(PublishCourtListType.WARN, PublishCourtListType.FIRM);

    private static final String HEARINGS = "hearings";

    @Inject
    private RangeSearchQueryRequestFactory rangeSearchQueryRequestFactory;

    @Inject
    private RangeSearchConverter rangeSearchConverter;

    @Inject
    private RangeSearchQuery rangeSearchQuery;

    @Inject
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @Inject
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

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

        final JsonObject rangeSearchResponsePayload = EX_PARTE_FILTERED_LIST_TYPES.contains(publishCourtListType)
                ? filterExParteOffences(rangeSearchResponse.payloadAsJsonObject())
                : rangeSearchResponse.payloadAsJsonObject();

        return rangeSearchConverter.generateCourtListQueryPayload(courtCentreId, rangeSearchResponsePayload, startDate, endDate);
    }

    private JsonObject filterExParteOffences(final JsonObject rangeSearchResponsePayload) {
        if (!rangeSearchResponsePayload.containsKey(HEARINGS) || rangeSearchResponsePayload.isNull(HEARINGS)) {
            return rangeSearchResponsePayload;
        }

        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
        rangeSearchResponsePayload.forEach((key, value) -> {
            if (HEARINGS.equals(key)) {
                builder.add(key, hearingJsonListConverterFilterEjectCases.filterExParteOffencesFromHearings(rangeSearchResponsePayload.getJsonArray(HEARINGS)));
            } else {
                builder.add(key, value);
            }
        });
        return builder.build();
    }

    public JsonObject emptyCourtList(final UUID courtCentreId) {

        final List<JsonObject> courtSites = commonXhibitReferenceDataService.getCrestCourtSitesForCrownCourtCentre(courtCentreId);

        final JsonArrayBuilder courtListsBuilder = JsonObjects.createArrayBuilder();

        courtSites.forEach(courtSiteJson -> courtListsBuilder.add(JsonObjects.createObjectBuilder()
                .add("crestCourtSite", courtSiteJson)
                .add("sittings", JsonObjects.createArrayBuilder().build())
                .build())
        );

        return JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", courtListsBuilder)
                .build();
    }
}
