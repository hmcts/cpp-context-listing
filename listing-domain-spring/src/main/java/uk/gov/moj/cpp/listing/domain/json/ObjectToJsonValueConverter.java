package uk.gov.moj.cpp.listing.domain.json;

import java.io.StringReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/** Framework-free replacement for {@code uk.gov.justice.services.common.converter.ObjectToJsonValueConverter}. */
public class ObjectToJsonValueConverter {

    private final ObjectMapper mapper;

    public ObjectToJsonValueConverter() {
        this(new ObjectMapperProducer().objectMapper());
    }

    public ObjectToJsonValueConverter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonValue convert(final Object source) {
        try (JsonReader reader = Json.createReader(new StringReader(mapper.writeValueAsString(source)))) {
            return reader.readValue();
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Error while converting " + source.getClass().getName() + " to JsonValue", e);
        }
    }
}
