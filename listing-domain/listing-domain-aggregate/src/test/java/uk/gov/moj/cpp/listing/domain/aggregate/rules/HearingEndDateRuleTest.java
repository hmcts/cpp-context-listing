package uk.gov.moj.cpp.listing.domain.aggregate.rules;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;

import java.time.LocalDate;

import org.junit.Test;

public class HearingEndDateRuleTest {

    private static final LocalDate START_DATE = FUTURE_LOCAL_DATE.next();
    private static final LocalDate END_DATE = FUTURE_LOCAL_DATE.next();
    private static final LocalDate NO_END_DATE = null;

    @Test
    public void shouldApplyEndDateEqualToStartDateIfNoEndDateSpecified() {
        LocalDate actualEndDate = HearingEndDateRule.apply(NO_END_DATE, START_DATE);
        assertThat(actualEndDate, is(START_DATE));
    }

    @Test
    public void shouldApplyEndDateIfEndDateSpecified() {
        LocalDate actualEndDate = HearingEndDateRule.apply(END_DATE, START_DATE);
        assertThat(actualEndDate, is(END_DATE));
    }

}