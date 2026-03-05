package uk.gov.moj.cpp.listing.domain.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HearingDayTest {

    @Test
    void isDraftShouldReturnFalseWhenNull() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withDurationMinutes(120)
                .build();
        assertThat(hearingDay.isDraft(), is(false));
    }

    @Test
    void isDraftShouldReturnTrueWhenSetToTrue() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withDurationMinutes(120)
                .withIsDraft(true)
                .build();
        assertThat(hearingDay.isDraft(), is(true));
    }

    @Test
    void isDraftShouldReturnFalseWhenSetToFalse() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withDurationMinutes(120)
                .withIsDraft(false)
                .build();
        assertThat(hearingDay.isDraft(), is(false));
    }

    @Test
    void isCancelledShouldReturnFalseWhenNull() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withDurationMinutes(120)
                .build();
        assertThat(hearingDay.isCancelled(), is(false));
    }

    @Test
    void equalsShouldConsiderIsDraft() {
        final LocalDate date = LocalDate.now();
        final HearingDay day1 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withIsDraft(true).build();
        final HearingDay day2 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withIsDraft(false).build();
        assertThat(day1, is(not(day2)));
    }

    @Test
    void equalsShouldBeEqualWhenAllFieldsMatch() {
        final LocalDate date = LocalDate.now();
        final UUID courtScheduleId = UUID.randomUUID();
        final HearingDay day1 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withCourtScheduleId(courtScheduleId).withIsDraft(true).build();
        final HearingDay day2 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withCourtScheduleId(courtScheduleId).withIsDraft(true).build();
        assertThat(day1, is(day2));
        assertThat(day1.hashCode(), is(day2.hashCode()));
    }

    @Test
    void withValuesFromShouldCopyAllFieldsIncludingIsDraft() {
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final LocalDate date = LocalDate.now();
        final ZonedDateTime start = date.atTime(10, 0).atZone(ZoneOffset.UTC);
        final ZonedDateTime end = date.atTime(12, 0).atZone(ZoneOffset.UTC);

        final HearingDay original = HearingDay.hearingDay()
                .withCourtScheduleId(courtScheduleId).withDurationMinutes(120).withEndTime(end)
                .withHearingDate(date).withSequence(1).withStartTime(start)
                .withIsCancelled(false).withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId).withIsDraft(true).build();

        final HearingDay copy = HearingDay.hearingDay().withValuesFrom(original).build();
        assertThat(copy, is(original));
        assertThat(copy.isDraft(), is(true));
        assertThat(copy.getCourtScheduleId(), is(courtScheduleId));
    }

    @Test
    void toStringShouldContainIsDraft() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now()).withIsDraft(true).build();
        assertThat(hearingDay.toString().contains("isDraft=true"), is(true));
    }
}
