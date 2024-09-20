package uk.gov.moj.cpp.listing.domain.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class HearingUtilTest {

    @Test
    public void shouldGetAdjustedDurationWithValidEstimate() {
        final Integer estimatedMinutes = 20;

        final Integer result = HearingUtil.getAdjustedDuration(estimatedMinutes);

        assertThat(result, is(estimatedMinutes));
    }

    @Test
    public void shouldGetAdjustedDurationWithZeroEstimate() {
        final Integer estimatedMinutes = 0;

        final Integer result = HearingUtil.getAdjustedDuration(estimatedMinutes);

        assertThat(result, is(1));
    }

    @Test
    public void shouldGetAdjustedDurationWithNoEstimate() {
        final Integer result = HearingUtil.getAdjustedDuration(null);

        assertThat(result, is(1));
    }
}