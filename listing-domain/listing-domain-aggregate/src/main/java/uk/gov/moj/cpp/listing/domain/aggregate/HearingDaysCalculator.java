package uk.gov.moj.cpp.listing.domain.aggregate;


import static java.util.Collections.emptyList;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class HearingDaysCalculator {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");

    private HearingDaysCalculator() {
    }

    public static List<HearingDay> calculate(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays,
                                             final List<NonDefaultDay> nonDefaultDays,
                                             final LocalTime defaultStartTime,
                                             final Integer defaultDuration) {

        return calculate(startDate, endDate, nonSittingDays, nonDefaultDays, defaultStartTime, defaultDuration, false);
    }

    @SuppressWarnings({"squid:S3358", "squid:S3776"})
    public static List<HearingDay> calculate(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays,
                                             final List<NonDefaultDay> nonDefaultDays,
                                             final LocalTime defaultStartTime,
                                             final Integer defaultDuration, final Boolean isCountBasedSlotSelected) {

        if (startDate == null || endDate == null) {
            return emptyList();
        }
        final Map<LocalDate, NonDefaultDay> nonDefaultDayMap = new HashMap<>();
        for (int i = 0; i < nonDefaultDays.size(); i++) {
            nonDefaultDayMap.put(nonDefaultDays.get(i).getStartTime().toLocalDate(), nonDefaultDays.get(i));
        }
        final List<HearingDay> hearingDayList = new ArrayList<>();
        nonDefaultDayMap.forEach(
                (date, ndd) -> {
                    if (!nonSittingDays.contains(date)) {
                        hearingDayList.add(buildNonDefaultHearingDay(nonDefaultDayMap, date, isCountBasedSlotSelected ? 1 : (ndd.getDuration().isPresent() ? ndd.getDuration().get() : defaultDuration)));
                    } else {
                        hearingDayList.add(buildDefaultHearingDay(defaultStartTime, isCountBasedSlotSelected ? 1 : (ndd.getDuration().isPresent() ? ndd.getDuration().get() : defaultDuration), date));
                    }
                }
        );

        if (nonDefaultDays.isEmpty()) {
            return buildSequentialHearingDays(startDate, endDate, nonSittingDays, nonDefaultDayMap, defaultStartTime, defaultDuration, isCountBasedSlotSelected);
        }
        Collections.reverse(hearingDayList);
        return hearingDayList;

    }

    @SuppressWarnings({"squid:S3358"})
    private static List<HearingDay> buildSequentialHearingDays(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays,
                                                               final Map<LocalDate, NonDefaultDay> nonDefaultDayMap,
                                                               final LocalTime defaultStartTime,
                                                               final Integer defaultDuration, final Boolean isCountBasedSlotSelected) {

        final long noOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        return IntStream.rangeClosed(0, (int) noOfDaysBetween)
                .mapToObj(i -> startDate.plusDays(i))
                .filter(d -> !nonSittingDays.contains(d))
                .map(date ->
                        nonDefaultDayMap.containsKey(date)
                                ? buildNonDefaultHearingDay(nonDefaultDayMap, date, isCountBasedSlotSelected ? 1 : defaultDuration)
                                : buildDefaultHearingDay(defaultStartTime, isCountBasedSlotSelected ? 1 : defaultDuration, date)
                )
                .collect(Collectors.toList());

    }

    private static HearingDay buildDefaultHearingDay(final LocalTime defaultStartTime, final Integer defaultDuration, final LocalDate date) {
        final LocalTime endTime = defaultStartTime.plusMinutes(defaultDuration);

        return HearingDay.hearingDay()
                .withCourtScheduleId(Optional.empty())
                .withHearingDate(date)
                .withStartTime(ZonedDateTime.of(date, defaultStartTime, BST)
                        .withZoneSameInstant(UTC))
                .withDurationMinutes(defaultDuration)
                .withSequence(0)
                .withEndTime(ZonedDateTime.of(date, endTime, BST).withZoneSameInstant(UTC))
                .build();
    }

    @SuppressWarnings("squid:S3655")
    private static HearingDay buildNonDefaultHearingDay(final Map<LocalDate, NonDefaultDay> startTimesMap, final LocalDate localDate, final Integer defaultDuration) {
        final NonDefaultDay nonDefaultDay = startTimesMap.get(localDate);
        final Integer durationMinutes = nonDefaultDay.getDuration().orElse(defaultDuration);
        final ZonedDateTime endDateTime = nonDefaultDay.getStartTime().plusMinutes(durationMinutes);

        final HearingDay.Builder builder = HearingDay.hearingDay()
                .withHearingDate(nonDefaultDay.getStartTime().toLocalDate())
                .withStartTime(nonDefaultDay.getStartTime())
                .withDurationMinutes(durationMinutes)
                .withSequence(0)
                .withEndTime(endDateTime);
        if (nonDefaultDay.getCourtScheduleId().isPresent()) {
            builder.withCourtScheduleId(Optional.of(UUID.fromString(nonDefaultDay.getCourtScheduleId().get())));
        }
        return builder.build();
    }

}