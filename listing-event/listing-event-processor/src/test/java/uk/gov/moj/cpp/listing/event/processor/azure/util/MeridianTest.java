package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class MeridianTest {

    @Test
    public void shouldReturnValue() {
        assertThat(Meridian.ELEVEN_AM.getValue(), is("11"));
        assertThat(Meridian.TWELVE_PM.getValue(), is("12"));
        assertThat(Meridian.ONE_PM.getValue(), is("13"));
        assertThat(Meridian.TWO_PM.getValue(), is("14"));
        assertThat(Meridian.THREE_PM.getValue(), is("15"));
        assertThat(Meridian.FOUR_PM.getValue(), is("16"));
        assertThat(Meridian.FIVE_PM.getValue(), is("17"));
        assertThat(Meridian.NINE_AM.getValue(), is("09"));
        assertThat(Meridian.TEN_AM.getValue(), is("10"));
        assertThat(Meridian.TWELVE_AM.getValue(), is("00"));
    }

    @Test
    public void shouldReturnEnumarationValByValueOf() {
        assertThat(Meridian.valueOf("ELEVEN_AM"), is(Meridian.ELEVEN_AM));
        assertThat(Meridian.valueOf("TWELVE_PM"), is(Meridian.TWELVE_PM));
        assertThat(Meridian.valueOf("ONE_PM"), is(Meridian.ONE_PM));
    }
}