package uk.gov.moj.cpp.listing.query.document.generator.util;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Sitting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class SittingsSorter {
    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

    public List<Sitting> sort(final List<Sitting> sittings) {
        sortBySittingTime(sittings);
        sortHearingsByStartTime(sittings);
        return sittings;
    }


    private void sortHearingsByStartTime(final List<Sitting> sittings) {
        sittings.stream().map(Sitting::getHearings).forEach(a -> Collections.sort(a,
                comparing(x -> LocalTime.parse(x.getHearingDay().getStartTime(), timeFormatter))));
    }

    /**
     * Sort by sitting times. If there is no  "Judiciary Names" then that sitting comes first.
     */
    private void sortBySittingTime(final List<Sitting> sittings) {

        Collections.sort(sittings, (a, b) -> {
            if (ofNullable(a.getJudiciaryNames()).map(List::size).orElse(0) == 0) {
                return -1;
            }
            if (ofNullable(b.getJudiciaryNames()).map(List::size).orElse(0) == 0) {
                return 1;
            }
            return LocalTime.parse(a.getSittingTime(), timeFormatter)
                    .compareTo(LocalTime.parse(b.getSittingTime(), timeFormatter));
        });
    }
}
