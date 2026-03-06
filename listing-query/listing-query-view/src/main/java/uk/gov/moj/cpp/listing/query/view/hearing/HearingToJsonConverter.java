package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.StringReader;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class HearingToJsonConverter {

    private HearingToJsonConverter() {
    }

    public static JsonObject convert(final Hearing hearing) {
        if (hearing.getProperties() == null) {
            return null;
        }
        return jsonFromString(hearing.getProperties().toString());
    }

    private static JsonObject jsonFromString(final String rawJson) {
        JsonObject object;
        try (JsonReader jsonReader = JsonObjects.createReader(new StringReader(rawJson))) {
            object = jsonReader.readObject();
            return object;
        }

    }
}
