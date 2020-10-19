package uk.gov.moj.cpp.listing.command.utils;


import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.NonDefaultDay;

import java.time.ZonedDateTime;
import java.util.Optional;

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
        final Optional<String> courtCentreId = of(randomUUID().toString());
        final Optional<String> roomId = of(randomUUID().toString());
        RotaSlot rotaSlot = RotaSlot.rotaSlot()
                .withCourtRoomId(of(COURT_ROOM_ID))
                .withCourtScheduleId(of(COURT_SCHEDULE_ID))
                .withDuration(of(DURATION))
                .withOucode(of(OUCODE))
                .withSession(of(SESSION))
                .withStartTime(START_TIME)
                .withCourtCentreId(courtCentreId)
                .withRoomId(roomId)
                .build();

        NonDefaultDay nonDefaultDay = rotaSlotToNonDefaultDayConverter.convert(rotaSlot, CourtCentre.courtCentre()
                .withId(fromString(COURT_CENTRE_ID))
                .withRoomId(fromString(ROOM_ID)).build());

        assertThat(nonDefaultDay.getCourtRoomId(), is(of(COURT_ROOM_ID)));
        assertThat(nonDefaultDay.getCourtScheduleId(), is(of(COURT_SCHEDULE_ID)));
        assertThat(nonDefaultDay.getDuration(), is(of(DURATION)));
        assertThat(nonDefaultDay.getOucode(), is(of(OUCODE)));
        assertThat(nonDefaultDay.getSession(), is(of(SESSION)));
        assertThat(nonDefaultDay.getStartTime(), is(START_TIME));
        assertThat(nonDefaultDay.getCourtCentreId(), is(courtCentreId));
        assertThat(nonDefaultDay.getRoomId(), is(roomId));
    }

    @Test
    public void shouldConvertRotaSlotToNonDefaultDayWithDefaultCourtCentreAndNoCourtRoom() {
        final Optional<String> courtCentreId = of(randomUUID().toString());
        final Optional<String> roomId = empty();
        RotaSlot rotaSlot = RotaSlot.rotaSlot()
                .withCourtRoomId(of(COURT_ROOM_ID))
                .withCourtScheduleId(of(COURT_SCHEDULE_ID))
                .withDuration(of(DURATION))
                .withOucode(of(OUCODE))
                .withSession(of(SESSION))
                .withStartTime(START_TIME)
                .withCourtCentreId(courtCentreId)
                .withRoomId(roomId)
                .build();

        NonDefaultDay nonDefaultDay = rotaSlotToNonDefaultDayConverter.convert(rotaSlot, CourtCentre.courtCentre()
                .withId(fromString(COURT_CENTRE_ID))
                .withRoomId(empty()).build());

        assertThat(nonDefaultDay.getCourtRoomId(), is(of(COURT_ROOM_ID)));
        assertThat(nonDefaultDay.getCourtScheduleId(), is(of(COURT_SCHEDULE_ID)));
        assertThat(nonDefaultDay.getDuration(), is(of(DURATION)));
        assertThat(nonDefaultDay.getOucode(), is(of(OUCODE)));
        assertThat(nonDefaultDay.getSession(), is(of(SESSION)));
        assertThat(nonDefaultDay.getStartTime(), is(START_TIME));
        assertThat(nonDefaultDay.getCourtCentreId(), is(courtCentreId));
        assertThat(nonDefaultDay.getRoomId(), is(roomId));
    }
}
