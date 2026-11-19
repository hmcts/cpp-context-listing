package uk.gov.moj.cpp.listing.domain.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;

/** Framework-free replacement for {@code uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter}. */
public class ObjectToJsonObjectConverter {

    private final ObjectToJsonValueConverter delegate;

    public ObjectToJsonObjectConverter() {
        this(new ObjectMapperProducer().objectMapper());
    }

    public ObjectToJsonObjectConverter(final ObjectMapper mapper) {
        this.delegate = new ObjectToJsonValueConverter(mapper);
    }

    public JsonObject convert(final Object source) {
        return (JsonObject) delegate.convert(source);
    }
}
