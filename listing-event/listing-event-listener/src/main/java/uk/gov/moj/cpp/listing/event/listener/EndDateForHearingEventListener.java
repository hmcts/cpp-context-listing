package uk.gov.moj.cpp.listing.event.listener;

import static java.time.LocalDate.parse;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.EndDateRemovedFromHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class EndDateForHearingEventListener {

    private static final String END_DATE_FIELD = "endDate";

    private HearingRepository hearingRepository;

    @Inject
    public EndDateForHearingEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.end-date-changed-for-hearing")
    public void endDateChangedForHearing(final Envelope<EndDateChangedForHearing> event) {
        final EndDateChangedForHearing endDateChangedForHearing = event.payload();
        final LocalDate endDate = parse(endDateChangedForHearing.getEndDate());
        final UUID hearingId = endDateChangedForHearing.getHearingId();
        using(hearingRepository)
                .find(hearingId)
                .put(END_DATE_FIELD, endDate)
                .save();
    }

    @Handles("listing.events.end-date-removed-from-hearing")
    public void endDateRemovedFromHearing(final Envelope<EndDateRemovedFromHearing> event) {
        final EndDateRemovedFromHearing endDateRemovedFromHearing = event.payload();
        final UUID hearingId = endDateRemovedFromHearing.getHearingId();
        using(hearingRepository)
                .find(hearingId)
                .remove(END_DATE_FIELD)
                .save();
    }
}
                                                                                                                                                                                           