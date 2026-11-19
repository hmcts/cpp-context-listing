package uk.gov.moj.cpp.listing.domain.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;

/** Framework-free replacement for {@code uk.gov.justice.services.common.converter.JsonObjectToObjectConverter}. */
public class JsonObjectToObjectConverter {

    private final ObjectMapper mapper;

    public JsonObjectToObjectConverter() {
        this(new ObjectMapperProducer().objectMapper());
    }

    public JsonObjectToObjectConverter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <R> R convert(final JsonObject source, final Class<R> clazz) {
        try {
            final R object = mapper.readValue(mapper.writeValueAsString(source), clazz);
            if (object == null) {
                throw new IllegalArgumentException("Failed to convert JsonObject to " + clazz.getName());
            }
            return object;
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error while converting JsonObject to " + clazz.getName(), e);
        }
    }
}
