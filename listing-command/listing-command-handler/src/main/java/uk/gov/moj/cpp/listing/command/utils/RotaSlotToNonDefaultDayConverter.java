package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.NonDefaultDay;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class RotaSlotToNonDefaultDayConverter {

    public NonDefaultDay convert(final RotaSlot rotaSlot, final CourtCentre courtCentre) {
        final String courtCentreId = StringUtils.isNotEmpty(rotaSlot.getCourtCentreId()) ? rotaSlot.getCourtCentreId() : courtCentre.getId().toString();
        final String roomId = getRoomId(rotaSlot, courtCentre.getRoomId());
        return NonDefaultDay.nonDefaultDay()
                .withCourtRoomId(rotaSlot.getCourtRoomId())
                .withCourtScheduleId(rotaSlot.getCourtScheduleId())
                .withDuration(rotaSlot.getDuration())
                .withStartTime(rotaSlot.getStartTime())
                .withSession(rotaSlot.getSession())
                .withOucode(rotaSlot.getOucode())
                .withRoomId(roomId)
                .withCourtCentreId(courtCentreId)
                .build();
    }

    private  String getRoomId(final RotaSlot rotaSlot, final UUID courtCentreRoomId) {
        if(StringUtils.isNotEmpty(rotaSlot.getRoomId())) {
            return rotaSlot.getRoomId();
        } else {
            return null != courtCentreRoomId ? courtCentreRoomId.toString() : null;
        }
    }
}
