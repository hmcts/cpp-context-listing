package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay;

import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;
import java.util.Optional;

public class HearingDaysCommandToDomainConverter implements Converter<List<HearingDay>, List<uk.gov.moj.cpp.listing.domain.HearingDay>> {

    @Override
    @SuppressWarnings({"squid:S3358"})
    public List<uk.gov.moj.cpp.listing.domain.HearingDay> convert(final List<HearingDay> source) {
        return isEmpty(source) ? emptyList() : source.stream()
                .map(hearingDay -> hearingDay()
                        .withCourtRoomId(Optional.ofNullable(hearingDay.getCourtRoomId()))
                        .withCourtCentreId(Optional.of(hearingDay.getCourtCentreId()))
                        .withDurationMinutes(hearingDay.getDurationMinutes())
                        .withSequence(hearingDay.getSequence())
                        .withIsCancelled(Optional.ofNullable(hearingDay.getIsCancelled()))
                        .withCourtScheduleId(Optional.ofNullable(hearingDay.getCourtScheduleId()))
                        .withIsDraft(Optional.ofNullable(hearingDay.getIsDraft()))
                        .withHearingDate(hearingDay.getHearingDate())
                        .withStartTime(hearingDay.getStartTime())
                        .withEndTime(nonNull(hearingDay.getEndTime()) ? hearingDay.getEndTime()
                                : nonNull(hearingDay.getStartTime()) && nonNull(hearingDay.getDurationMinutes())
                                        ? hearingDay.getStartTime().plusMinutes(hearingDay.getDurationMinutes())
                                        : hearingDay.getStartTime())
                        .build())
                .toList();
    }
}
