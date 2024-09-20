package uk.gov.moj.cpp.listing.domain.utils;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.convertHoursAndMinutesToMinutes;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.toIsoString;

import java.time.LocalDate;
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
}