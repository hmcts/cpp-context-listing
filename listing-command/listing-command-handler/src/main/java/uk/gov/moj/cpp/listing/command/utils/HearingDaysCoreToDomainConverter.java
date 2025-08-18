package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.listing.events.HearingDay.hearingDay;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;

public class HearingDaysCoreToDomainConverter implements Converter<List<HearingDay>, List<uk.gov.justice.listing.events.HearingDay>> {

    @Override
    public List<uk.gov.justice.listing.events.HearingDay> convert(final List<HearingDay> source) {
        return source.stream()
                .map(hearingDay -> hearingDay()
                        .withCourtRoomId(hearingDay.getCourtRoomId())
                        .withCourtCentreId(hearingDay.getCourtCentreId())
                        .withHearingDate(hearingDay.getSittingDay().toLocalDate())
                        .withStartTime(hearingDay.getSittingDay())
                        .withSequence(hearingDay.getListingSequence())
                        .withDurationMinutes(hearingDay.getListedDurationMinutes())
                        .withIsCancelled(hearingDay.getIsCancelled())
                        .build())
                .collect(toList());
    }
}
