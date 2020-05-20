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
