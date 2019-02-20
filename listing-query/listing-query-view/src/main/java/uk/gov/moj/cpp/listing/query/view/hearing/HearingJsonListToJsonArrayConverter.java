package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;

public class HearingJsonListToJsonArrayConverter implements Converter<List<Hearing>, JsonArray> {

    @Override
    public JsonArray convert(final List<Hearing> hearings) {
        return hearings.stream()
                .map(Hearing::getProperties)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.toString()) )
                .collect(JsonArrayCollector.toArrayNode());
    }

    private JsonObject jsonFromString(String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }

    public JsonArray convertHearingResult(final List<Hearing> hearings) {
        return hearings.stream().filter(hearing -> Objects.nonNull(hearing.getProperties()) )
                .map(Hearing::getProperties)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.get(0).toString()) )
                .collect(JsonArrayCollector.toArrayNode());
    }
}
