package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class RangeSearchConverter {

    public JsonObject generateCourtListQueryPayload(final UUID courtCentreId, final JsonObject rangeSearchResponsePayload) {

        final List<FlatHearing> flatHearings = FlatHearingsConverter.generateFlatHearingList(rangeSearchResponsePayload.getJsonArray("hearings"));

        final List<Sitting> sittings = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings);

        return createObjectBuilder()
                .add("courtList", Json.createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("sittings", SittingsJsonGenerator.buildSittingsJson(sittings)))
                .build();
    }
}
