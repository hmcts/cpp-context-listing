package uk.gov.moj.cpp.listing.domain.json;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Framework-free replacement for {@code uk.gov.justice.services.common.converter.LocalDates}. */
public final class LocalDates {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private LocalDates() {
    }

    public static LocalDate from(final String date) {
        return LocalDate.parse(date, FORMAT);
    }

    public static String to(final LocalDate date) {
        return date.format(FORMAT);
    }
}
