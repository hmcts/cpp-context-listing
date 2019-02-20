package uk.gov.moj.cpp.listing.domain;

import static org.junit.Assert.assertTrue;

import java.time.Month;

import org.junit.Test;

public class WelshMonthTest {

    @Test
    public void testValueFor() {
        assertTrue(WelshMonth.valueFor(Month.JANUARY).get().equals(WelshMonth.IONAWR));
        assertTrue(WelshMonth.valueFor(Month.FEBRUARY).get().equals(WelshMonth.CHWEFROR));
        assertTrue(WelshMonth.valueFor(Month.MARCH).get().equals(WelshMonth.MAWRTH));
        assertTrue(WelshMonth.valueFor(Month.APRIL).get().equals(WelshMonth.EBRILL));
        assertTrue(WelshMonth.valueFor(Month.MAY).get().equals(WelshMonth.MAI));
        assertTrue(WelshMonth.valueFor(Month.JUNE).get().equals(WelshMonth.MEHEFIN));
        assertTrue(WelshMonth.valueFor(Month.JULY).get().equals(WelshMonth.GORFFENNAF));
        assertTrue(WelshMonth.valueFor(Month.AUGUST).get().equals(WelshMonth.AWST));
        assertTrue(WelshMonth.valueFor(Month.SEPTEMBER).get().equals(WelshMonth.MEDI));
        assertTrue(WelshMonth.valueFor(Month.OCTOBER).get().equals(WelshMonth.HYDREF));
        assertTrue(WelshMonth.valueFor(Month.NOVEMBER).get().equals(WelshMonth.TACHWEDD));
        assertTrue(WelshMonth.valueFor(Month.DECEMBER).get().equals(WelshMonth.RHAGFYR));
    }

}

