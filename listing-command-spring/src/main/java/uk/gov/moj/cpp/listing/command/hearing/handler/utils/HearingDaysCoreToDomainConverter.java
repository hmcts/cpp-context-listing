package uk.gov.moj.cpp.listing.command.hearing.handler.utils;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.listing.events.HearingDay.hearingDay;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.Converter;

import java.util.List;

@org.springframework.stereotype.Component("hearingHandlerHearingDaysCoreToDomainConverter")
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
                        .withEndTime(hearingDay.getSittingDay().plusMinutes(hearingDay.getListedDurationMinutes()))
                        .build())
                .collect(toList());
    }
}
