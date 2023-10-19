package uk.gov.moj.cpp.listing.command.utils;


import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.NonDefaultDay;

import java.time.ZonedDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;



@RunWith(MockitoJUnitRunner.class)
public class RotaSlotToNonDefaultDayConverterTest {

    public static final int COURT_ROOM_ID = 1234567;
    public static final String COURT_SCHEDULE_ID = randomUUID().toString();
    public static final String COURT_CENTRE_ID = randomUUID().toString();
    public static final String ROOM_ID = randomUUID().toString();
    public static final int DURATION = 25;
    public static final String OUCODE = "ABCD";
    public static final String SESSION = "AM";
    public static final ZonedDateTime START_TIME = ZonedDateTime.now();

    @InjectMocks
    RotaSlotToNonDefaultDayConverter rotaSlotToNonDefaultDayConverter;

    @Test
    public void shouldConvertRotaSlotToNonDefaultDayWithDefaultCourtCentre() {
        final String courtCentreId = randomUUID().toString();
        final String roomId = randomUUID().toString();
        RotaSlot rotaSlot = RotaSlot.rotaSlot()
                .withCourtRoomId(COURT_ROOM_ID)
                .withCourtScheduleId(COURT_SCHEDULE_ID)
                .withDuration(DURATION)
                .withOucode(OUCODE)
                .withSession(SESSION)
                .withStartTime(START_TIME)
                .withCourtCentreId(courtCentreId)
                .withRoomId(roomId)
                .build();

        NonDefaultDay nonDefaultDay = rotaSlotToNonDefaultDayConverter.convert(rotaSlot, CourtCentre.courtCentre()
                .withId(fromString(COURT_CENTRE_ID))
                .withRoomId(fromString(ROOM_ID)).build());

        assertThat(nonDefaultDay.getCourtRoomId(), is(COURT_ROOM_ID));
        assertThat(nonDefaultDay.getCourtScheduleId(), is(COURT_SCHEDULE_ID));
        assertThat(nonDefaultDay.getDuration(), is(DURATION));
        assertThat(nonDefaultDay.getOucode(), is(OUCODE));
        assertThat(nonDefaultDay.getSession(), is(SESSION));
        assertThat(nonDefaultDay.getStartTime(), is(START_TIME));
        assertThat(nonDefaultDay.getCourtCentreId(), is(courtCentreId));
        assertThat(nonDefaultDay.getRoomId(), is(roomId));
    }

    @Test
    public void shouldConvertRotaSlotToNonDefaultDayWithDefaultCourtCentreAndNoCourtRoom() {
        final String courtCentreId = randomUUID().toString();
        final String roomId = null;
        RotaSlot rotaSlot = RotaSlot.rotaSlot()
                .withCourtRoomId(COURT_ROOM_ID)
                .withCourtScheduleId(COURT_SCHEDULE_ID)
                .withDuration(DURATION)
                .withOucode(OUCODE)
                .withSession(SESSION)
                .withStartTime(START_TIME)
                .withCourtCentreId(courtCentreId)
                .withRoomId(roomId)
                .build();

        NonDefaultDay nonDefaultDay = rotaSlotToNonDefaultDayConverter.convert(rotaSlot, CourtCentre.courtCentre()
                .withId(fromString(COURT_CENTRE_ID))
                .withRoomId(null).build());

        assertThat(nonDefaultDay.getCourtRoomId(), is(COURT_ROOM_ID));
        assertThat(nonDefaultDay.getCourtScheduleId(), is(COURT_SCHEDULE_ID));
        assertThat(nonDefaultDay.getDuration(), is(DURATION));
        assertThat(nonDefaultDay.getOucode(), is(OUCODE));
        assertThat(nonDefaultDay.getSession(), is(SESSION));
        assertThat(nonDefaultDay.getStartTime(), is(START_TIME));
        assertThat(nonDefaultDay.getCourtCentreId(), is(courtCentreId));
        assertThat(nonDefaultDay.getRoomId(), nullValue());
    }
}
