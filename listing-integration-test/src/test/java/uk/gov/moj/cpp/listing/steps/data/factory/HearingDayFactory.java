package uk.gov.moj.cpp.listing.steps.data.factory;

import static java.time.ZonedDateTime.now;
import static uk.gov.moj.cpp.listing.steps.data.HearingDay.hearingDay;

import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.listing.steps.data.HearingDay;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class HearingDayFactory {
    public static List<HearingDay> buildHearingDaysWithCancelledFlag(final Boolean firstDayFlag,
                                                                     final Boolean secondDayFlag,
                                                                     final Boolean thirdDayFlag) {
        final StoppedClock clock = new StoppedClock(now());
        final HearingDay day1 = hearingDay()
                .withListedDurationMinutes(30)
                .withListingSequence(0)
                .withIsCancelled(firstDayFlag)
                .withSittingDay(clock.now())
                .build();
        final HearingDay day2 = hearingDay()
                .withListedDurationMinutes(10)
                .withListingSequence(1)
                .withSittingDay(clock.now().plusDays(1))
                .withIsCancelled(secondDayFlag)
                .build();
        final HearingDay day3 = hearingDay()
                .withListedDurationMinutes(10)
                .withListingSequence(2)
                .withSittingDay(clock.now().plusDays(2))
                .withIsCancelled(thirdDayFlag)
                .build();
        return ImmutableList.of(day1, day2, day3);
    }
}
