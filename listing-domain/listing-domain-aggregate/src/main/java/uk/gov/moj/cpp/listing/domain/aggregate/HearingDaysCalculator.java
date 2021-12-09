package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.emptyList;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingDaysCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDaysCalculator.class);

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");

    private HearingDaysCalculator() {
    }

    @SuppressWarnings({"squid:S3358", "squid:S3776"})
    public static List<HearingDay> calculate(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays,
                                             final List<NonDefaultDay> nonDefaultDays, final LocalTime defaultStartTime,
                                             final Integer defaultDuration, final CourtCentre defaultCourtCentre) {

        if (startDate == null || endDate == null) {
            return emptyList();
        }

        final List<LocalDate> nonDefaultDates = new ArrayList<>();
        final List<HearingDay> hearingDayList = new ArrayList<>();

        nonDefaultDays.forEach(ndd -> {
            final LocalDate nonDefaultDate = ndd.getStartTime().toLocalDate();
            if (!nonSittingDays.contains(nonDefaultDate)) {
                hearingDayList.add(buildNonDefaultHearingDay(ndd, (ndd.getDuration().isPresent() ? ndd.getDuration().get() : defaultDuration)));
            }
            nonDefaultDates.add(nonDefaultDate);
        });

        hearingDayList.addAll(buildSequentialHearingDays(startDate, endDate, nonSittingDays, nonDefaultDates, defaultStartTime, defaultDuration, defaultCourtCentre));
        hearingDayList.sort(Comparator.comparing(HearingDay::getStartTime));
        return hearingDayList;

    }

    @SuppressWarnings({"squid:S3358"})
    private static List<HearingDay> buildSequentialHearingDays(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays,
                                                               final List<LocalDate> nonDefaultDates, final LocalTime defaultStartTime,
                                                               final Integer defaultDuration, final CourtCentre defaultCourtCentre) {

        final long noOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        return IntStream.rangeClosed(0, (int) noOfDaysBetween)
                .mapToObj(startDate::plusDays)
                .filter(d -> !nonSittingDays.contains(d) && !nonDefaultDates.contains(d))
                .map(date -> buildDefaultHearingDay(defaultStartTime, defaultDuration, date, defaultCourtCentre))
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

        return HearingDay.hearingDay()
                .withHearingDate(nonDefaultDay.getStartTime().toLocalDate())
                .withStartTime(nonDefaultDay.getStartTime())
                .withDurationMinutes(durationMinutes)
                .withSequence(0)
                .withEndTime(endDateTime)
                .withCourtScheduleId(toUUID(nonDefaultDay.getCourtScheduleId()))
                .withCourtCentreId(toUUID(nonDefaultDay.getCourtCentreId()))
                .withCourtRoomId(toUUID(nonDefaultDay.getRoomId()))
                .build();
    }

    private static Optional<UUID> toUUID(Optional<String> optionalValue) {
        if (optionalValue.isPresent()) {
            try {
                final UUID uuid = UUID.fromString(optionalValue.get());
                return Optional.of(uuid);
            } catch (IllegalArgumentException ignored) {
                LOGGER.warn("Invalid UUID string: {}", optionalValue.get(), ignored);
            }
        }

        return Optional.empty();
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
