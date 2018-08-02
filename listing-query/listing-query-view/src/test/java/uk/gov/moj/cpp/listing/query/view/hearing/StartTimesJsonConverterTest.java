package uk.gov.moj.cpp.listing.query.view.hearing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.ZonedDateTime;
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
    public void convertOneStartTime() {
        String startTime = "2018-01-21T10:00:00Z";
        String json = "{\"startTimes\":[\"" + startTime + "\"]}";
        List<ZonedDateTime> zonedDateTimes = startTimesConverter.convertStartTimesFrom(json);

        assertThat(zonedDateTimes.size(), is(1));
        assertThat(zonedDateTimes.get(0), is(ZonedDateTime.parse(startTime)));

    }

    @Test
    public void convertStartTimes() {
        String startTime1 = "2018-01-21T10:00:00Z";
        String startTime2 = "2018-01-22T15:30:00Z";
        String json = "{\"startTimes\":[\"" + startTime1 + "\", \"" + startTime2 + "\"]}";
        List<ZonedDateTime> zonedDateTimes = startTimesConverter.convertStartTimesFrom(json);

        assertThat(zonedDateTimes.size(), is(2));
        assertThat(zonedDateTimes.get(0), is(ZonedDateTime.parse(startTime1)));
        assertThat(zonedDateTimes.get(1), is(ZonedDateTime.parse(startTime2)));

    }
}