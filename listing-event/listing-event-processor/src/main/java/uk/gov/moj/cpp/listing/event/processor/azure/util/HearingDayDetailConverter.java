package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.lang.String.valueOf;
import static java.util.EnumSet.range;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.FIVE_PM;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.ONE_PM;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.TWELVE_AM;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.TWO_PM;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.moj.cpp.listing.event.processor.azure.data.HearingDayDetail;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

public class HearingDayDetailConverter {
    private static final EnumSet<Meridian> amMeridian = range(TWELVE_AM, ONE_PM);
    private static final EnumSet<Meridian> pmMeridian = range(TWO_PM, FIVE_PM);

    private HearingDayDetailConverter() {
    }

    public static List<HearingDayDetail> getHearingDayDetails(final List<HearingDay> hearingDays) {

        return hearingDays.stream()
                .map(HearingDayDetailConverter::convertHearingDayToHearingDayDetail)
                .collect(toList());
    }

    public static String getMeridian(final ZonedDateTime hearingDaySittingDay) {

        final String pattern = "hh";

        final String hour = hearingDaySittingDay.format(DateTimeFormatter.ofPattern(pattern));

        final boolean isAmMeridian = amMeridian.stream().anyMatch(meridian -> checkMeridian(meridian.getValue(), hour));
        final boolean isPmMeridian = pmMeridian.stream().anyMatch(meridian -> checkMeridian(meridian.getValue(), hour));

        if (isAmMeridian) {
            return "AM";
        }

        if (isPmMeridian) {
            return "PM";
        }

        throw new IllegalArgumentException("Session does not fall within AM or PM range");
    }

    private static boolean checkMeridian(final String value, final String hour) {
        return valueOf(value).equals(hour);
    }

    private static HearingDayDetail convertHearingDayToHearingDayDetail(final HearingDay hearingDay) {
        final int duration = hearingDay.getListedDurationMinutes();
        final ZonedDateTime hearingDaySittingDay = hearingDay.getSittingDay();
        final LocalDate date = hearingDaySittingDay.toLocalDate();

        return new HearingDayDetail(date.toString(), getMeridian(hearingDaySittingDay), duration);
    }
}
