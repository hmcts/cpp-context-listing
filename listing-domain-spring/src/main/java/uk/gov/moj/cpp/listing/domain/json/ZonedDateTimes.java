package uk.gov.moj.cpp.listing.domain.json;

import static java.time.ZoneOffset.UTC;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.json.JsonString;

/** Framework-free replacement for {@code uk.gov.justice.services.common.converter.ZonedDateTimes} (same formats). */
public final class ZonedDateTimes {

    public static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private ZonedDateTimes() {
    }

    public static ZonedDateTime fromJsonString(final JsonString jsonString) {
        return fromString(jsonString.getString());
    }

    public static String toString(final ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(UTC).format(DateTimeFormatter.ofPattern(ISO_8601));
    }

    public static ZonedDateTime fromString(final String dateTime) {
        return ZonedDateTime.parse(dateTime).withZoneSameInstant(UTC);
    }

    public static Timestamp toSqlTimestamp(final ZonedDateTime dateTime) {
        return Timestamp.from(dateTime.toInstant());
    }

    public static ZonedDateTime fromSqlTimestamp(final Timestamp timestamp) {
        return ZonedDateTime.ofInstant(timestamp.toInstant(), UTC);
    }
}
