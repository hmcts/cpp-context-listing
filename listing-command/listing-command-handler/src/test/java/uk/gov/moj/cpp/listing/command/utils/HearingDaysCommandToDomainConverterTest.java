package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.listing.commands.HearingDay;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HearingDaysCommandToDomainConverterTest {

    private final HearingDaysCommandToDomainConverter converter = new HearingDaysCommandToDomainConverter();

    @Test
    void shouldReturnEmptyListWhenSourceIsNull() {
        assertThat(converter.convert(null), is(empty()));
    }

    @Test
    void shouldReturnEmptyListWhenSourceIsEmpty() {
        assertThat(converter.convert(emptyList()), is(empty()));
    }

    @Test
    void shouldPreserveEndTimeWhenExplicitlySet() {
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime startTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        final ZonedDateTime endTime = ZonedDateTime.of(2026, 4, 10, 14, 0, 0, 0, ZoneOffset.UTC);

        final HearingDay commandDay = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withHearingDate(startTime.toLocalDate())
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withDurationMinutes(240)
                .withSequence(0)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(commandDay));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getEndTime(), is(endTime));
    }

    @Test
    void shouldComputeEndTimeFromStartTimeAndDurationWhenEndTimeIsNull() {
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime startTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC);

        final HearingDay commandDay = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withHearingDate(startTime.toLocalDate())
                .withStartTime(startTime)
                .withDurationMinutes(30)
                .withSequence(0)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(commandDay));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getEndTime(), is(startTime.plusMinutes(30)));
    }

    @Test
    void shouldFallBackToStartTimeWhenBothEndTimeAndDurationAreNull() {
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime startTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC);

        final HearingDay commandDay = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withHearingDate(startTime.toLocalDate())
                .withStartTime(startTime)
                .withSequence(0)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(commandDay));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getEndTime(), is(startTime));
    }

    @Test
    void shouldMapIsDraftFromCommandToDomain() {
        final UUID courtCentreId = randomUUID();
        final UUID courtScheduleId = randomUUID();
        final ZonedDateTime startTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC);

        final HearingDay commandDay = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withCourtScheduleId(courtScheduleId)
                .withHearingDate(startTime.toLocalDate())
                .withStartTime(startTime)
                .withEndTime(startTime.plusMinutes(30))
                .withDurationMinutes(30)
                .withSequence(0)
                .withIsDraft(true)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(commandDay));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getIsDraft().isPresent(), is(true));
        assertThat(result.get(0).getIsDraft().get(), is(true));
        assertThat(result.get(0).getCourtScheduleId().get(), is(courtScheduleId));
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final UUID courtScheduleId = randomUUID();
        final ZonedDateTime startTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        final ZonedDateTime endTime = startTime.plusMinutes(60);
        final LocalDate hearingDate = startTime.toLocalDate();

        final HearingDay commandDay = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withCourtScheduleId(courtScheduleId)
                .withHearingDate(hearingDate)
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withDurationMinutes(60)
                .withSequence(1)
                .withIsCancelled(false)
                .withIsDraft(false)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(commandDay));

        assertThat(result, hasSize(1));
        final uk.gov.moj.cpp.listing.domain.HearingDay domain = result.get(0);
        assertThat(domain.getCourtCentreId().get(), is(courtCentreId));
        assertThat(domain.getCourtRoomId().get(), is(courtRoomId));
        assertThat(domain.getCourtScheduleId().get(), is(courtScheduleId));
        assertThat(domain.getHearingDate(), is(hearingDate));
        assertThat(domain.getStartTime(), is(startTime));
        assertThat(domain.getEndTime(), is(endTime));
        assertThat(domain.getDurationMinutes(), is(60));
        assertThat(domain.getSequence(), is(1));
        assertThat(domain.getIsCancelled().get(), is(false));
        assertThat(domain.getIsDraft().get(), is(false));
    }

    @Test
    void shouldSetOptionalEmptyForNullableFieldsWhenNull() {
        final ZonedDateTime startTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC);

        final HearingDay commandDay = HearingDay.hearingDay()
                .withCourtCentreId(randomUUID())
                .withCourtRoomId(null)
                .withCourtScheduleId(null)
                .withIsCancelled(null)
                .withHearingDate(startTime.toLocalDate())
                .withStartTime(startTime)
                .withEndTime(startTime.plusMinutes(15))
                .withDurationMinutes(15)
                .withSequence(0)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(commandDay));

        assertThat(result, hasSize(1));
        final uk.gov.moj.cpp.listing.domain.HearingDay domain = result.get(0);
        assertThat(domain.getCourtRoomId(), is(Optional.empty()));
        assertThat(domain.getCourtScheduleId(), is(Optional.empty()));
        assertThat(domain.getIsCancelled(), is(Optional.empty()));
    }

    @Test
    void shouldConvertMultipleHearingDaysPreservingOrder() {
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime firstStart = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        final ZonedDateTime secondStart = firstStart.plusDays(1);
        final ZonedDateTime thirdStart = firstStart.plusDays(2);

        final HearingDay first = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withHearingDate(firstStart.toLocalDate())
                .withStartTime(firstStart)
                .withEndTime(firstStart.plusMinutes(30))
                .withDurationMinutes(30)
                .withSequence(0)
                .withIsCancelled(null)
                .build();
        final HearingDay second = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withHearingDate(secondStart.toLocalDate())
                .withStartTime(secondStart)
                .withEndTime(secondStart.plusMinutes(10))
                .withDurationMinutes(10)
                .withSequence(1)
                .withIsCancelled(false)
                .build();
        final HearingDay third = HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withHearingDate(thirdStart.toLocalDate())
                .withStartTime(thirdStart)
                .withEndTime(thirdStart.plusMinutes(20))
                .withDurationMinutes(20)
                .withSequence(2)
                .withIsCancelled(true)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(first, second, third));

        assertThat(result, hasSize(3));
        assertThat(result.get(0).getHearingDate(), is(firstStart.toLocalDate()));
        assertThat(result.get(0).getSequence(), is(0));
        assertThat(result.get(0).getDurationMinutes(), is(30));
        assertThat(result.get(0).getIsCancelled(), is(Optional.empty()));
        assertThat(result.get(1).getHearingDate(), is(secondStart.toLocalDate()));
        assertThat(result.get(1).getSequence(), is(1));
        assertThat(result.get(1).getDurationMinutes(), is(10));
        assertThat(result.get(1).getIsCancelled(), is(Optional.of(false)));
        assertThat(result.get(2).getHearingDate(), is(thirdStart.toLocalDate()));
        assertThat(result.get(2).getSequence(), is(2));
        assertThat(result.get(2).getDurationMinutes(), is(20));
        assertThat(result.get(2).getIsCancelled(), is(Optional.of(true)));
    }
}
