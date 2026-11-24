package uk.gov.moj.cpp.listing.command.api.util;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.NonDefaultDay;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NonDefaultDayConverter {

    public static List<NonDefaultDay> convertNonDefaultDaysCoreToCommand(final List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays) {
        List<uk.gov.justice.listing.commands.NonDefaultDay> nonDefaultDaysList = new ArrayList<>();
        for (uk.gov.justice.core.courts.NonDefaultDay nonDefaultDay : nonDefaultDays) {
            nonDefaultDaysList.add(uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                    .withCourtCentreId(nonDefaultDay.getCourtCentreId())
                    .withCourtRoomId(nonDefaultDay.getCourtRoomId())
                    .withStartTime(nonDefaultDay.getStartTime())
                    .withOucode(nonDefaultDay.getOucode())
                    .withRoomId(nonDefaultDay.getRoomId())
                    .withSession(nonDefaultDay.getSession())
                    .withDuration(nonDefaultDay.getDuration())
                    .withCourtScheduleId(nonDefaultDay.getCourtScheduleId())
                    .build());
        }
        return nonDefaultDaysList;
    }

    public static List<HearingDay> convertCoreNonDefaultDaysToHearingDays(final List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays) {
        List<HearingDay> hearingDayList = new ArrayList<>();
        for (uk.gov.justice.core.courts.NonDefaultDay nonDefaultDay : nonDefaultDays) {
            HearingDay.Builder builder = HearingDay.hearingDay()
                    .withCourtCentreId(UUID.fromString(nonDefaultDay.getCourtCentreId()))
                    .withCourtRoomId(UUID.fromString(nonDefaultDay.getRoomId()))
                    .withDurationMinutes(nonDefaultDay.getDuration())
                    .withStartTime(nonDefaultDay.getStartTime())
                    .withHearingDate(nonDefaultDay.getStartTime().toLocalDate())
                    .withEndTime(nonDefaultDay.getStartTime().plusMinutes(nonDefaultDay.getDuration()));
            if (nonNull(nonDefaultDay.getCourtScheduleId())) {
                builder.withCourtScheduleId(UUID.fromString(nonDefaultDay.getCourtScheduleId()));
            }
            hearingDayList.add(builder.build());
        }
        return hearingDayList;
    }

    public static List<HearingDay> convertBookedSlotsToHearingDays(final List<RotaSlot> bookedSlots) {
        List<HearingDay> hearingDayList = new ArrayList<>();
        for (RotaSlot slot : bookedSlots) {
            HearingDay.Builder builder = HearingDay.hearingDay()
                    .withCourtCentreId(UUID.fromString(slot.getCourtCentreId()))
                    .withCourtScheduleId(UUID.fromString(slot.getCourtScheduleId()))
                    .withDurationMinutes(slot.getDuration())
                    .withHearingDate(slot.getStartTime().toLocalDate())
                    .withStartTime(slot.getStartTime())
                    .withEndTime(slot.getStartTime().plusMinutes(slot.getDuration()));
            if (nonNull(slot.getRoomId())) {
                builder.withCourtRoomId(UUID.fromString(slot.getRoomId()));
            }
            hearingDayList.add(builder.build());
        }
        return hearingDayList;
    }

}
