package uk.gov.moj.cpp.listing.query.view.hearing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class NonSittingDaysJsonConverterTest {

    private NonSittingDaysJsonConverter nonSittingDayConverter;

    @Before
    public void setup(){
        nonSittingDayConverter = new NonSittingDaysJsonConverter();
    }


    @Test
    public void convertEmptyStartTimes() {
        String json = "{\"nonSittingDays\":[]}";
        List<LocalDate> localDates = nonSittingDayConverter.convertNonSittingDays(json);

        assertThat(localDates.size(), is(equalTo(0)));
    }

    @Test
    public void convertSingleNonSittingDay() {
        String nonSittingDay1 = "2018-01-21";
        String json = "{\"nonSittingDays\":[\"" + nonSittingDay1 + "\"]}";
        List<LocalDate> localDates = nonSittingDayConverter.convertNonSittingDays(json);

        assertThat(localDates.size(), is(1));
        assertThat(localDates.get(0), is(LocalDate.parse(nonSittingDay1)));

    }

    @Test
    public void convertNonSittingDays() {
        String nonSittingDay1 = "2018-01-21";
        String nonSittingDay2 = "2018-01-22";
        String json = "{\"nonSittingDays\":[\"" + nonSittingDay1 + "\", \"" + nonSittingDay2 + "\"]}";
        List<LocalDate> localDates = nonSittingDayConverter.convertNonSittingDays(json);

        assertThat(localDates.size(), is(2));
        assertThat(localDates.get(0), is(LocalDate.parse(nonSittingDay1)));
        assertThat(localDates.get(1), is(LocalDate.parse(nonSittingDay2)));

    }
}