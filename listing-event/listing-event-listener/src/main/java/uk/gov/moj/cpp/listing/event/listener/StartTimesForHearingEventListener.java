package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.StartTimesAssignedToHearing;
import uk.gov.justice.listing.events.StartTimesChangedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.converter.StartTimesJsonConverter;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class StartTimesForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private StartTimesJsonConverter startTimesConverter;



    @Handles("listing.events.start-times-assigned-to-hearing")
    public void startTimesAssignedForHearing(final Envelope<StartTimesAssignedToHearing> event) {
        final StartTimesAssignedToHearing startTimeAssignedToHearing = event.payload();
        final List<ZonedDateTime> zonedStartTimes = startTimeAssignedToHearing.getStartTimes();
        final UUID hearingId = startTimeAssignedToHearing.getHearingId();

        String startTimesJson = startTimesConverter.convertStartTimesTo(zonedStartTimes);
        hearingRepository.updateStartTimes(startTimesJson, hearingId);
    }

    @Handles("listing.events.start-times-changed-for-hearing")
    public void startTimesChangedForHearing(final Envelope<StartTimesChangedForHearing> event) {
        final StartTimesChangedForHearing startTimeChangedForHearing =  event.payload();
        final List<ZonedDateTime> zonedStartTimes = startTimeChangedForHearing.getStartTimes();
        final UUID hearingId = startTimeChangedForHearing.getHearingId();

        String startTimesJSon = startTimesConverter.convertStartTimesTo(zonedStartTimes);
        hearingRepository.updateStartTimes(startTimesJSon, hearingId);
    }
}
