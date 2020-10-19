package uk.gov.moj.cpp.listing.domain.aggregate;


import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
                                             final List<NonDefaultDay> nonDefaultDays, final LocalTime defaultStartTime,
                                             final Integer defaultDuration, final CourtCentre defaultCourtCentre) {

        return calculate(startDate, endDate, nonSittingDays, nonDefaultDays, defaultStartTime, defaultDuration, false, defaultCourtCentre);
    }

    @SuppressWarnings({"squid:S3358", "squid:S3776"})
    public static List<HearingDay> calculate(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays,
                                             final List<NonDefaultDay> nonDefaultDays, final LocalTime defaultStartTime,
                                             final Integer defaultDuration, final Boolean isCountBasedSlotSelected,
                                             final CourtCentre defaultCourtCentre) {

        if (startDate == null || endDate == null) {
            return emptyList();
        }

        final List<LocalDate> nonDefaultDates = new ArrayList<>();
        final List<HearingDay> hearingDayList = new ArrayList<>();

        nonDefaultDays.stream().forEach(ndd -> {
            final LocalDate nonDefaultDate = ndd.getStartTime().toLocalDate();
            if (!nonSittingDays.contains(nonDefaultDate)) {
                hearingDayList.add(buildNonDefaultHearingDay(ndd, isCountBasedSlotSelected ? 1 : (ndd.getDuration().isPresent() ? ndd.getDuration().get() : defaultDuration)));
            }
            nonDefaultDates.add(nonDefaultDate);
        });

        hearingDayList.addAll(buildSequentialHearingDays(startDate, endDate, nonSittingDays, nonDefaultDates, defaultStartTime, defaultDuration, isCountBasedSlotSelected, defaultCourtCentre));
        Collections.sort(hearingDayList, Comparator.comparing(HearingDay::getStartTime));
        return hearingDayList;

    }

    @SuppressWarnings({"squid:S3358"})
    private static List<HearingDay> buildSequentialHearingDays(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays,
                                                               final List<LocalDate> nonDefaultDates, final LocalTime defaultStartTime,
                                                               final Integer defaultDuration, final Boolean isCountBasedSlotSelected,
                                                               final CourtCentre defaultCourtCentre) {

        final long noOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        return IntStream.rangeClosed(0, (int) noOfDaysBetween)
                .mapToObj(startDate::plusDays)
                .filter(d -> !nonSittingDays.contains(d) && !nonDefaultDates.contains(d))
                .map(date -> buildDefaultHearingDay(defaultStartTime, isCountBasedSlotSelected ? 1 : defaultDuration, date, defaultCourtCentre))
                .collect(Collectors.toList());

    }

    private static HearingDay buildDefaultHearingDay(final LocalTime defaultStartTime, final Integer defaultDuration, final LocalDate date,
                                                     final CourtCentre defaultCourtCentre) {
        final LocalTime endTime = defaultStartTime.plusMinutes(defaultDuration);

        return HearingDay.hearingDay()
                .withCourtScheduleId(Optional.empty())
                .withCourtCentreId(defaultCourtCentre.getId())
                .withCourtRoomId(defaultCourtCentre.getRoomId())
                .withHearingDate(date)
                .withStartTime(ZonedDateTime.of(date, defaultStartTime, BST)
                        .withZoneSameInstant(UTC))
                .withDurationMinutes(defaultDuration)
                .withSequence(0)
                .withEndTime(ZonedDateTime.of(date, endTime, BST).withZoneSameInstant(UTC))
                .build();
    }

    @SuppressWarnings("squid:S3655")
    private static HearingDay buildNonDefaultHearingDay(final NonDefaultDay nonDefaultDay, final Integer defaultDuration) {
        final Integer durationMinutes = nonDefaultDay.getDuration().orElse(defaultDuration);
        final ZonedDateTime endDateTime = nonDefaultDay.getStartTime().plusMinutes(durationMinutes);

        final HearingDay.Builder builder = HearingDay.hearingDay()
                .withHearingDate(nonDefaultDay.getStartTime().toLocalDate())
                .withStartTime(nonDefaultDay.getStartTime())
                .withDurationMinutes(durationMinutes)
                .withSequence(0)
                .withEndTime(endDateTime);
        if (nonDefaultDay.getCourtScheduleId().isPresent()) {
            builder.withCourtScheduleId(Optional.of(fromString(nonDefaultDay.getCourtScheduleId().get())));
        }
        nonDefaultDay.getCourtCentreId().map(UUID::fromString).ifPresent(builder::withCourtCentreId);
        nonDefaultDay.getRoomId().map(UUID::fromString).ifPresent(builder::withCourtRoomId);
        return builder.build();
    }

    public static List<uk.gov.justice.listing.events.NonDefaultDay> calculateNewNonDefaultDaysForUnscheduled(final Integer hearingTypeDuration, final ZonedDateTime startDate, final LocalTime defaultStartTime, final CourtCentre courtCentre) {
        return Collections.singletonList(uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                .withCourtScheduleId(Optional.empty())
                .withCourtCentreId(courtCentre.getId().toString())
                .withRoomId(courtCentre.getRoomId().map(UUID::toString).orElse(null))
                .withDuration(Optional.of(hearingTypeDuration))
                .withStartTime(ZonedDateTime.of(startDate.toLocalDate(), defaultStartTime, BST)
                        .withZoneSameInstant(UTC))
                .build());
    }
}
