package uk.gov.moj.cpp.listing.common.duration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HearingDurationDefaultsTest {

    private static final String KNOWN_TYPE_ID = UUID.randomUUID().toString();
    private static final String UNKNOWN_TYPE_ID = UUID.randomUUID().toString();

    @Test
    void shouldReturnConfiguredDurationWhenHearingTypeIsInMap() {
        final Map<String, Integer> map = new HashMap<>();
        map.put(KNOWN_TYPE_ID, 90);

        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(KNOWN_TYPE_ID, map), is(90));
    }

    @Test
    void shouldFallBackToDefaultMinWhenHearingTypeNotInMap() {
        final Map<String, Integer> map = new HashMap<>();
        map.put(KNOWN_TYPE_ID, 90);

        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(UNKNOWN_TYPE_ID, map), is(20));
    }

    @Test
    void shouldFallBackToDefaultMinWhenMapIsEmpty() {
        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(KNOWN_TYPE_ID, Collections.emptyMap()), is(20));
    }

    @Test
    void shouldFallBackToDefaultMinWhenMapIsNull() {
        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(KNOWN_TYPE_ID, null), is(20));
    }

    @Test
    void shouldFallBackToDefaultMinWhenHearingTypeIdIsNull() {
        final Map<String, Integer> map = new HashMap<>();
        map.put(KNOWN_TYPE_ID, 90);

        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(null, map), is(20));
    }

    @Test
    void shouldFallBackToDefaultMinWhenConfiguredDurationIsZero() {
        final Map<String, Integer> map = new HashMap<>();
        map.put(KNOWN_TYPE_ID, 0);

        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(KNOWN_TYPE_ID, map), is(20));
    }

    @Test
    void shouldFallBackToDefaultMinWhenConfiguredDurationIsOne() {
        final Map<String, Integer> map = new HashMap<>();
        map.put(KNOWN_TYPE_ID, 1);

        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(KNOWN_TYPE_ID, map), is(20));
    }

    @Test
    void shouldFallBackToDefaultMinWhenConfiguredDurationIsNull() {
        final Map<String, Integer> map = new HashMap<>();
        map.put(KNOWN_TYPE_ID, null);

        assertThat(HearingDurationDefaults.resolveHearingTypeDuration(KNOWN_TYPE_ID, map), is(20));
    }

    @Test
    void shouldCoerceNullToDefaultMin() {
        assertThat(HearingDurationDefaults.coerceToValidDuration(null), is(20));
    }

    @Test
    void shouldCoerceZeroToDefaultMin() {
        assertThat(HearingDurationDefaults.coerceToValidDuration(0), is(20));
    }

    @Test
    void shouldCoerceOneToDefaultMin() {
        assertThat(HearingDurationDefaults.coerceToValidDuration(1), is(20));
    }

    @Test
    void shouldPreserveValidDuration() {
        assertThat(HearingDurationDefaults.coerceToValidDuration(90), is(90));
    }
}
