package uk.gov.moj.cpp.listing.testutil;

import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ListToJsonArrayConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.StringToJsonObjectConverter;

/** Test-only replacement for the framework's JsonObjectConvertersFactory. */
public class JsonConverters {

    public ObjectToJsonObjectConverter objectToJsonObjectConverter() {
        return new ObjectToJsonObjectConverter();
    }

    public JsonObjectToObjectConverter jsonObjectToObjectConverter() {
        return new JsonObjectToObjectConverter();
    }

    public StringToJsonObjectConverter stringToJsonObjectConverter() {
        return new StringToJsonObjectConverter();
    }

    @SuppressWarnings("rawtypes")
    public ListToJsonArrayConverter listToJsonArrayConverter() {
        return new ListToJsonArrayConverter<>();
    }
}
