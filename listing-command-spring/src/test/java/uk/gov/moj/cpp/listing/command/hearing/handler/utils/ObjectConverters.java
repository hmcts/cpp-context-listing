package uk.gov.moj.cpp.listing.command.hearing.handler.utils;


import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.io.IOException;

import jakarta.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectConverters {

    private ObjectConverters() {

    }

    public static <T> T convert(Class<T> clazz, JsonObject source) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        try {
            final T object = mapper.readValue(mapper.writeValueAsString(source), clazz);
            if (object == null) {
                throw new IllegalArgumentException(String.format("Failed to convert %s to Object", source));
            } else {
                return object;
            }
        } catch (IOException var4) {
            throw new IllegalArgumentException(String.format("Error while converting %s to JsonObject", source), var4);
        }
    }

    public static <T> T asPojo(Envelope jsonEnvelope, Class<T> clazz) {
        return convert(clazz, jsonEnvelope.payload());
    }
}
