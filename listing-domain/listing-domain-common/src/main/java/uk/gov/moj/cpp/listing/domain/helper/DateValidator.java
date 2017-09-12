package uk.gov.moj.cpp.listing.domain.helper;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonObjects;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateValidator.class);

    private DateValidator() {
    }

    public static boolean isDateRangeValid(final JsonObject payload) {
        final Optional<String> fromDate = JsonObjects.getString(payload, "fromDate");
        final Optional<String> toDate = JsonObjects.getString(payload, "toDate");

        if (fromDate.isPresent() && LocalDates.from(fromDate.get()).isAfter(LocalDate.now())) {
            return false;
        }
        if (toDate.isPresent() && LocalDates.from(toDate.get()).isAfter(LocalDate.now())) {
            return false;
        }
        return !(fromDate.isPresent() && toDate.isPresent())
                || !LocalDates.from(fromDate.get()).isAfter(LocalDates.from(toDate.get()));
    }

    public static boolean isDateFormatValid(final String date) {
        try {
            LocalDates.from(date);
        } catch (final DateTimeParseException e) {
            LOGGER.warn("Invalid Date supplied: {} and exception is {}", date, e);
            return false;
        }
        return true;
    }
}
