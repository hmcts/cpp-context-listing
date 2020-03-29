package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.InvalidReferenceDataException;

import javax.json.JsonObject;

public class XhibitReferenceDataValidator {

    public void validate(final String propertyName, final String propertyValue, final JsonObject parentObject) {
        if (isBlank(propertyValue)) {
            throw new InvalidReferenceDataException(format("Invalid value '%s' for '%s' in '%s'", propertyValue, propertyName, parentObject));
        }
    }
}
