package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;


import java.util.Optional;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.StartDateRemovedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

@Component
public class StartDateForHearingEventListener implements ListenerPort {

    private static final String START_DATE = "startDate";
    private final HearingRepository hearingRepository;
    private final HearingSearchSyncService hearingSearchSyncService;

    public StartDateForHearingEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(StartDateChangedForHearing.class, this::startDateChangedForHearing)
                .on(StartDateRemovedForHearing.class, this::startDateRemovedForHearing);
    }

    public void startDateChangedForHearing(final RaisedEvent<StartDateChangedForHearing> event) {
        final StartDateChangedForHearing startDateChangedForHearing = event.payload();
        final LocalDate startDate = parse(startDateChangedForHearing.getStartDate());
        final UUID hearingId = startDateChangedForHearing.getHearingId();

        if (nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .put(START_DATE, startDate)
                    .save();

            hearingSearchSyncService.sync(hearingId);
        }
    }
    public void startDateRemovedForHearing(final RaisedEvent<StartDateRemovedForHearing> event) {
        final StartDateRemovedForHearing startDateRemovedForHearing = event.payload();
        final UUID hearingId = startDateRemovedForHearing.getHearingId();
        final JsonNodeUpdater updater = using(hearingRepository)
                .find(hearingId)
                .remove(START_DATE);

        if (Optional.ofNullable(startDateRemovedForHearing.getUnscheduled()).orElse(false)) {
            updater.put("unscheduled", true);
        }
        updater.save();
        hearingSearchSyncService.sync(hearingId);
    }
}
