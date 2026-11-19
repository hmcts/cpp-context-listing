package uk.gov.moj.cpp.listing.query.view.courtlist;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

@RequiredArgsConstructor
@Component
public class RangeSearchConverter {

    private final CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    public JsonObject generateCourtListQueryPayload(final UUID courtCentreId, final JsonObject rangeSearchResponsePayload, final LocalDate startDate, final String endDate) {

        final List<FlatHearing> flatHearings = FlatHearingsConverter.generateFlatHearingList(rangeSearchResponsePayload.getJsonArray("hearings"));

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(commonXhibitReferenceDataService)
                .prepareEmptyCourtSiteHearings(courtCentreId)
                .assignHearingsToCourtSitesUsingCourtRoom(courtCentreId,flatHearings)
                .groupFlatHearingsIntoSittings(startDate, endDate)
                .buildCourtListsArray(courtCentreId);

        return createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", courtLists)
                .build();
    }
}
