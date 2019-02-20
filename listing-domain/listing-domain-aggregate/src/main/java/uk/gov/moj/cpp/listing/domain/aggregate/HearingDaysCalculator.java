package uk.gov.moj.cpp.listing.domain.aggregate;


import static java.util.Collections.emptyList;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;

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

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");

    private HearingDaysCalculator() {
    }


    public static List<HearingDay> calculate(LocalDate startDate, LocalDate endDate, List<LocalDate> nonSittingDays,
                                       List<NonDefaultDay> nonDefaultDays,
                                       LocalTime defaultStartTime,
                                       Integer defaultDuration) {

        if (startDate == null || endDate == null) {
            return emptyList();
        }
        final Map<LocalDate, NonDefaultDay> nonDefaultDayMap = nonDefaultDays.stream()
                .collect(Collectors.toMap(ndd -> ndd.getStartTime().toLocalDate(), ndd -> ndd));

        final long noOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        return IntStream.rangeClosed(0, (int) noOfDaysBetween)
                .mapToObj(i -> startDate.plusDays(i))
                .filter(d -> !nonSittingDays.contains(d))
                .map(date ->
                        nonDefaultDayMap.containsKey(date)
                                ? buildNonDefaultHearingDay(nonDefaultDayMap, date, defaultDuration)
                                : buildDefaultHearingDay(defaultStartTime, defaultDuration, date)
                )
                .collect(Collectors.toList());

    }

    private static HearingDay buildDefaultHearingDay(LocalTime defaultStartTime, Integer defaultDuration, LocalDate date) {
        final LocalTime endTime = defaultStartTime.plusMinutes(defaultDuration);

        return HearingDay.hearingDay()
                .withHearingDate(date)
                .withStartTime(ZonedDateTime.of(date, defaultStartTime, BST)
                        .withZoneSameInstant(UTC))
                .withDurationMinutes(defaultDuration)
                .withSequence(0)
                .withEndTime(ZonedDateTime.of(date, endTime, BST).withZoneSameInstant(UTC))
                .build();
    }

    private static HearingDay buildNonDefaultHearingDay(Map<LocalDate, NonDefaultDay> startTimesMap, LocalDate localDate, Integer defaultDuration) {
        final NonDefaultDay nonDefaultDay = startTimesMap.get(localDate);
        final Integer durationMinutes = nonDefaultDay.getDuration().orElse(defaultDuration);
        final ZonedDateTime endDateTime = nonDefaultDay.getStartTime().plusMinutes(durationMinutes);

        return HearingDay.hearingDay()
                .withHearingDate(nonDefaultDay.getStartTime().toLocalDate())
                .withStartTime(nonDefaultDay.getStartTime())
                .withDurationMinutes(durationMinutes)
                .withSequence(0)
                .withEndTime(endDateTime)
                .build();
    }

}
