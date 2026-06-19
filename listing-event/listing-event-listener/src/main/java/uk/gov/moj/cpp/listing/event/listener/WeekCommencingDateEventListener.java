package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing;
import uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class WeekCommencingDateEventListener {

    private static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    private static final String WEEK_COMMENCING_DURATION = "weekCommencingDurationInWeeks";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;

    @Handles("listing.events.week-commencing-date-changed-for-hearing")
    public void weekCommencingAssignedForHearing(final Envelope<WeekCommencingDateChangedForHearing> event) {
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

    @Handles("listing.events.week-commencing-date-removed-for-hearing")
    public void weekCommencingRemovedForHearing(final Envelope<WeekCommencingDateRemovedForHearing> event) {
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
