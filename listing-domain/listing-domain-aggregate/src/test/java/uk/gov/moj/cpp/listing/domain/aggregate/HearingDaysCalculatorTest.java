package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

@SuppressWarnings({"squid:UnusedPrivateMethod", "squid:CommentedOutCodeLine"})
public class HearingDaysCalculatorTest {

    private static final LocalTime DEFAULT_TIME = LocalTime.of(10, 30);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final LocalDate START_DATE = FUTURE_LOCAL_DATE.next();
    private static final LocalDate END_DATE = FUTURE_LOCAL_DATE.next();
    private static final int DEFAULT_DURATION = 30;
    private static final int OTHER_DURATION = 60;

    @Test
    public void shouldCreateEmptyHearingDaysCollectionIfNoStartDateProvided() {
        //given
        final LocalDate startDate = null;

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(startDate, END_DATE, new ArrayList<>(), new ArrayList<>(), DEFAULT_TIME, DEFAULT_DURATION);

        //then
        assertThat(actual, is(empty()));

    }

    @Test
    public void shouldCreateEmptyHearingDaysCollectionIfNoEndDateProvided() {
        //given
        final LocalDate endDate = null;

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(START_DATE, endDate, new ArrayList<>(), new ArrayList<>(), DEFAULT_TIME, DEFAULT_DURATION);

        //then
        assertThat(actual, is(empty()));

    }


    @Test
    public void shouldCreateHearingDaysForASingleDayWithoutASuppliedStartTime() {
        //given
        final LocalDate endDate = START_DATE;

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(START_DATE, endDate, new ArrayList<>(), new ArrayList<>(), DEFAULT_TIME, DEFAULT_DURATION);

        //then
        assertThat(actual, not(empty()));
        assertThat(actual.get(0).getStartTime(), is(ZonedDateTime.of(START_DATE, DEFAULT_TIME, BST).withZoneSameInstant(UTC)));
        assertThat(actual.get(0).getHearingDate(), is(START_DATE));
        assertThat(actual.get(0).getDurationMinutes(), is(DEFAULT_DURATION));
        assertThat(actual.get(0).getSequence(), is(0));
        assertThat(actual.get(0).getEndTime(), is(ZonedDateTime.of(START_DATE, DEFAULT_TIME.plusMinutes(DEFAULT_DURATION), BST).withZoneSameInstant(UTC)));


    }


    @Test
    public void shouldCreateHearingDaysForConsecutiveDaysWithoutSuppliedStartTime() {
        //given
        final LocalDate startDate = FUTURE_LOCAL_DATE.next();
        final int totalHearingDays = 5;
        final LocalDate endDate = startDate.plusDays(totalHearingDays - 1L);

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(startDate, endDate, new ArrayList<>(), new ArrayList<>(), DEFAULT_TIME, DEFAULT_DURATION);

        //then
        assertThat(actual.size(), is(totalHearingDays));

    }


    @Test
    public void shouldCreateHearingDaysWithoutSuppliedNonDefaultDays() {

        //given
        final LocalDate startDate = FUTURE_LOCAL_DATE.next();
        final int totalDaysStartToEndDate = 5;
        final LocalDate endDate = startDate.plusDays(totalDaysStartToEndDate - 1L);
        final List<LocalDate> nonSittingDays = Arrays.asList(startDate.plusDays(1));

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(startDate, endDate, nonSittingDays, new ArrayList<>(), DEFAULT_TIME, DEFAULT_DURATION);

        //then
        assertThat(actual.size(), is(totalDaysStartToEndDate - 1));

    }

    @Test
    public void shouldCreateHearingDaysForASingleDayWithASuppliedStartTimeAndDuration() {
        //given
        final LocalTime startTime = LocalTime.now();
        final LocalDate endDate = START_DATE;

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(
                START_DATE,
                endDate,
                new ArrayList<>(), Collections.singletonList(NonDefaultDay.nonDefaultDay()
                        .withStartTime(ZonedDateTime.of(START_DATE, startTime, UTC))
                        .withDuration(of(OTHER_DURATION))
                        .build()),
                DEFAULT_TIME,
                DEFAULT_DURATION);

        //then
        assertThat(actual.get(0).getStartTime(), is(ZonedDateTime.of(START_DATE, startTime, UTC)));
        assertThat(actual.get(0).getDurationMinutes(), is(OTHER_DURATION));
        assertThat(actual.get(0).getEndTime(), is(ZonedDateTime.of(START_DATE, startTime, UTC).plusMinutes(OTHER_DURATION)));


    }

