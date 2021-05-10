package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static uk.gov.moj.cpp.platform.data.utils.date.MeridianUtil.getMeridian;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.toIsoString;

import uk.gov.moj.cpp.listing.event.processor.azure.data.HearingDayDetail;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingDayDetailConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDayDetailConverter.class);

    private HearingDayDetailConverter() {
    }

    public static List<HearingDayDetail> getHearingDayDetails(final List<uk.gov.justice.listing.events.HearingDay> hearingDays, final boolean isForAdjournmentHearing) {

        return hearingDays.stream()
                .map((uk.gov.justice.listing.events.HearingDay hearingDay) -> convertHearingDayToHearingDayDetail(hearingDay, isForAdjournmentHearing))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private static HearingDayDetail convertHearingDayToHearingDayDetail(final uk.gov.justice.listing.events.HearingDay hearingDay, final boolean isForAdjournmentHearing) {
        final int duration = hearingDay.getDurationMinutes();
        final ZonedDateTime hearingDaySittingDay = hearingDay.getHearingDate().atStartOfDay(ZoneId.systemDefault());
        final LocalDate date = hearingDaySittingDay.toLocalDate();
        final String session = StringUtils.trimToEmpty(getMeridian(hearingDaySittingDay));
        final String hearingStartTime = toIsoString(hearingDay.getStartTime());
        final Optional<String> courtScheduleId = hearingDay.getCourtScheduleId().map(UUID::toString).map(Optional::of).orElse(Optional.empty());
        final Optional<String> courtCentreId = hearingDay.getCourtCentreId().map(UUID::toString).map(Optional::of).orElse(Optional.empty());
        final Optional<String> courtRoomId = hearingDay.getCourtRoomId().map(UUID::toString).map(Optional::of).orElse(Optional.empty());

        HearingDayDetail hearingDayDetail = null;
        if ("AD".equalsIgnoreCase(session) && isFalse(isForAdjournmentHearing)) {
            LOGGER.info("Is for Adjournment hearing:{}, session is {} and does not fall within AM or PM range. Slot will not be updated", isForAdjournmentHearing, session);
        } else {
            hearingDayDetail = new HearingDayDetail(date.toString(), getMeridian(hearingDaySittingDay), duration, hearingStartTime, courtScheduleId, courtCentreId, courtRoomId);
        }
        return hearingDayDetail;
    }
}
