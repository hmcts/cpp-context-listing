package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.EndDateRemovedFromHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;


@Component
public class EndDateForHearingEventListener implements ListenerPort {

    private static final String END_DATE_FIELD = "endDate";

    private HearingRepository hearingRepository;

    private HearingSearchSyncService hearingSearchSyncService;
    public EndDateForHearingEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(EndDateChangedForHearing.class, this::endDateChangedForHearing)
                .on(EndDateRemovedFromHearing.class, this::endDateRemovedFromHearing);
    }

    public void endDateChangedForHearing(final RaisedEvent<EndDateChangedForHearing> event) {
        final EndDateChangedForHearing endDateChangedForHearing = event.payload();
        final LocalDate endDate = parse(endDateChangedForHearing.getEndDate());
        final UUID hearingId = endDateChangedForHearing.getHearingId();

        if (nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .put(END_DATE_FIELD, endDate)
                    .save();
            hearingSearchSyncService.sync(hearingId);
        }
    }
    public void endDateRemovedFromHearing(final RaisedEvent<EndDateRemovedFromHearing> event) {
        final EndDateRemovedFromHearing endDateRemovedFromHearing = event.payload();
        final UUID hearingId = endDateRemovedFromHearing.getHearingId();
        using(hearingRepository)
                .find(hearingId)
                .remove(END_DATE_FIELD)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
}
                                                                                                                                                                                           