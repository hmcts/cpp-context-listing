package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.HearingDayDetailConverter.getHearingDayDetails;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.HearingDayDetailConverter.getMeridian;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.listing.event.processor.azure.data.HearingDayDetail;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.Test;

public class HearingDayDetailConverterTest {
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.now();
    private static final ZonedDateTime START_DATE_TIME1 = ZonedDateTime.now().plusDays(2).plusHours(3);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final String EXPECTED_SECOND_HEARING_DETAIL_DATE = ZonedDateTime.now().plusDays(2).plusHours(3).toLocalDate().toString();
    private static final String EXPECTED_FIRST_HEARING_DETAIL_DATE = ZonedDateTime.now().toLocalDate().toString();
    private static final String EXPECTED_FIRST_HEARING_DETAIL_TIME = getMeridian(ZonedDateTime.now());
    private static final String EXPECTED_SECOND_HEARING_DETAIL_TIME = getMeridian(ZonedDateTime.now().plusDays(2).plusHours(3));

    @Test
    public void shouldGetHearingDayDetails() {
        final List<HearingDayDetail> hearingDayDetails = getHearingDayDetails(getHearingDayDetail());

        assertNotNull(hearingDayDetails);
        assertThat(hearingDayDetails.size(), is(2));
        assertThat(hearingDayDetails.get(0).getDate(), is(EXPECTED_FIRST_HEARING_DETAIL_DATE));
        assertThat(hearingDayDetails.get(0).getTime(), is(EXPECTED_FIRST_HEARING_DETAIL_TIME));
        assertThat(hearingDayDetails.get(0).getDuration(), is(20));
        assertThat(hearingDayDetails.get(1).getDate(), is(EXPECTED_SECOND_HEARING_DETAIL_DATE));
        assertThat(hearingDayDetails.get(1).getTime(), is(EXPECTED_SECOND_HEARING_DETAIL_TIME));
        assertThat(hearingDayDetails.get(1).getDuration(), is(10));

    }

    @Test
    public void shouldGetAmMeridian() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2019-12-02T11:15:30-05:00");

        final String meridian = getMeridian(zonedDateTime);

        assertThat(meridian, is("AM"));
    }

    @Test
    public void shouldGetPmMeridian() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2019-12-02T14:15:30-05:00");

        final String meridian = getMeridian(zonedDateTime);

        assertThat(meridian, is("PM"));
    }

    @Test
    public void shouldGetAdMeridian() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2019-12-02T19:15:30-05:00");

        final String meridian = getMeridian(zonedDateTime);

        assertThat(meridian, is("AD"));
    }

    private List<HearingDay> getHearingDayDetail(){
        final String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);
        final String formattedDateTime1 = DATE_TIME_FORMAT.format(START_DATE_TIME1);
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                .withListedDurationMinutes(20)
                .build();
        final HearingDay hearingDay1 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTimes.fromString(formattedDateTime1))
                .withListedDurationMinutes(10)
                .build();
        return asList(hearingDay, hearingDay1);
    }
}