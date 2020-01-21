package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class RangeSearchConverter {

    @Inject
    private XhibitReferenceDataService xhibitReferenceDataService;

    public JsonObject generateCourtListQueryPayload(final JsonEnvelope envelope, final UUID courtCentreId, final JsonObject rangeSearchResponsePayload) {

        final List<FlatHearing> flatHearings = FlatHearingsConverter.generateFlatHearingList(rangeSearchResponsePayload.getJsonArray("hearings"));

        final List<Sitting> sittings = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings);

        final JsonObject crestCourtSiteJson = xhibitReferenceDataService.getCrestCourtSitesForCourtCentre(envelope, courtCentreId)
                .get(0);  // TODO SCSL-333 Use court room reference data to create lists for court sites under OU

        return createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", createArrayBuilder()
                        .add(courtSiteCourtList(crestCourtSiteJson, sittings))
                        .build())
                .build();
    }

    private JsonObject courtSiteCourtList(final JsonObject crestCourtSiteJson, final List<Sitting> sittings) {

        return Json.createObjectBuilder()
                .add("crestCourtSite", crestCourtSiteJson)
                .add("sittings", SittingsJsonGenerator.buildSittingsJson(sittings))
                .build();
    }
}
