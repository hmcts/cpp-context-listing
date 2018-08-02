package uk.gov.moj.cpp.listing.domain.aggregate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HearingDaysCalculator {

    private static final LocalTime DEFAULT_TIME = LocalTime.of(10, 30);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");

    private HearingDaysCalculator() {
    }

    public static List<ZonedDateTime> calculate(LocalDate startDate,
                                                LocalDate endDate,
                                                List<LocalDate> nonSittingDays,
                                                List<ZonedDateTime> startTimes) {

        final Map<LocalDate, LocalTime> startTimesMap = startTimes.stream().collect(
                Collectors.toMap(ZonedDateTime::toLocalDate, ZonedDateTime::toLocalTime));

        final long noOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        final List<ZonedDateTime> hearingDays = IntStream.rangeClosed(0, (int) noOfDaysBetween)
                .mapToObj(i -> startDate.plusDays(i))
                .filter(d -> !nonSittingDays.contains(d))
                .map(d ->
                        startTimesMap.containsKey(d)
                                ? ZonedDateTime.of(d, startTimesMap.get(d), UTC)
                                : ZonedDateTime.of(d, DEFAULT_TIME, BST).withZoneSameInstant(UTC)
                )
                .collect(Collectors.toList());
        return hearingDays;
    }
}
