package uk.gov.moj.cpp.listing.domain.json;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;

/** Framework-free replacement for {@code uk.gov.justice.services.common.converter.ListToJsonArrayConverter}. */
public class ListToJsonArrayConverter<T> {

    private final ObjectToJsonValueConverter delegate;

    public ListToJsonArrayConverter() {
        this(new ObjectMapperProducer().objectMapper());
    }

    public ListToJsonArrayConverter(final ObjectMapper mapper) {
        this.delegate = new ObjectToJsonValueConverter(mapper);
    }

    public JsonArray convert(final List<T> source) {
        return (JsonArray) delegate.convert(source);
    }
}
