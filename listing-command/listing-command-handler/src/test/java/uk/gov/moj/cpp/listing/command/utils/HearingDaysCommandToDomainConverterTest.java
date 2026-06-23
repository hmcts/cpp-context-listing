package uk.gov.moj.cpp.listing.command.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.listing.commands.HearingDay.hearingDay;

import uk.gov.justice.listing.commands.HearingDay;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class HearingDaysCommandToDomainConverterTest {

    private final HearingDaysCommandToDomainConverter converter = new HearingDaysCommandToDomainConverter();

    private static final UUID COURT_CENTRE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURT_ROOM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURT_SCHEDULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDate HEARING_DATE = LocalDate.of(2020, 8, 18);
    private static final ZonedDateTime START_TIME = ZonedDateTime.parse("2020-08-18T01:22:12.381Z");
    private static final ZonedDateTime END_TIME = ZonedDateTime.parse("2020-08-18T02:22:12.381Z");

    @Test
    public void shouldReturnEmptyListWhenSourceIsNull() {
        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(null);

        assertThat(result, hasSize(0));
    }

    @Test
    public void shouldReturnEmptyListWhenSourceIsEmpty() {
        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(Collections.emptyList());

        assertThat(result, hasSize(0));
    }

    @Test
    public void shouldConvertAllFieldsFromCommandToDomain() {
        final HearingDay command = hearingDay()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withCourtScheduleId(COURT_SCHEDULE_ID)
                .withDurationMinutes(30)
                .withSequence(1)
                .withIsCancelled(false)
                .withHearingDate(HEARING_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(command));

        assertThat(result, hasSize(1));
        final uk.gov.moj.cpp.listing.domain.HearingDay domain = result.get(0);
        assertThat(domain.getCourtCentreId(), is(Optional.of(COURT_CENTRE_ID)));
        assertThat(domain.getCourtRoomId(), is(Optional.of(COURT_ROOM_ID)));
        assertThat(domain.getCourtScheduleId(), is(Optional.of(COURT_SCHEDULE_ID)));
        assertThat(domain.getDurationMinutes(), is(30));
        assertThat(domain.getSequence(), is(1));
        assertThat(domain.getIsCancelled(), is(Optional.of(false)));
        assertThat(domain.getHearingDate(), is(HEARING_DATE));
        assertThat(domain.getStartTime(), is(START_TIME));
        assertThat(domain.getEndTime(), is(END_TIME));
    }

    @Test
    public void shouldFallBackToStartTimeWhenEndTimeIsNull() {
        final HearingDay command = hearingDay()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withDurationMinutes(30)
                .withSequence(0)
                .withHearingDate(HEARING_DATE)
                .withStartTime(START_TIME)
                .withEndTime(null)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(command));

        assertThat(result.get(0).getEndTime(), is(START_TIME));
    }

    @Test
    public void shouldSetOptionalEmptyForNullableFieldsWhenNull() {
        final HearingDay command = hearingDay()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(null)
                .withCourtScheduleId(null)
                .withIsCancelled(null)
                .withDurationMinutes(15)
                .withSequence(0)
                .withHearingDate(HEARING_DATE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(command));

        final uk.gov.moj.cpp.listing.domain.HearingDay domain = result.get(0);
        assertThat(domain.getCourtRoomId(), is(Optional.empty()));
        assertThat(domain.getCourtScheduleId(), is(Optional.empty()));
        assertThat(domain.getIsCancelled(), is(Optional.empty()));
    }

    @Test
    public void shouldConvertMultipleHearingDaysPreservingOrder() {
        final HearingDay first = hearingDay()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withDurationMinutes(30)
                .withSequence(0)
                .withIsCancelled(null)
                .withHearingDate(LocalDate.of(2020, 8, 18))
                .withStartTime(ZonedDateTime.parse("2020-08-18T01:22:12.381Z"))
                .withEndTime(ZonedDateTime.parse("2020-08-18T02:22:12.381Z"))
                .build();

        final HearingDay second = hearingDay()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withDurationMinutes(10)
                .withSequence(1)
                .withIsCancelled(false)
                .withHearingDate(LocalDate.of(2020, 8, 19))
                .withStartTime(ZonedDateTime.parse("2020-08-19T01:22:12.381Z"))
                .withEndTime(ZonedDateTime.parse("2020-08-19T02:22:12.381Z"))
                .build();

        final HearingDay third = hearingDay()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withDurationMinutes(20)
                .withSequence(2)
                .withIsCancelled(true)
                .withHearingDate(LocalDate.of(2020, 8, 20))
                .withStartTime(ZonedDateTime.parse("2020-08-20T02:22:12.381Z"))
                .withEndTime(ZonedDateTime.parse("2020-08-20T03:22:12.381Z"))
                .build();

        final List<uk.gov.moj.cpp.listing.domain.HearingDay> result = converter.convert(List.of(first, second, third));

        assertThat(result, hasSize(3));

        assertThat(result.get(0).getHearingDate(), is(LocalDate.of(2020, 8, 18)));
        assertThat(result.get(0).getSequence(), is(0));
        assertThat(result.get(0).getDurationMinutes(), is(30));
        assertThat(result.get(0).getIsCancelled(), is(Optional.empty()));

        assertThat(result.get(1).getHearingDate(), is(LocalDate.of(2020, 8, 19)));
        assertThat(result.get(1).getSequence(), is(1));
        assertThat(result.get(1).getDurationMinutes(), is(10));
        assertThat(result.get(1).getIsCancelled(), is(Optional.of(false)));

        assertThat(result.get(2).getHearingDate(), is(LocalDate.of(2020, 8, 20)));
        assertThat(result.get(2).getSequence(), is(2));
        assertThat(result.get(2).getDurationMinutes(), is(20));
        assertThat(result.get(2).getIsCancelled(), is(Optional.of(true)));
    }
}
