package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class RangeSearchConverter {

    @Inject
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    public JsonObject generateCourtListQueryPayload(final UUID courtCentreId, final JsonObject rangeSearchResponsePayload, final LocalDate startDate, final String endDate) {

        final List<FlatHearing> flatHearings = FlatHearingsConverter.generateFlatHearingList(rangeSearchResponsePayload.getJsonArray("hearings"));

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(courtCentreId, commonXhibitReferenceDataService)
                .prepareEmptyCourtSiteHearings()
                .assignHearingsToCourtSitesUsingCourtRoom(flatHearings)
                .groupFlatHearingsIntoSittings(startDate, endDate)
                .buildCourtListsArray();

        return createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", courtLists)
                .build();
    }
}
