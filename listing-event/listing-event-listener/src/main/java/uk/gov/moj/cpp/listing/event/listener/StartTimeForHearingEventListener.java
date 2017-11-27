package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.StartTimeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.StartTimeChangedForHearing;
import uk.gov.moj.cpp.listing.event.StartTimeRemovedFromHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalTime;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class StartTimeForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Handles("listing.events.start-time-assigned-to-hearing")
    public void startTimeAssignedForHearing(final JsonEnvelope event) {
        final StartTimeAssignedToHearing startTimeAssignedToHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), StartTimeAssignedToHearing.class);
        final LocalTime startTime = startTimeAssignedToHearing.getStartTime();
        final UUID hearingId = UUID.fromString(startTimeAssignedToHearing.getHearingId());
        hearingRepository.updateStartTime(startTime, hearingId);
    }

    @Handles("listing.events.start-time-changed-for-hearing")
    public void startTimeChangedForHearing(final JsonEnvelope event) {
        final StartTimeChangedForHearing startTimeChangedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), StartTimeChangedForHearing.class);
        final LocalTime startTime = startTimeChangedForHearing.getStartTime();
        final UUID hearingId = UUID.fromString(startTimeChangedForHearing.getHearingId());
        hearingRepository.updateStartTime(startTime, hearingId);
    }

    @Handles("listing.events.start-time-removed-from-hearing")
    public void startTimeRemovedFromHearing(final JsonEnvelope event) {
        final StartTimeRemovedFromHearing startTimeRemovedFromHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), StartTimeRemovedFromHearing.class);
        final UUID hearingId = UUID.fromString(startTimeRemovedFromHearing.getHearingId());
        hearingRepository.updateStartTime(null, hearingId);
    }
}
