package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NonDefaultDayDurationBuilder {

    private static final String ALL_DAY = "AD";
    private static final int HALF_DAY_MINUTES = 180;
    private static final int FULL_DAY_MINUTES = 360;

    public List<NonDefaultDay> updateNonDefaultDayWithNewDuration(final List<NonDefaultDay> ndd, final Integer totalDuration) {
        //single day selection as all day session
        if (isAllDaySessionBooking(ndd)) {
            return totalDuration < FULL_DAY_MINUTES ? ndd : returnSingleDayAllDaySession(ndd);
        }

        return buildNewNonDefaultDays(ndd, totalDuration);
    }

    private List<NonDefaultDay> returnSingleDayAllDaySession(final List<NonDefaultDay> ndd) {
        return Collections.singletonList(NonDefaultDay.nonDefaultDay().withValuesFrom(ndd.get(0)).withDuration(FULL_DAY_MINUTES).build());
    }

    public UpdateHearingForListing buildNewUpdateHearingForListingWithNewNonDefaultDays(final UpdateHearingForListing hearing, final List<NonDefaultDay> nonDefaultDays) {
        return new UpdateHearingForListing.Builder()
                .withCourtCentreId(hearing.getCourtCentreId())
                .withCourtRoomId(hearing.getCourtRoomId())
                .withEndDate(hearing.getEndDate())
                .withHearingId(hearing.getHearingId())
                .withHearingLanguage(hearing.getHearingLanguage())
                .withJudiciary(hearing.getJudiciary())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withNonDefaultDays(updateNonDefaultDayWithNewDuration(hearing.getNonDefaultDays(), getDuration(hearing.getNonDefaultDays())))
                .withNonSittingDays(hearing.getNonSittingDays())
                .withStartDate(getNewStartDate(nonDefaultDays))
                .withType(hearing.getType())
                .withWeekCommencingDurationInWeeks(hearing.getWeekCommencingDurationInWeeks())
                .withWeekCommencingEndDate(hearing.getWeekCommencingEndDate())
                .withWeekCommencingStartDate(hearing.getWeekCommencingStartDate())
                .withHasVideoLink(hearing.getHasVideoLink())
                .withPublicListNote(hearing.getPublicListNote())
                .withSelectedCourtCentre(hearing.getSelectedCourtCentre())
                .withPanel(hearing.getPanel())
                .build();
    }

    private LocalDate getNewStartDate(final List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.
                get(0)
                .getStartTime()
                .toLocalDate();
    }

    private List<NonDefaultDay> buildNewNonDefaultDays(final List<NonDefaultDay> nonDefaultDays, final int totalDuration) {
        if (totalDuration < HALF_DAY_MINUTES || isHalfDaySession(totalDuration)) {
            final int duration = totalDuration < HALF_DAY_MINUTES ? totalDuration : HALF_DAY_MINUTES;

            return nonDefaultDays.stream().map(e -> nonDefaultDayWithNewDuration(e, duration)).collect(toList());

        } else {

            final List<NonDefaultDay> newNonDefaultDays = new ArrayList<>();
            int remainingDuration = totalDuration;

            for (final NonDefaultDay nonDefaultDay : nonDefaultDays) {
                final int duration = remainingDuration > HALF_DAY_MINUTES ? HALF_DAY_MINUTES : remainingDuration;

                final NonDefaultDay newNonDefaultDay = nonDefaultDayWithNewDuration(nonDefaultDay, duration);
                newNonDefaultDays.add(newNonDefaultDay);

                remainingDuration -= duration;
            }

            return newNonDefaultDays;
        }
    }

    private NonDefaultDay nonDefaultDayWithNewDuration(final NonDefaultDay nonDefaultDay, final int totalDuration) {
        return new NonDefaultDay.Builder()
                .withCourtRoomId(nonDefaultDay.getCourtRoomId())
                .withCourtScheduleId(nonDefaultDay.getCourtScheduleId())
                .withDuration(totalDuration)
                .withOucode(nonDefaultDay.getOucode())
                .withSession(nonDefaultDay.getSession())
                .withStartTime(nonDefaultDay.getStartTime())
                .withCourtCentreId(nonDefaultDay.getCourtCentreId())
                .withRoomId(nonDefaultDay.getRoomId())
                .withVirtual(nonDefaultDay.getVirtual()).build();
    }

    private boolean isAllDaySessionBooking(final List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.size() == 1 && nonDefaultDays.get(0).getSession().equals(ALL_DAY);
    }

    private boolean isHalfDaySession(final int duration) {

        return duration == HALF_DAY_MINUTES;
    }

    private int getDuration(final List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream().map(NonDefaultDay::getDuration)
                .findFirst().orElse(1);
    }
}