    @Test
    public void shouldCreateHearingDaysForASingleDayWithASuppliedStartTimeAndDefaultDuration() {
        //given
        final LocalTime startTime = LocalTime.now();
        final LocalDate endDate = START_DATE;

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(
                START_DATE,
                endDate,
                new ArrayList<>(), Collections.singletonList(NonDefaultDay.nonDefaultDay()
                        .withStartTime(ZonedDateTime.of(START_DATE, startTime, UTC))
                        .withDuration(Optional.of(DEFAULT_DURATION))
                        .build()),
                DEFAULT_TIME,
                DEFAULT_DURATION);

        //then
        assertThat(actual.get(0).getStartTime(), is(ZonedDateTime.of(START_DATE, startTime, UTC)));
        assertThat(actual.get(0).getHearingDate(), is(START_DATE));
        assertThat(actual.get(0).getDurationMinutes(), is(DEFAULT_DURATION));
        assertThat(actual.get(0).getEndTime(), is(ZonedDateTime.of(START_DATE, startTime, UTC).plusMinutes(DEFAULT_DURATION)));
    }


    @Test
    public void shouldCreateHearingDaysForMultipleDaysWithSuppliedStartTimesAndDurations() {
        //given
        final LocalDate startDate = FUTURE_LOCAL_DATE.next();
        final int totalDaysStartToEndDate = 6;
        final LocalDate endDate = startDate.plusDays(totalDaysStartToEndDate - 1L);

        final List<LocalDate> nonSittingDays = Arrays.asList(endDate.minusDays(2), endDate.minusDays(1));

        final ZonedDateTime startTime1 = ZonedDateTime.of(startDate, LocalTime.of(14, 0), UTC);
        final ZonedDateTime startTime2 = ZonedDateTime.of(startDate.plusDays(1), LocalTime.of(15, 30), UTC);
        final ZonedDateTime startTime3 = ZonedDateTime.of(startDate.plusDays(2), LocalTime.of(9, 0), UTC);
        final ZonedDateTime startTime4 = ZonedDateTime.of(endDate, LocalTime.of(9, 0), UTC);

        final Integer[] durations = {45, 60, 120, 180};

        final List<NonDefaultDay> startTimes = Arrays.asList(
                NonDefaultDay.nonDefaultDay().withStartTime(startTime1).withDuration(of(durations[0])).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(startTime2).withDuration(of(durations[1])).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(startTime3).withDuration(of(durations[2])).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(startTime4).withDuration(of(durations[3])).build());

        //when
        final List<HearingDay> actual = HearingDaysCalculator.calculate(startDate, endDate, nonSittingDays, startTimes, DEFAULT_TIME, DEFAULT_DURATION);

        //then
        assertThat(actual.size(), is(totalDaysStartToEndDate - nonSittingDays.size()));
    }


    private List<HearingDay> expectedHearingDaysWithDefaultStartTimeAndDuration(final LocalDate startDate, final int totalDays, final LocalTime defaultTime, final int defaultDuration) {
        return IntStream.iterate(0, i -> i + 1)
                .limit(totalDays)
                .mapToObj(i -> ZonedDateTime.of(startDate.plusDays(i), defaultTime, BST).withZoneSameInstant(UTC))
                .map(zdt -> buildHearingDay(zdt, defaultDuration))
                .collect(Collectors.toList());
    }

    private List<HearingDay> expectedHearingDaysWithDefaultStartTimeAndDuration(final LocalDate startDate, final int totalDays, final List<LocalDate> nonSittingDays, final LocalTime defaultTime, final int defaultDuration) {
        final List<ZonedDateTime> nonSittingDatesWithDefaultTimes = nonSittingDays.stream()
                .map(d -> ZonedDateTime.of(d, LocalTime.of(10, 30, 0), BST).withZoneSameInstant(UTC))
                .collect(Collectors.toList());
        return expectedHearingDaysWithDefaultStartTimeAndDuration(startDate, totalDays, defaultTime, defaultDuration).stream()
                .filter(d -> !nonSittingDatesWithDefaultTimes.contains(d.getStartTime()))
                .collect(Collectors.toList());
    }


    private HearingDay buildHearingDay(final ZonedDateTime startTime, final Integer defaultDuration) {

        return HearingDay.hearingDay()
                .withHearingDate(startTime.toLocalDate())
                .withStartTime(startTime)
                .withDurationMinutes(defaultDuration)
                .withSequence(0)
                .withEndTime(startTime.plusMinutes(defaultDuration))
                .build();
    }

}
