package uk.gov.moj.cpp.listing.common.offence;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

/**
 * LPT-2677: court outputs must order offences the way Manage hearing does, not in the order they
 * happen to be stored on the listing hearing.
 */
class OffenceOrderingTest {

    @Test
    void shouldOrderMagistratesOffencesByOrderIndexIgnoringCount() {
        assertThat(order("MAGISTRATES",
                        offence("oi 3 count 1", 3, 1),
                        offence("oi 1 count 2", 1, 2),
                        offence("oi 2 count 0", 2, 0)),
                contains("oi 1 count 2", "oi 2 count 0", "oi 3 count 1"));
    }

    @Test
    void shouldOrderCrownOffencesByCountThenOrderIndexWithUncountedOffencesLast() {
        assertThat(order("CROWN",
                        offence("oi 1 count 2", 1, 2),
                        offence("oi 2 no count", 2, 0),
                        offence("oi 3 count 1", 3, 1),
                        offence("oi 0 no count", 0, 0)),
                contains("oi 3 count 1", "oi 1 count 2", "oi 0 no count", "oi 2 no count"));
    }

    /**
     * Hearings damaged before the fix hold offences without orderIndex/count; they stay listed,
     * after the orderable ones, in their stored order.
     */
    @Test
    void shouldPlaceOffencesWithoutOrderIndexLastKeepingTheirStoredOrder() {
        assertThat(order("MAGISTRATES",
                        offenceWithoutOrdering("no index A"),
                        offence("oi 2", 2, 0),
                        offenceWithoutOrdering("no index B"),
                        offence("oi 1", 1, 0)),
                contains("oi 1", "oi 2", "no index A", "no index B"));
    }

    /**
     * Some outputs (the XHIBIT feed) carry no jurisdiction: they get the count-then-orderIndex
     * rule, which is identical to the Magistrates rule whenever offences have no count.
     */
    @Test
    void shouldOrderByCountThenOrderIndexWhenJurisdictionIsUnknown() {
        assertThat(order(null,
                        offence("oi 2 count 1", 2, 1),
                        offence("oi 1 no count", 1, 0)),
                contains("oi 2 count 1", "oi 1 no count"));
    }

    private static List<String> order(final String jurisdictionType, final JsonObject... offences) {
        return Stream.of(offences)
                .sorted(OffenceOrdering.forJurisdiction(jurisdictionType))
                .map(offence -> offence.getString("title"))
                .collect(toList());
    }

    private static JsonObject offence(final String title, final int orderIndex, final int count) {
        return builder(title).add("orderIndex", orderIndex).add("count", count).build();
    }

    private static JsonObject offenceWithoutOrdering(final String title) {
        return builder(title).build();
    }

    private static JsonObjectBuilder builder(final String title) {
        return createObjectBuilder().add("title", title);
    }
}
