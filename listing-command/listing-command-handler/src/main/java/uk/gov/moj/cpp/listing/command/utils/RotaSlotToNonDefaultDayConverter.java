package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.services.common.converter.Converter;

public class RotaSlotToNonDefaultDayConverter implements Converter<RotaSlot, NonDefaultDay> {

    @Override
    public NonDefaultDay convert(final RotaSlot rotaSlot) {
        return NonDefaultDay.nonDefaultDay()
                .withCourtRoomId(rotaSlot.getCourtRoomId())
                .withCourtScheduleId(rotaSlot.getCourtScheduleId())
                .withDuration(rotaSlot.getDuration())
                .withStartTime(rotaSlot.getStartTime())
                .withSession(rotaSlot.getSession())
                .withOucode(rotaSlot.getOucode())
                .build();
    }
}
