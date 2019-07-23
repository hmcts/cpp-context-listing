package uk.gov.moj.cpp.listing.domain.utils;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;

public class ZonedDateTimeFormatterTest {

    private static final ZoneId zid = ZoneId.of("Europe/London");

    private ZonedDateTime getZonedDateTime(boolean isBST){
        ZonedDateTime zdt;
        if(isBST) {
            zdt = ZonedDateTime.of(2018, 07, 18, 10, 30, 0, 0, zid);
        }
        else {
            zdt = ZonedDateTime.of(2018, 11, 18, 10, 30, 0, 0, zid);
        }
        return zdt;
    }

    @Test
    public void adjustDateTimeBST() {
        ZonedDateTime zdt = getZonedDateTime(true);
        ZonedDateTime adjustedZDT = ZonedDateTimeFormatter.adjustDateTime(zdt);
        assertEquals(11,adjustedZDT.getHour());
        assertEquals(30, adjustedZDT.getMinute());
        assertEquals(18, adjustedZDT.getDayOfMonth());
        assertEquals(07, adjustedZDT.getMonthValue());
    }

    @Test
    public void adjustDateTimeGMT() {
        ZonedDateTime zdt = getZonedDateTime(false);
        ZonedDateTime adjustedZDT = ZonedDateTimeFormatter.adjustDateTime(zdt);
        assertEquals(10, adjustedZDT.getHour());
        assertEquals(30, adjustedZDT.getMinute());
        assertEquals(18, adjustedZDT.getDayOfMonth());
        assertEquals(11, adjustedZDT.getMonthValue());
    }
}