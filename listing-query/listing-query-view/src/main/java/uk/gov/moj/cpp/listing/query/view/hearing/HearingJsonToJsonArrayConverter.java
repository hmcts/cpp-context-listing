package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

public class HearingJsonToJsonArrayConverter implements Converter<Hearing, JsonArray> {

    private static final String JUDICIARY = "judiciary";
    private static final String HEARINGS = "hearings";
    private static final JsonArray EMPTY_JSON_ARRAY = Json.createArrayBuilder().build();

    @Override
    public JsonArray convert(final Hearing hearing) {
        if(hearing!=null){
            JsonObject publicHearingResult = this.jsonFromString(hearing.getProperties().toString());
            JsonArray judiciary = publicHearingResult.isNull(JUDICIARY) ?  EMPTY_JSON_ARRAY : publicHearingResult.getJsonArray(JUDICIARY);
            if(publicHearingResult.isNull(HEARINGS)) {
                return EMPTY_JSON_ARRAY;
            }
            return publicHearingResult.getJsonArray(HEARINGS).getValuesAs(JsonObject.class).stream()
                    .map(hearingByCourtCentreId -> this.enrich(hearingByCourtCentreId, JUDICIARY, judiciary))
                    .collect(JsonArrayCollector.toArrayNode());

        }
        return EMPTY_JSON_ARRAY;
    }

    private JsonObject jsonFromString(String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }

    private JsonObject enrich(JsonObject source, String key, JsonArray value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, value);
        source.entrySet().
                forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }



}
