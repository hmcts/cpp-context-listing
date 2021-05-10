package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.platform.data.utils.date.MeridianUtil.getMeridian;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.HearingDayDetailConverter.getHearingDayDetails;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.listing.event.processor.azure.data.HearingDayDetail;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;

public class HearingDayDetailConverterTest {
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.parse("2019-12-02T11:11:30-05:00");
    private static final ZonedDateTime START_DATE_TIME1 = ZonedDateTime.parse("2019-12-04T11:15:30-05:00");
    private static final ZonedDateTime START_DATE_TIME2 = ZonedDateTime.parse("2019-12-04T19:15:30-05:00");

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final String EXPECTED_SECOND_HEARING_DETAIL_DATE = "2019-12-04";
    private static final String EXPECTED_FIRST_HEARING_DETAIL_DATE = "2019-12-02";
    private static final String EXPECTED_FIRST_HEARING_DETAIL_TIME = getMeridian(ZonedDateTime.parse("2019-12-04T11:15:30-05:00"));
    private static final String EXPECTED_SECOND_HEARING_DETAIL_TIME = getMeridian(ZonedDateTime.parse("2019-12-04T11:15:30-05:00"));
    public static final String EXPECTED_FIRST_HEARING_START_TIME = "2019-12-02T11:11:30.000Z";
    public static final String EXPECTED_SECONG_HEARING_START_TIME = "2019-12-04T11:15:30.000Z";

    @Test
    public void shouldGetHearingDayDetails() {
        final boolean isForAdjournmentHearing = false;
        final List<HearingDayDetail> hearingDayDetails = getHearingDayDetails(convertCourtHearingDayToEventHearingDay(getHearingDayDetail()), isForAdjournmentHearing);

        assertNotNull(hearingDayDetails);
        assertThat(hearingDayDetails.get(0).getDate(), is(EXPECTED_FIRST_HEARING_DETAIL_DATE));
        assertThat(hearingDayDetails.get(0).getTime(), is(EXPECTED_FIRST_HEARING_DETAIL_TIME));
        assertThat(hearingDayDetails.get(0).getDuration(), is(20));
        assertThat(hearingDayDetails.get(0).getHearingStartTime(), is(EXPECTED_FIRST_HEARING_START_TIME));

        assertThat(hearingDayDetails.get(1).getDate(), is(EXPECTED_SECOND_HEARING_DETAIL_DATE));
        assertThat(hearingDayDetails.get(1).getTime(), is(EXPECTED_SECOND_HEARING_DETAIL_TIME));
        assertThat(hearingDayDetails.get(1).getDuration(), is(10));
        assertThat(hearingDayDetails.get(1).getHearingStartTime(), is(EXPECTED_SECONG_HEARING_START_TIME));
    }

    @Test
    public void shouldGetHearingDayDetailsWhenSessionIsAllDayAndIsAdjournmentHearing() {
        final boolean isForAdjournmentHearing = true;
        final List<HearingDayDetail> hearingDayDetails = getHearingDayDetails(convertCourtHearingDayToEventHearingDay(getHearingDayDetailForAllDay()), isForAdjournmentHearing);

        assertNotNull(hearingDayDetails);
        assertThat(hearingDayDetails.size(), is(1));
        assertThat(hearingDayDetails.get(0).getDate(), is(EXPECTED_FIRST_HEARING_DETAIL_DATE));
        assertThat(hearingDayDetails.get(0).getDuration(), is(20));
        assertThat(hearingDayDetails.get(0).getHearingStartTime(), is("2019-12-02T04:15:30.000Z"));

    }

    private List<HearingDay> getHearingDayDetail() {
        final String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);
        final String formattedDateTime1 = DATE_TIME_FORMAT.format(START_DATE_TIME1);
        final String formattedDateTime2 = DATE_TIME_FORMAT.format(START_DATE_TIME2);
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                .withListedDurationMinutes(20)
                .build();
        final HearingDay hearingDay1 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTimes.fromString(formattedDateTime1))
                .withListedDurationMinutes(10)
                .build();
        final HearingDay hearingDay2 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTimes.fromString(formattedDateTime2))
                .withListedDurationMinutes(30)
                .build();
        return asList(hearingDay, hearingDay1, hearingDay2);
    }

    private List<HearingDay> getHearingDayDetailForAllDay() {
        final ZonedDateTime START_DATE_TIME = ZonedDateTime.parse("2019-12-02T04:15:30-05:00");
        final String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);

        final HearingDay hearingDay = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                .withListedDurationMinutes(20)
                .build();

        return singletonList(hearingDay);
    }

    public List<uk.gov.justice.listing.events.HearingDay> convertCourtHearingDayToEventHearingDay(final List<HearingDay> courtHearingDay) {
        return courtHearingDay.stream().map(chd -> uk.gov.justice.listing.events.HearingDay.hearingDay()
                .withCourtScheduleId(Optional.of(UUID.randomUUID()))
                .withDurationMinutes(chd.getListedDurationMinutes())
                .withHearingDate(chd.getSittingDay().toLocalDate())
                .withStartTime(chd.getSittingDay())
                .build()
        ).collect(Collectors.toList());

    }
}
