package uk.gov.moj.cpp.listing.domain.utils;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.convertHoursAndMinutesToMinutes;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.getUtcLocalTimeForDate;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.toIsoString;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

public class DateAndTimeUtilsTest {

    private final static LocalDate SUNDAY_25TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 25);
    private final static LocalDate MONDAY_26TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 26);
    private final static LocalDate TUESDAY_27TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 27);
    private final static LocalDate WEDNESDAY_28TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 28);
    private final static LocalDate THURSDAY_29TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 29);
    private final static LocalDate FRIDAY_30TH_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 30);
    private final static LocalDate SATURDAY_1ST_DECEMBER_2018 = LocalDate.of(2018, Month.DECEMBER, 1);
    private final static LocalDate MONDAY_3rd_DECEMBER_2018 = LocalDate.of(2018, Month.DECEMBER, 3);


    @Test
    public void theNextWorkingDayShouldBeMondayWhenTodayIsSunday() {
        assertThatNextWorkingDayIsAsExpected(SUNDAY_25TH_NOVEMBER_2018, MONDAY_26TH_NOVEMBER_2018);
    }

    @Test
    public void theNextWorkingDayShouldBeMondayWhenTodayIsMonday() {
        assertThatNextWorkingDayIsAsExpected(MONDAY_26TH_NOVEMBER_2018, TUESDAY_27TH_NOVEMBER_2018);
    }

    @Test
    public void theNextWorkingDayShouldBeMondayWhenTodayIsTuesday() {
        assertThatNextWorkingDayIsAsExpected(TUESDAY_27TH_NOVEMBER_2018, WEDNESDAY_28TH_NOVEMBER_2018);
    }

    @Test
    public void theNextWorkingDayShouldBeMondayWhenTodayIsWednesday() {
        assertThatNextWorkingDayIsAsExpected(WEDNESDAY_28TH_NOVEMBER_2018, THURSDAY_29TH_NOVEMBER_2018);
    }

    @Test
    public void theNextWorkingDayShouldBeMondayWhenTodayIsThursday() {
        assertThatNextWorkingDayIsAsExpected(THURSDAY_29TH_NOVEMBER_2018, FRIDAY_30TH_NOVEMBER_2018);
    }

    @Test
    public void theNextWorkingDayShouldBeMondayWhenTodayIsFriday() {
        assertThatNextWorkingDayIsAsExpected(FRIDAY_30TH_NOVEMBER_2018, MONDAY_3rd_DECEMBER_2018);
    }

    @Test
    public void theNextWorkingDayShouldBeMondayWhenTodayIsSaturday() {
        assertThatNextWorkingDayIsAsExpected(SATURDAY_1ST_DECEMBER_2018, MONDAY_3rd_DECEMBER_2018);
    }

    @Test
    public void convertingHoursAndMinutesToMinutesWhenNull() {

        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(null);

        assertThat(numberOfMinutesOpt, is(Optional.empty()));

    }

    @Test
    public void convertingHoursAndMinutesToMinutesWhenBlank() {

        final String rawDurationInHoursAndMinutes = "   ";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.empty()));

    }


    @Test
    public void convertingHoursAndMinutesToMinutesWhenRawHourIsNonNumeric() {

        final String rawDurationInHoursAndMinutes = "A:30";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.empty()));

    }


    @Test
    public void convertingHoursAndMinutesToMinutesWhenRawMinuteIsNonNumeric() {

        final String rawDurationInHoursAndMinutes = "2:B";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.empty()));

    }

    @Test
    public void convertingHoursAndMinutesToMinutesWhenOnlyHourPresent() {

        final String rawDurationInHoursAndMinutes = "2";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.of(120)));

    }

    @Test
    public void convertingHoursAndMinutesToMinutesWhenOnlyHourAndColonPresent() {

        final String rawDurationInHoursAndMinutes = "2:";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.of(120)));

    }


    @Test
    public void convertingHoursAndMinutesToMinutesWhenOnlyMinutesPresent() {

        final String rawDurationInHoursAndMinutes = ":20";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.of(20)));

    }

    @Test
    public void convertingHoursAndMinutesToMinutesWhenMinutesAreExcessive() {

        final String rawDurationInHoursAndMinutes = "1:64";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.of(124)));

    }

    @Test
    public void convertingHoursAndMinutesToMinutesWhenBothValid() {

        final String rawDurationInHoursAndMinutes = "3:42";
        final Optional<Integer> numberOfMinutesOpt = convertHoursAndMinutesToMinutes(rawDurationInHoursAndMinutes);

        assertThat(numberOfMinutesOpt, is(Optional.of(222)));

    }

    @Test
    public void shouldConvertToIsoString() {
        final ZonedDateTime dateTimeOffset = ZonedDateTime.of(2020, 1, 2, 18, 5, 22, 0, UTC);
        final String actual = toIsoString(dateTimeOffset);
        assertThat(actual, CoreMatchers.is("2020-01-02T18:05:22.000Z"));
    }

    private void assertThatNextWorkingDayIsAsExpected(LocalDate today, LocalDate expectedNextWorkingDay) {
        assertThat(DateAndTimeUtils.getNextWorkingDay(today), is(expectedNextWorkingDay));
    }

    // ===== Tests for getUtcLocalTimeForDate method =====

    @Test
    public void shouldConvertSummerTimeToUtcCorrectly() {
        // Given - Summer date (BST = UTC+1)
        LocalDate summerDate = LocalDate.of(2024, 7, 15); // July 15, 2024 (summer)
        int hour = 10;
        int minute = 0;

        // When
        LocalTime result = getUtcLocalTimeForDate(summerDate, hour, minute);

        // Then
        // 10:00 BST = 09:00 UTC (BST is UTC+1)
        assertEquals(LocalTime.of(9, 0), result);
    }

    @Test
    public void shouldConvertWinterTimeToUtcCorrectly() {
        // Given - Winter date (GMT = UTC+0)
        LocalDate winterDate = LocalDate.of(2024, 1, 15); // January 15, 2024 (winter)
        int hour = 10;
        int minute = 0;

        // When
        LocalTime result = getUtcLocalTimeForDate(winterDate, hour, minute);

        // Then
        // 10:00 GMT = 10:00 UTC (GMT is UTC+0)
        assertEquals(LocalTime.of(10, 0), result);
    }

    @Test
    public void shouldHandleSpringTransitionDateCorrectly() {
        // Given - Spring transition date (clocks go forward)
        LocalDate springTransitionDate = LocalDate.of(2024, 3, 31); // March 31, 2024 (spring transition)
        int hour = 10;
        int minute = 0;

        // When
        LocalTime result = getUtcLocalTimeForDate(springTransitionDate, hour, minute);

        // Then
        // 10:00 BST = 09:00 UTC (BST is UTC+1)
        assertEquals(LocalTime.of(9, 0), result);
    }

    @Test
    public void shouldHandleAutumnTransitionDateCorrectly() {
        // Given - Autumn transition date (clocks go back)
        LocalDate autumnTransitionDate = LocalDate.of(2024, 10, 27); // October 27, 2024 (autumn transition)
        int hour = 10;
        int minute = 0;

        // When
        LocalTime result = getUtcLocalTimeForDate(autumnTransitionDate, hour, minute);

        // Then
        // 10:00 GMT = 10:00 UTC (GMT is UTC+0)
        assertEquals(LocalTime.of(10, 0), result);
    }

    @Test
    public void shouldHandleDifferentTimesCorrectlyInSummer() {
        // Given - Summer date with various times
        LocalDate summerDate = LocalDate.of(2024, 7, 15); // July 15, 2024 (summer)
        
        // Test different times
        LocalTime[] testTimes = {
                LocalTime.of(8, 0),   // 8:00 AM
                LocalTime.of(9, 30),  // 9:30 AM
                LocalTime.of(14, 0),   // 2:00 PM
                LocalTime.of(16, 45)   // 4:45 PM
        };

        LocalTime[] expectedUtcTimes = {
                LocalTime.of(7, 0),   // 7:00 AM UTC
                LocalTime.of(8, 30),  // 8:30 AM UTC
                LocalTime.of(13, 0),  // 1:00 PM UTC
                LocalTime.of(15, 45)  // 3:45 PM UTC
        };

        for (int i = 0; i < testTimes.length; i++) {
            // When
            LocalTime result = getUtcLocalTimeForDate(
                    summerDate, testTimes[i].getHour(), testTimes[i].getMinute());

            // Then
            assertEquals(expectedUtcTimes[i], result, 
                    String.format("Failed for time %s - expected %s, got %s", 
                            testTimes[i], expectedUtcTimes[i], result));
        }
    }

    @Test
    public void shouldHandleDifferentTimesCorrectlyInWinter() {
        // Given - Winter date with various times
        LocalDate winterDate = LocalDate.of(2024, 1, 15); // January 15, 2024 (winter)
        
        // Test different times
        LocalTime[] testTimes = {
                LocalTime.of(8, 0),   // 8:00 AM
                LocalTime.of(9, 30),  // 9:30 AM
                LocalTime.of(14, 0),   // 2:00 PM
                LocalTime.of(16, 45)   // 4:45 PM
        };

        // In winter, GMT = UTC, so times should be the same
        for (LocalTime testTime : testTimes) {
            // When
            LocalTime result = getUtcLocalTimeForDate(
                    winterDate, testTime.getHour(), testTime.getMinute());

            // Then
            assertEquals(testTime, result, 
                    String.format("Failed for time %s - expected %s, got %s", 
                            testTime, testTime, result));
        }
    }

    @Test
    public void shouldHandleEdgeCaseTimesCorrectly() {
        // Given - Summer date with edge case times
        LocalDate summerDate = LocalDate.of(2024, 7, 15); // July 15, 2024 (summer)
        
        // Test edge cases
        int[][] edgeCases = {
                {0, 0},   // Midnight
                {23, 59}, // 11:59 PM
                {12, 0},  // Noon
                {1, 30}   // 1:30 AM
        };

        LocalTime[] expectedResults = {
                LocalTime.of(23, 0),  // 11:00 PM UTC (previous day)
                LocalTime.of(22, 59), // 10:59 PM UTC
                LocalTime.of(11, 0), // 11:00 AM UTC
                LocalTime.of(0, 30)   // 12:30 AM UTC
        };

        for (int i = 0; i < edgeCases.length; i++) {
            // When
            LocalTime result = getUtcLocalTimeForDate(
                    summerDate, edgeCases[i][0], edgeCases[i][1]);

            // Then
            assertEquals(expectedResults[i], result, 
                    String.format("Failed for time %02d:%02d - expected %s, got %s", 
                            edgeCases[i][0], edgeCases[i][1], expectedResults[i], result));
        }
    }

    @Test
    public void shouldHandleLeapYearDatesCorrectly() {
        // Given - Leap year date
        LocalDate leapYearDate = LocalDate.of(2024, 2, 29); // February 29, 2024 (leap year)
        int hour = 10;
        int minute = 0;

        // When
        LocalTime result = getUtcLocalTimeForDate(leapYearDate, hour, minute);

        // Then
        // February is still winter time (GMT), so 10:00 GMT = 10:00 UTC
        assertEquals(LocalTime.of(10, 0), result);
    }

    @Test
    public void shouldHandleYearBoundaryCorrectly() {
        // Given - New Year's Day
        LocalDate newYearDate = LocalDate.of(2024, 1, 1); // January 1, 2024
        int hour = 10;
        int minute = 0;

        // When
        LocalTime result = getUtcLocalTimeForDate(newYearDate, hour, minute);

        // Then
        // January is winter time (GMT), so 10:00 GMT = 10:00 UTC
        assertEquals(LocalTime.of(10, 0), result);
    }

    @Test
    public void shouldHandleDifferentDatesInSameYearCorrectly() {
        // Given - Different dates in 2024
        LocalDate[] testDates = {
                LocalDate.of(2024, 1, 1),   // New Year (winter)
                LocalDate.of(2024, 3, 31),   // Spring transition
                LocalDate.of(2024, 6, 21),   // Summer solstice (summer)
                LocalDate.of(2024, 9, 23),   // Autumn equinox (summer)
                LocalDate.of(2024, 10, 27),  // Autumn transition
                LocalDate.of(2024, 12, 25)   // Christmas (winter)
        };

        boolean[] isSummerTime = {false, true, true, true, false, false};
        int hour = 10;
        int minute = 0;

        for (int i = 0; i < testDates.length; i++) {
            // When
            LocalTime result = getUtcLocalTimeForDate(testDates[i], hour, minute);

            // Then
            LocalTime expectedTime = isSummerTime[i] ? LocalTime.of(9, 0) : LocalTime.of(10, 0);
            assertEquals(expectedTime, result, 
                    String.format("Failed for date %s - expected %s, got %s", 
                            testDates[i], expectedTime, result));
        }
    }

    @Test
    public void shouldReturnNonNullResult() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 7, 15);
        int hour = 10;
        int minute = 0;

        // When
        LocalTime result = getUtcLocalTimeForDate(testDate, hour, minute);

        // Then
        assertNotNull(result);
    }

    @Test
    public void shouldHandleExactDaylightSavingTransitionTimes() {
        // Given - Spring forward date (clocks go from 1:00 AM to 3:00 AM)
        LocalDate springForwardDate = LocalDate.of(2024, 3, 31); // March 31, 2024
        
        // Test times around the transition
        int[][] testTimes = {
                {0, 30},  // 12:30 AM (before transition)
                {1, 0},    // 1:00 AM (transition time)
                {2, 0},    // 2:00 AM (doesn't exist on spring forward)
                {3, 0},    // 3:00 AM (after transition)
                {4, 0}     // 4:00 AM (after transition)
        };

        LocalTime[] expectedResults = {
                LocalTime.of(0, 30),  // 12:30 AM UTC (same day, before transition)
                LocalTime.of(1, 0),   // 1:00 AM UTC (transition time)
                LocalTime.of(1, 0),   // 1:00 AM UTC (2:00 AM local doesn't exist, so it maps to 1:00 AM UTC)
                LocalTime.of(2, 0),   // 2:00 AM UTC (after transition)
                LocalTime.of(3, 0)    // 3:00 AM UTC (after transition)
        };

        for (int i = 0; i < testTimes.length; i++) {
            // When
            LocalTime result = getUtcLocalTimeForDate(
                    springForwardDate, testTimes[i][0], testTimes[i][1]);

            // Then
            assertEquals(expectedResults[i], result, 
                    String.format("Failed for time %02d:%02d on spring forward date - expected %s, got %s", 
                            testTimes[i][0], testTimes[i][1], expectedResults[i], result));
        }
    }
}