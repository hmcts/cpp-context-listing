package uk.gov.moj.cpp.listing.domain.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ZonedDateTimeFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZonedDateTimeFormatter.class);
    private static final ZoneId zid = ZoneId.of("Europe/London");

    private ZonedDateTimeFormatter() {
    }

    public static ZonedDateTime adjustDateTime(final ZonedDateTime zonedDateTime) {
        try {
            return zonedDateTime.plusSeconds(
                    zonedDateTime.withZoneSameInstant(zid).getOffset().getTotalSeconds());
        } catch (DateTimeException dte) {
            LOGGER.warn("Invalid Date supplied: {} and exception is {}", zonedDateTime, dte);
            return zonedDateTime;
        }
    }
}