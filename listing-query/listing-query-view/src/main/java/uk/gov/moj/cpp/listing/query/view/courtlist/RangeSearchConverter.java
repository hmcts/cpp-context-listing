package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

public class RangeSearchConverter {

    public JsonObject generateCourtListQueryPayload(final JsonObject rangeSearchResponsePayload) {

        final List<Sitting> sittings = SittingsPojoBuilder.assignHearingsToSittings(rangeSearchResponsePayload.getJsonArray("hearings"));

        return createObjectBuilder()
                .add("courtList", Json.createObjectBuilder()
                        .add("sittings", SittingsJsonGenerator.buildSittingsJson(sittings)))
                .build();
    }
}
