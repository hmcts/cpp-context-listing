package uk.gov.moj.cpp.listing.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Test-only replacement for hibernate-types' JacksonUtil. */
public final class JacksonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JacksonUtil() {
    }

    public static JsonNode toJsonNode(final String json) {
        try {
            return MAPPER.readTree(json);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
