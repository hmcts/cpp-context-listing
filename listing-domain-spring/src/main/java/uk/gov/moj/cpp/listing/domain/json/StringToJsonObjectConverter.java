package uk.gov.moj.cpp.listing.domain.json;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/** Framework-free replacement for {@code uk.gov.justice.services.common.converter.StringToJsonObjectConverter}. */
public class StringToJsonObjectConverter {

    public JsonObject convert(final String source) {
        try (JsonReader reader = Json.createReader(new StringReader(source))) {
            return reader.readObject();
        }
    }
}
