package uk.gov.moj.cpp.listing.common.offence;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

import java.util.Comparator;

import javax.json.JsonNumber;
import javax.json.JsonObject;

/**
 * The order in which offences are printed on court outputs (LPT-2677).
 *
 * <p>The order of an offences array carries no meaning anywhere in CPP — progression reshuffles it
 * on every offence edit, and the UIs (Case at a glance, Manage hearing) sort client-side. This
 * follows Manage hearing:
 * <ul>
 *     <li>Magistrates: by {@code orderIndex}.</li>
 *     <li>Otherwise: by {@code count}, where 0 means "no count" and sorts last, then by
 *     {@code orderIndex}. Without counts this is the Magistrates order, so it is also the rule when
 *     the jurisdiction is unknown.</li>
 * </ul>
 * Offences missing the values sort last. Use with a stable sort so ties keep their stored order.
 */
public final class OffenceOrdering {

    private static final String MAGISTRATES = "MAGISTRATES";
    private static final String ORDER_INDEX = "orderIndex";
    private static final String COUNT = "count";

    private static final Comparator<JsonObject> BY_ORDER_INDEX =
            comparing(OffenceOrdering::orderIndex, nullsLast(naturalOrder()));

    private static final Comparator<JsonObject> BY_COUNT_THEN_ORDER_INDEX =
            comparing(OffenceOrdering::countOrNull, nullsLast(Comparator.<Integer>naturalOrder()))
                    .thenComparing(BY_ORDER_INDEX);

    private OffenceOrdering() {
    }

    public static Comparator<JsonObject> forJurisdiction(final String jurisdictionType) {
        return MAGISTRATES.equals(jurisdictionType) ? BY_ORDER_INDEX : BY_COUNT_THEN_ORDER_INDEX;
    }

    private static Integer orderIndex(final JsonObject offence) {
        return intOrNull(offence, ORDER_INDEX);
    }

    private static Integer countOrNull(final JsonObject offence) {
        final Integer count = intOrNull(offence, COUNT);
        return count == null || count == 0 ? null : count;
    }

    private static Integer intOrNull(final JsonObject offence, final String key) {
        final JsonNumber value = offence.containsKey(key) && !offence.isNull(key) ? offence.getJsonNumber(key) : null;
        return value == null ? null : value.intValue();
    }
}
