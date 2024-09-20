package uk.gov.moj.cpp.listing.domain.utils;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class DateValidatorTest {

    private static JsonObject getJsonObject(final String fromDate, final String toDate) {
        return Json.createObjectBuilder().add("fromDate", fromDate).add("toDate", toDate).build();
    }

    @Test
    public void shouldReturnTrueIfValidDateRange() throws Exception {

        final JsonObject jsonObject = getJsonObject("2012-05-05", "2016-05-05");

        final boolean isValid = DateValidator.isDateRangeValid(jsonObject);

        assertTrue(isValid);
    }

    @Test
    public void shouldReturnFalseIfInValidFromDates() throws Exception {

        final JsonObject jsonObject = getJsonObject("2120-05-05", "2016-05-05");

        final boolean isValid = DateValidator.isDateRangeValid(jsonObject);

        assertFalse(isValid);
    }

    @Test
    public void shouldReturnFalseIfInValidToDates() throws Exception {

        final JsonObject jsonObject = getJsonObject("2015-05-05", "2120-05-05");

        final boolean isValid = DateValidator.isDateRangeValid(jsonObject);

        assertFalse(isValid);
    }

    @Test
    public void shouldReturnFalseIfInValidDateRange() throws Exception {

        final JsonObject jsonObject = getJsonObject("2012-05-05", "2011-05-05");

        final boolean isValid = DateValidator.isDateRangeValid(jsonObject);

        assertFalse(isValid);
    }

    @Test
    public void shouldReturnFalseIfInValidDateFormat() throws Exception {
        final boolean isValid = DateValidator.isDateFormatValid("201205-05");

        assertFalse(isValid);
    }

    @Test
    public void shouldReturnTrueIfValidDateFormat() throws Exception {
        final boolean isValid = DateValidator.isDateFormatValid("2012-05-05");

        assertTrue(isValid);
    }
}