package uk.gov.moj.cpp.listing.event.listener;

import static java.time.LocalDate.parse;

import uk.gov.justice.listing.events.EndDateAssignedToHearing;
import uk.gov.justice.listing.events.EndDateChangedForHearing;
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

    @Inject
    private HearingRepository hearingRepository;


    @Handles("listing.events.end-date-changed-for-hearing")
    public void endDateChangedForHearing(final Envelope<EndDateChangedForHearing> event) {
        final EndDateChangedForHearing endDateChangedForHearing = event.payload();
        final LocalDate endDate = parse(endDateChangedForHearing.getEndDate());
        final UUID hearingId = endDateChangedForHearing.getHearingId();
        hearingRepository.updateEndDate(endDate, hearingId);
    }

    @Handles("listing.events.end-date-assigned-to-hearing")
    public void endDateAssignedToHearing(final Envelope<EndDateAssignedToHearing> event) {
        final EndDateAssignedToHearing endDateAssignedToHearing = event.payload();
        final LocalDate endDate = parse(endDateAssignedToHearing.getEndDate());
        final UUID hearingId = endDateAssignedToHearing.getHearingId();
        hearingRepository.updateEndDate(endDate, hearingId);
    }

}
                                                                                                                                                                                           