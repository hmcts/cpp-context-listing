package uk.gov.moj.cpp.listing.domain;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HearingDayTest {

    @Test
    void getIsDraftShouldReturnEmptyByDefault() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withDurationMinutes(120)
                .build();
        assertThat(hearingDay.getIsDraft(), is(empty()));
    }

    @Test
    void getIsDraftShouldReturnValueWhenSet() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withIsDraft(of(true))
                .build();
        assertThat(hearingDay.getIsDraft(), is(of(true)));
    }

    @Test
    void equalsShouldConsiderIsDraft() {
        final LocalDate date = LocalDate.now();
        final HearingDay day1 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withIsDraft(of(true)).build();
        final HearingDay day2 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withIsDraft(of(false)).build();
        assertThat(day1, is(not(day2)));
    }

    @Test
    void equalsShouldBeEqualWhenAllFieldsMatch() {
        final LocalDate date = LocalDate.now();
        final UUID courtScheduleId = UUID.randomUUID();
        final HearingDay day1 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withCourtScheduleId(of(courtScheduleId)).withIsDraft(of(true)).build();
        final HearingDay day2 = HearingDay.hearingDay()
                .withHearingDate(date).withDurationMinutes(120).withCourtScheduleId(of(courtScheduleId)).withIsDraft(of(true)).build();
        assertThat(day1, is(day2));
        assertThat(day1.hashCode(), is(day2.hashCode()));
    }

    @Test
    void getIsCancelledShouldReturnEmptyByDefault() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now()).build();
        assertThat(hearingDay.getIsCancelled(), is(empty()));
    }

    @Test
    void toStringShouldContainIsDraft() {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now()).withIsDraft(of(true)).build();
        assertThat(hearingDay.toString().contains("isDraft"), is(true));
    }
}
