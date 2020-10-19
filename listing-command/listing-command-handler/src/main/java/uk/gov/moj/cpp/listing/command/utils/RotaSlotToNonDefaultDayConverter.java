package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.NonDefaultDay;

import java.util.Optional;
import java.util.UUID;

public class RotaSlotToNonDefaultDayConverter {

    public NonDefaultDay convert(final RotaSlot rotaSlot, final CourtCentre courtCentre) {
        final String courtCentreId = rotaSlot.getCourtCentreId().orElse(courtCentre.getId().toString());
        final Optional<String> roomId = rotaSlot.getRoomId().map(Optional::of).orElse(courtCentre.getRoomId().map(UUID::toString));
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
}
