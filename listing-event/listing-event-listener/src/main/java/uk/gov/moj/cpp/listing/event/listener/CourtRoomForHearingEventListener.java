package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class CourtRoomForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;


    @Handles("listing.events.court-room-assigned-to-hearing")
    public void courtRoomAssignedToHearing(final Envelope<CourtRoomAssignedToHearing> event) {
        final CourtRoomAssignedToHearing courtRoomAssignedToHearing = event.payload();
        final UUID courtRoomId = courtRoomAssignedToHearing.getCourtRoomId();
        final UUID hearingId = courtRoomAssignedToHearing.getHearingId();
        hearingRepository.updateCourtRoomId(courtRoomId, hearingId);
    }

    @Handles("listing.events.court-room-changed-for-hearing")
    public void courtRoomChangedForHearing(final Envelope<CourtRoomChangedForHearing> event) {
        final CourtRoomChangedForHearing courtRoomChangedForHearing = event.payload();
        final UUID courtRoomId = courtRoomChangedForHearing.getCourtRoomId();
        final UUID hearingId = courtRoomChangedForHearing.getHearingId();
        hearingRepository.updateCourtRoomId(courtRoomId, hearingId);
    }

    @Handles("listing.events.court-room-removed-from-hearing")
    public void courtRoomRemovedFromHearing(final Envelope<CourtRoomRemovedFromHearing> event) {
        final CourtRoomRemovedFromHearing courtRoomRemovedFromHearing = event.payload();
        final UUID hearingId = courtRoomRemovedFromHearing.getHearingId();
        hearingRepository.updateCourtRoomId(null, hearingId);
    }
}
