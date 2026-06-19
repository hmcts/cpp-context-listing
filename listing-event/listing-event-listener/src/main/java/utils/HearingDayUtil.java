package utils;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.HearingDay;

import java.util.List;

public final class HearingDayUtil {

    private HearingDayUtil(){}

    public static List<HearingDay> getNotCancelledHearingDays(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .filter(hearingDay -> isNull(hearingDay.getIsCancelled()) || !hearingDay.getIsCancelled())
                .collect(toList());
    }
}
