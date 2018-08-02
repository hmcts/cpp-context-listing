package uk.gov.moj.cpp.listing.domain.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;

public class HearingDaysCalculatorTest {

    private static final LocalTime DEFAULT_TIME = LocalTime.of(10, 30);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final LocalDate START_DATE = FUTURE_LOCAL_DATE.next();


    @Test
    public void shouldCreateHearingDaysForASingleDayWithoutASuppliedStartTime() {
        //given
        LocalDate endDate = START_DATE;

        //when
        List<ZonedDateTime> actual = HearingDaysCalculator.calculate(START_DATE, endDate, new ArrayList<>(), new ArrayList<>());

        //then
        assertThat(actual, not(IsEmptyCollection.empty()));
        assertThat(actual.get(0), is(ZonedDateTime.of(START_DATE, DEFAULT_TIME, BST).withZoneSameInstant(UTC)));

    }


    @Test
    public void shouldCreateHearingDaysForConsecutiveDaysWithoutSuppliedStartTime() {
        //given
        LocalDate startDate = FUTURE_LOCAL_DATE.next();
        int totalHearingDays = 5;
        LocalDate endDate = startDate.plusDays(totalHearingDays - 1);

        //when
        List<ZonedDateTime> actual = HearingDaysCalculator.calculate(startDate, endDate, new ArrayList<>(), new ArrayList<>());

        //then
        assertThat(actual.size(), is(totalHearingDays));
        assertThat(actual, is(expectedHearingDaysWithDefaultStartTime(startDate, totalHearingDays)));

    }


    @Test
    public void shouldCreateHearingDaysWithoutSuppliedNonSittingDays() {

        //given
        LocalDate startDate = FUTURE_LOCAL_DATE.next();
        int totalDaysStartToEndDate = 5;
        LocalDate endDate = startDate.plusDays(totalDaysStartToEndDate - 1);
        List<LocalDate> nonSittingDays = Arrays.asList(startDate.plusDays(1));

        //when
        List<ZonedDateTime> actual = HearingDaysCalculator.calculate(startDate, endDate, nonSittingDays, new ArrayList<>());

        //then
        assertThat(actual.size(), is(totalDaysStartToEndDate - 1));
        assertThat(actual, is(expectedHearingDaysWithDefaultStartTime(startDate, totalDaysStartToEndDate, nonSittingDays)));

    }

    @Test
    public void shouldCreateHearingForASingleDayWithASuppliedStartTime() {
        //given
        LocalTime startTime = LocalTime.now();
        LocalDate endDate = START_DATE;

        //when
        List<ZonedDateTime> actual = HearingDaysCalculator.calculate(START_DATE, endDate, new ArrayList<>(),
                Collections.singletonList(ZonedDateTime.of(START_DATE, startTime, UTC)));

        //then
        assertThat(actual.get(0), is(ZonedDateTime.of(START_DATE, startTime, UTC)));

    }


    @Test
    public void shouldCreateHearingForMultipleDaysWithSuppliedStartTimes() {
        //given
        LocalDate startDate = FUTURE_LOCAL_DATE.next();
        int totalDaysStartToEndDate = 6;
        LocalDate endDate = startDate.plusDays(totalDaysStartToEndDate - 1);

        List<LocalDate> nonSittingDays = Arrays.asList(endDate.minusDays(2), endDate.minusDays(1));

        ZonedDateTime startTime1 = ZonedDateTime.of(startDate, LocalTime.of(14, 0), UTC);
        ZonedDateTime startTime2 = ZonedDateTime.of(startDate.plusDays(1), LocalTime.of(15, 30),UTC);
        ZonedDateTime startTime3 = ZonedDateTime.of(endDate, LocalTime.of(9, 0), UTC);
        List<ZonedDateTime> startTimes = Arrays.asList(startTime1, startTime2, startTime3);

        List<ZonedDateTime> expectedLocalDateTimes = Arrays.asList(startTime1, startTime2,
                ZonedDateTime.of(startDate.plusDays(2), DEFAULT_TIME, BST).withZoneSameInstant(UTC), startTime3);

        //when
        List<ZonedDateTime> actual = HearingDaysCalculator.calculate(startDate, endDate, nonSittingDays, startTimes);

        //then
        assertThat(actual.size(), is(totalDaysStartToEndDate - nonSittingDays.size()));
        assertThat(actual, is(expectedLocalDateTimes));
    }





    private List<ZonedDateTime> expectedHearingDaysWithDefaultStartTime(LocalDate startDate, int totalDays) {
        return IntStream.iterate(0, i -> i + 1)
                .limit(totalDays)
                .mapToObj(i -> ZonedDateTime.of(startDate.plusDays(i), LocalTime.of(10, 30), BST).withZoneSameInstant(UTC))
                .collect(Collectors.toList());
    }

    private List<ZonedDateTime> expectedHearingDaysWithDefaultStartTime(LocalDate startDate, int totalDays, List<LocalDate> nonSittingDays) {
        List<ZonedDateTime> nonSittingDatesWithDefaultTimes = nonSittingDays.stream()
                .map(d -> ZonedDateTime.of(d, LocalTime.of(10, 30, 0), BST).withZoneSameInstant(UTC))
                .collect(Collectors.toList());
        return expectedHearingDaysWithDefaultStartTime(startDate, totalDays).stream()
                .filter(d -> !nonSittingDatesWithDefaultTimes.contains(d))
                .collect(Collectors.toList());
    }
}