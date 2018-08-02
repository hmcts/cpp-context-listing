package uk.gov.moj.cpp.listing.event.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class StartTimesJsonConverterTest {

    private StartTimesJsonConverter startTimesConverter;

    @Before
    public void setup(){
        startTimesConverter = new StartTimesJsonConverter();
    }

    @Test
    public void convertEmptyStartTimes() {
        String json = "{\"startTimes\":[]}";
        List<ZonedDateTime> zonedDateTimes = startTimesConverter.convertStartTimesFrom(json);

        assertThat(zonedDateTimes.size(), is(equalTo(0)));
    }


    @Test
    public void convertOneStartTimeFromJson() {
        String startTime = "2019-07-21T11:30:00Z";
        String json = "{\"startTimes\":[\"" + startTime + "\"]}";

        List<ZonedDateTime> zonedDateTimes = startTimesConverter.convertStartTimesFrom(json);

        assertThat(zonedDateTimes.size(), is(1));
        assertThat(zonedDateTimes.get(0), is(ZonedDateTime.parse(startTime)));

    }

    @Test
    public void convertStartTimesFromJson() {
        String startTime1 = "2020-11-10T11:00:00Z";
        String startTime2 = "2020-11-12T15:30:00Z";
        String startTime3 = "2020-11-18T11:30:00Z";
        String startTime4 = "2020-11-21T09:30:00Z";
        String json = "{\"startTimes\":[\"" + startTime1 + "\", \""+ startTime2 + "\", \""+
                startTime3 + "\", \"" + startTime4 + "\"]}";
        StartTimesJsonConverter startTimesConverter = new StartTimesJsonConverter();
        List<ZonedDateTime> zonedDateTimes = startTimesConverter.convertStartTimesFrom(json);

        assertThat(zonedDateTimes.size(), is(4));
        assertThat(zonedDateTimes.get(0), is(ZonedDateTime.parse(startTime1)));
        assertThat(zonedDateTimes.get(1), is(ZonedDateTime.parse(startTime2)));
        assertThat(zonedDateTimes.get(2), is(ZonedDateTime.parse(startTime3)));
        assertThat(zonedDateTimes.get(3), is(ZonedDateTime.parse(startTime4)));

    }

    @Test
    public void convertStartTimesToJson() {
        //given
        ZonedDateTime startTime1 = ZonedDateTimes.fromString("2020-11-10T11:00:00Z");
        ZonedDateTime startTime2 =  ZonedDateTimes.fromString("2020-11-12T15:30:00Z");

        String expectedJson = "{\"startTimes\":[\"" + ZonedDateTimes.toString(startTime1)
                + "\",\"" + ZonedDateTimes.toString(startTime2) + "\"]}";

        //when
        String actualJson = startTimesConverter.convertStartTimesTo(
                Arrays.asList(startTime1, startTime2));

        //then
        assertThat(actualJson, is(expectedJson));
    }
}