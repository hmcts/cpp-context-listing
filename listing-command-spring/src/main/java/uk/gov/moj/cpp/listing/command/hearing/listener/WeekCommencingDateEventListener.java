package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing;
import uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;


@Component
public class WeekCommencingDateEventListener implements ListenerPort {

    private static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    private static final String WEEK_COMMENCING_DURATION = "weekCommencingDurationInWeeks";
    private final HearingRepository hearingRepository;
    private final HearingSearchSyncService hearingSearchSyncService;

    public WeekCommencingDateEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(WeekCommencingDateChangedForHearing.class, this::weekCommencingAssignedForHearing)
                .on(WeekCommencingDateRemovedForHearing.class, this::weekCommencingRemovedForHearing);
    }

    public void weekCommencingAssignedForHearing(final RaisedEvent<WeekCommencingDateChangedForHearing> event) {
        final WeekCommencingDateChangedForHearing weekCommencingDateChangedForHearing = event.payload();
        final LocalDate weekCommencingStartDate = LocalDate.parse(weekCommencingDateChangedForHearing.getWeekCommencingStartDate());
        final LocalDate weekCommencingEndDate = LocalDate.parse(weekCommencingDateChangedForHearing.getWeekCommencingEndDate());
        final Integer weekCommencingDurationInWeeks = weekCommencingDateChangedForHearing.getWeekCommencingDurationInWeeks();
        final UUID hearingId = weekCommencingDateChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .put(WEEK_COMMENCING_START_DATE, weekCommencingStartDate)
                .put(WEEK_COMMENCING_END_DATE, weekCommencingEndDate)
                .put(WEEK_COMMENCING_DURATION, weekCommencingDurationInWeeks.toString())
                .remove("unscheduled")
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
    public void weekCommencingRemovedForHearing(final RaisedEvent<WeekCommencingDateRemovedForHearing> event) {
        final WeekCommencingDateRemovedForHearing weekCommencingDateRemovedFromHearing = event.payload();
        final UUID hearingId = weekCommencingDateRemovedFromHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .remove(WEEK_COMMENCING_START_DATE)
                .remove(WEEK_COMMENCING_END_DATE)
                .remove(WEEK_COMMENCING_DURATION)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
}
