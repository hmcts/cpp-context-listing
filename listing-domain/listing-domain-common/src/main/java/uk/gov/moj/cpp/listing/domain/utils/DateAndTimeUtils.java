package uk.gov.moj.cpp.listing.domain.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.math.NumberUtils;

public class DateAndTimeUtils {
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final ZoneId BST = ZoneId.of("Europe/London");

    private static final Set<DayOfWeek> WEEKDAYS = ImmutableSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);

    private DateAndTimeUtils() {
    }

    public static LocalDate getNextWorkingDay(final LocalDate date) {
        final LocalDate candidateLocalDate = date.plusDays(1);
        if (WEEKDAYS.contains(candidateLocalDate.getDayOfWeek())) {
            return candidateLocalDate;
        } else {
            return getNextWorkingDay(candidateLocalDate);
        }
    }

    /**
     * The supplied duration is presume to be HH:MM; for example: 1:45, which should be converted
     * into 95 minutes.
     *
     * @param rawDurationInHoursAndMinutes
     * @return The number of minutes, if we can determine it.
     */
    public static Optional<Integer> convertHoursAndMinutesToMinutes(final String rawDurationInHoursAndMinutes) {
        if (rawDurationInHoursAndMinutes == null || rawDurationInHoursAndMinutes.trim().isEmpty()) {
            return Optional.empty();
        }

        final String[] hoursAndMinutes = rawDurationInHoursAndMinutes.split(":");
        final String rawHours = correctRawHours(hoursAndMinutes[0]);

        if (!NumberUtils.isNumber(rawHours)) {
            return Optional.empty();
        }

        final int hours = Integer.parseInt(rawHours);
        final int minutesInTheHours = hours * 60;
        if (hoursAndMinutes.length == 1) {
            return Optional.of(minutesInTheHours);
        }

        final String rawMinutes = hoursAndMinutes[1];
        if (!NumberUtils.isNumber(rawMinutes)) {
            return Optional.empty();
        }
        return Optional.of(minutesInTheHours + Integer.parseInt(rawMinutes));

    }

    public static ZonedDateTime convertUTCToLocalTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(BST);
    }

    public static String toIsoString(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }

        return zonedDateTime.format(ISO_8601_FORMATTER);
    }

    public static String toIsoString(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }

        return localDate.format(ISO_8601_FORMATTER);
    }

    private static String correctRawHours(final String rawHours) {

        return rawHours.trim().isEmpty() ? "0" : rawHours.trim();
    }
}
