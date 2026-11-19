package uk.gov.moj.cpp.listing.testutil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Test-only replacement for DeltaSpike's ArraysUtils.asSet. */
public final class TestSets {

    private TestSets() {
    }

    @SafeVarargs
    public static <T> Set<T> asSet(final T... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
