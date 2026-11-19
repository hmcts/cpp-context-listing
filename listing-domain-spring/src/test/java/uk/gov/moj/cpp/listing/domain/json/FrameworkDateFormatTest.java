package uk.gov.moj.cpp.listing.domain.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Pins the date formats of the CJS framework (utilities-core 17.104.0 {@code ObjectMapperProducer}
 * and {@code ZonedDateTimes}, on the WAR's jackson-datatype-jsr310 2.12.7), MBD refactor P2.3.3.
 * The expected values were produced by that framework configuration on jsr310 2.12.7.
 */
class FrameworkDateFormatTest {

    public static class Dates {
        public ZonedDateTime zoned;
        public OffsetDateTime offset;
        public Instant instant;
        public LocalDate date;
        public LocalTime time;
        public LocalDateTime dateTime;
    }

    private final ObjectToJsonObjectConverter toJson = new ObjectToJsonObjectConverter();
    private final JsonObjectToObjectConverter fromJson = new JsonObjectToObjectConverter();

    @Test
    void serialisesAsTheFramework() {
        final Dates dates = new Dates();
        dates.zoned = ZonedDateTime.of(2026, 7, 19, 12, 20, 54, 123_456_789, ZoneId.of("Europe/London"));
        dates.offset = dates.zoned.toOffsetDateTime();
        dates.instant = dates.zoned.toInstant();
        dates.date = LocalDate.of(2026, 1, 2);
        dates.time = LocalTime.of(9, 5);
        dates.dateTime = LocalDateTime.of(2026, 1, 2, 9, 5);

        assertThat(toJson.convert(dates).toString()).isEqualTo(
                "{\"zoned\":\"2026-07-19T11:20:54.123Z\",\"offset\":\"2026-07-19T11:20:54.123456789Z\","
                        + "\"instant\":\"2026-07-19T11:20:54.123456789Z\",\"date\":\"2026-01-02\",\"time\":\"09:05:00\","
                        + "\"dateTime\":\"2026-01-02T09:05:00\"}");
    }

    @Test
    void writesZeroMillisecondsAndUtc() {
        final Dates dates = new Dates();
        dates.zoned = ZonedDateTime.of(2026, 7, 19, 12, 20, 54, 0, ZoneOffset.UTC);
        dates.offset = dates.zoned.toOffsetDateTime();
        dates.instant = dates.zoned.toInstant();

        assertThat(toJson.convert(dates).toString()).isEqualTo(
                "{\"zoned\":\"2026-07-19T12:20:54.000Z\",\"offset\":\"2026-07-19T12:20:54Z\",\"instant\":\"2026-07-19T12:20:54Z\"}");
        assertThat(ZonedDateTimes.toString(dates.zoned)).isEqualTo("2026-07-19T12:20:54.000Z");
    }

    @Test
    void deserialisesZonedDateTimeToTheRegionZoneUtcAsTheFramework() {
        for (final String value : new String[]{"2026-11-19T12:20:54.880Z", "2026-11-19T13:20:54.880+01:00",
                "2026-11-19T12:20:54.880Z[UTC]", "2026-11-19T12:20:54.880Z[Europe/London]"}) {
            final JsonObject json = Json.createObjectBuilder().add("zoned", value).add("offset", "2026-11-19T13:20:54.880+01:00").build();
            final Dates dates = fromJson.convert(json, Dates.class);

            assertThat(dates.zoned.getZone()).as(value).isEqualTo(ZoneId.of("UTC"));
            assertThat(dates.zoned.toString()).as(value).isEqualTo("2026-11-19T12:20:54.880Z[UTC]");
            assertThat(dates.offset.toString()).isEqualTo("2026-11-19T12:20:54.880Z");
            assertThat(toJson.convert(dates).getString("zoned")).isEqualTo("2026-11-19T12:20:54.880Z");
        }
    }

    @Test
    void zonedDateTimesHelperAsTheFramework() {
        final ZonedDateTime parsed = ZonedDateTimes.fromString("2026-11-19T13:20:54.123456+01:00");

        assertThat(parsed.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(parsed.toString()).isEqualTo("2026-11-19T12:20:54.123456Z");
        assertThat(ZonedDateTimes.toString(parsed)).isEqualTo("2026-11-19T12:20:54.123Z");
    }
}
