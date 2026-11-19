package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;
import static uk.gov.moj.cpp.listing.command.hearing.listener.HearingDayUtil.getNotCancelledHearingDays;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysCancelled;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


import org.apache.commons.collections.CollectionUtils;

@Component
public class HearingDaysForHearingEventListener implements ListenerPort {

    private static final String HEARING_DAYS = "hearingDays";
    private static final String ESTIMATED_MINUTES = "estimatedMinutes";
    private static final String EVENT_HEARING_DAYS_CANCELLED = "listing.events.hearing-days-cancelled";
    private final HearingRepository hearingRepository;
    private final HearingSearchSyncService hearingSearchSyncService;

    public HearingDaysForHearingEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(HearingDaysChangedForHearing.class, this::hearingDaysChangedForHearing)
                .on(HearingDaysSequenced.class, this::hearingDaysSequenced)
                .on(HearingDaysCancelled.class, this::hearingDaysCancelled);
    }

    public void hearingDaysChangedForHearing(final RaisedEvent<HearingDaysChangedForHearing> event) {
        final HearingDaysChangedForHearing hearingDaysChangedForHearing = event.payload();
        final List<HearingDay> hearingDays = hearingDaysChangedForHearing.getHearingDays();
        final UUID hearingId = hearingDaysChangedForHearing.getHearingId();

        if (nonNull(hearingRepository.findBy(hearingId))) {
            final JsonNodeUpdater hearing = using(hearingRepository).find(hearingId);
            hearing.putObjectList(HEARING_DAYS, hearingDays);
            if(CollectionUtils.isNotEmpty(hearingDays)){
                hearing.remove("unscheduled");
            }
            applyEstimatedMinutes(hearing, hearingDays);
            hearing.save();

            hearingSearchSyncService.sync(hearingId);
        }
    }
    public void hearingDaysSequenced(final RaisedEvent<HearingDaysSequenced> event) {
        final HearingDaysSequenced sequencedHearing = event.payload();

        final List<HearingDay> hearingDays = sequencedHearing.getHearingDays();
        final UUID hearingId = sequencedHearing.getHearingId();

        if (nonNull(hearingRepository.findBy(hearingId))) {
            final List<HearingDay> nonCancelledHearingDays = getNotCancelledHearingDays(hearingDays);
            final JsonNodeUpdater hearing = using(hearingRepository)
                    .find(hearingId)
                    .putObjectList(HEARING_DAYS, nonCancelledHearingDays);
            applyEstimatedMinutes(hearing, nonCancelledHearingDays);
            hearing.save();

            hearingSearchSyncService.sync(hearingId);
        }
    }
    public void hearingDaysCancelled(final RaisedEvent<HearingDaysCancelled> envelope) {
        final HearingDaysCancelled payload = envelope.payload();
        final List<HearingDay> hearingDays = payload.getHearingDays();
        final UUID hearingId = payload.getHearingId();

        final List<HearingDay> nonCancelledHearingDays = getNotCancelledHearingDays(hearingDays);

        final JsonNodeUpdater hearing = using(hearingRepository)
                .find(hearingId)
                .putObjectList(HEARING_DAYS, nonCancelledHearingDays);
        applyEstimatedMinutes(hearing, nonCancelledHearingDays);
        hearing.save();

        hearingSearchSyncService.sync(hearingId);
    }

    private static void applyEstimatedMinutes(final JsonNodeUpdater hearing, final List<HearingDay> hearingDays) {
        final int total = sumDurationMinutes(hearingDays);
        if (total > 0) {
            hearing.put(ESTIMATED_MINUTES, total);
        }
    }

    private static int sumDurationMinutes(final List<HearingDay> hearingDays) {
        if (hearingDays == null) {
            return 0;
        }
        return hearingDays.stream()
                .map(HearingDay::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
