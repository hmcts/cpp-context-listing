package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;

import javax.json.JsonObject;

public class RangeSearchConverter {

    public JsonObject generateCourtListResponse(final JsonObject rangeSearchResponsePayload) {

        return createObjectBuilder()
                .add("courtList", rangeSearchResponsePayload)   // TODO SCSL-89 convert object
                .build();
    }
}
