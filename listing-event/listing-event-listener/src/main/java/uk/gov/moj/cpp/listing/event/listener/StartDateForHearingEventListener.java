package uk.gov.moj.cpp.listing.event.listener;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;


import java.util.Optional;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.StartDateRemovedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

@ServiceComponent(Component.EVENT_LISTENER)
public class StartDateForHearingEventListener {

    private static final String START_DATE = "startDate";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;

    @Handles("listing.events.start-date-changed-for-hearing")
    public void startDateChangedForHearing(final Envelope<StartDateChangedForHearing> event) {
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


    @Handles("listing.events.start-date-removed-for-hearing")
    public void startDateRemovedForHearing(final Envelope<StartDateRemovedForHearing> event) {
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
