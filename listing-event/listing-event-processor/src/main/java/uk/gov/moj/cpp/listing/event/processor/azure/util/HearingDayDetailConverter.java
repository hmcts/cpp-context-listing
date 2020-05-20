package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.lang.String.valueOf;
import static java.util.EnumSet.range;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.FIVE_PM;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.ONE_PM;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.TWELVE_AM;
import static uk.gov.moj.cpp.listing.event.processor.azure.util.Meridian.TWELVE_PM;

import uk.gov.moj.cpp.listing.event.processor.azure.data.HearingDayDetail;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingDayDetailConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDayDetailConverter.class);
    private static final EnumSet<Meridian> amMeridian = range(TWELVE_AM, TWELVE_PM);
    private static final EnumSet<Meridian> pmMeridian = range(ONE_PM, FIVE_PM);

    private HearingDayDetailConverter() {
    }

    public static List<HearingDayDetail> getHearingDayDetails(final List<uk.gov.justice.listing.events.HearingDay> hearingDays, final boolean isForAdjournmentHearing) {

        return hearingDays.stream()
                .map((uk.gov.justice.listing.events.HearingDay hearingDay) -> convertHearingDayToHearingDayDetail(hearingDay, isForAdjournmentHearing))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public static String getMeridian(final ZonedDateTime hearingDaySittingDay) {

        final String pattern = "HH";

        final String hour = hearingDaySittingDay.format(DateTimeFormatter.ofPattern(pattern));

        final boolean isAmMeridian = amMeridian.stream().anyMatch(meridian -> checkMeridian(meridian.getValue(), hour));
        final boolean isPmMeridian = pmMeridian.stream().anyMatch(meridian -> checkMeridian(meridian.getValue(), hour));

        if (isAmMeridian) {
            return "AM";
        }

        if (isPmMeridian) {
            return "PM";
        }

        LOGGER.info("Session {} does not fall within AM or PM range", hour);

        return "AD";
    }

    private static boolean checkMeridian(final String value, final String hour) {
        return valueOf(value).equals(hour);
    }

    private static HearingDayDetail convertHearingDayToHearingDayDetail(final uk.gov.justice.listing.events.HearingDay hearingDay, final boolean isForAdjournmentHearing) {
        final int duration = hearingDay.getDurationMinutes();
        final ZonedDateTime hearingDaySittingDay = hearingDay.getHearingDate().atStartOfDay(ZoneId.systemDefault());
        final LocalDate date = hearingDaySittingDay.toLocalDate();
        final Optional<UUID> courtScheduleId = hearingDay.getCourtScheduleId();
        final String session = StringUtils.trimToEmpty(getMeridian(hearingDaySittingDay));

        HearingDayDetail hearingDayDetail = null;
        if ("AD".equalsIgnoreCase(session) && isFalse(isForAdjournmentHearing)) {
            LOGGER.info("Is for Adjournment hearing:{}, session is {} and does not fall within AM or PM range. Slot will not be updated", isForAdjournmentHearing, session);
        } else {
            hearingDayDetail = new HearingDayDetail(date.toString(), getMeridian(hearingDaySittingDay), duration, courtScheduleId.isPresent() ? Optional.of(courtScheduleId.get().toString()) : Optional.empty());
        }
        return hearingDayDetail;
    }
}
