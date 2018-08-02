package uk.gov.moj.cpp.listing.event.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class NonSittingDaysJsonConverterTest {
    private NonSittingDaysJsonConverter nonSittingDayConverter;

    @Before
    public void setup() {
        nonSittingDayConverter = new NonSittingDaysJsonConverter();
    }


    @Test
    public void convertEmptyStartTimes() {
        String json = "{\"nonSittingDays\":[]}";
        List<LocalDate> localDates = nonSittingDayConverter.convertNonSittingDaysFrom(json);

        assertThat(localDates.size(), is(equalTo(0)));
    }

    @Test
    public void convertSingleNonSittingDayFromJSon() {
        //given
        String nonSittingDay1 = "2018-08-10";
        String json = "{\"nonSittingDays\":[\"" + nonSittingDay1 + "\"]}";

        //when
        List<LocalDate> localDates = nonSittingDayConverter.convertNonSittingDaysFrom(json);

        //then
        assertThat(localDates.size(), is(1));
        assertThat(localDates.get(0), is(LocalDate.parse(nonSittingDay1)));

    }

    @Test
    public void convertNonSittingDaysFromJson() {
        //given
        String nonSittingDay1 = "2018-01-11";
        String nonSittingDay2 = "2018-01-15";
        String nonSittingDay3 = "2018-01-21";
        String json = "{\"nonSittingDays\":[\"" + nonSittingDay1 + "\", \"" + nonSittingDay2 + "\", \"" + nonSittingDay3 + "\"]}";

        //when
        List<LocalDate> localDates = nonSittingDayConverter.convertNonSittingDaysFrom(json);


        //then
        assertThat(localDates.size(), is(3));
        assertThat(localDates.get(0), is(LocalDate.parse(nonSittingDay1)));
        assertThat(localDates.get(1), is(LocalDate.parse(nonSittingDay2)));
        assertThat(localDates.get(2), is(LocalDate.parse(nonSittingDay3)));
    }

    @Test
    public void convertNonSittingDaysToJson() {
        //given
        LocalDate nonSittingDay1 = LocalDate.parse("2018-01-11");
        LocalDate nonSittingDay2 = LocalDate.parse("2018-01-15");
        LocalDate nonSittingDay3 = LocalDate.parse("2018-01-21");

        String expectedJson = "{\"nonSittingDays\":[\"" + nonSittingDay1.toString() + "\",\""
                + nonSittingDay2.toString() + "\",\"" + nonSittingDay3.toString() + "\"]}";

        //when
        String actualJson = nonSittingDayConverter.convertNonSittingDaysTo(
                Arrays.asList(nonSittingDay1, nonSittingDay2, nonSittingDay3));

        //then
        assertThat(actualJson, is(expectedJson));
    }

}