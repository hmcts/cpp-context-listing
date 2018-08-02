package uk.gov.moj.cpp.listing.event.listener;

import static java.time.LocalDate.parse;

import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class StartDateForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;


    @Handles("listing.events.start-date-changed-for-hearing")
    public void startDateChangedForHearing(final Envelope<StartDateChangedForHearing> event) {
        final StartDateChangedForHearing startDateChangedForHearing = event.payload();
        final LocalDate startDate = parse(startDateChangedForHearing.getStartDate());
        final UUID hearingId = startDateChangedForHearing.getHearingId();
        hearingRepository.updateStartDate(startDate, hearingId);
    }

 
}
