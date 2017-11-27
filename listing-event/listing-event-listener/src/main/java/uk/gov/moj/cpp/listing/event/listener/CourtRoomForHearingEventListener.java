package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CourtRoomAssignedToHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomChangedForHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomRemovedFromHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class CourtRoomForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Handles("listing.events.court-room-assigned-to-hearing")
    public void courtRoomAssignedToHearing(final JsonEnvelope event) {
        final  CourtRoomAssignedToHearing courtRoomAssignedToHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtRoomAssignedToHearing.class);
        final UUID courtRoomId = UUID.fromString(courtRoomAssignedToHearing.getCourtRoomId());
        final UUID hearingId = UUID.fromString(courtRoomAssignedToHearing.getHearingId());
        hearingRepository.updateCourtRoomId(courtRoomId, hearingId);
    }

    @Handles("listing.events.court-room-changed-for-hearing")
    public void courtRoomChangedForHearing(final JsonEnvelope event) {
        final CourtRoomChangedForHearing courtRoomChangedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtRoomChangedForHearing.class);
        final UUID courtRoomId = UUID.fromString(courtRoomChangedForHearing.getCourtRoomId());
        final UUID hearingId = UUID.fromString(courtRoomChangedForHearing.getHearingId());
        hearingRepository.updateCourtRoomId(courtRoomId, hearingId);
    }

    @Handles("listing.events.court-room-removed-from-hearing")
    public void courtRoomRemovedFromHearing(final JsonEnvelope event) {
        final CourtRoomRemovedFromHearing courtRoomRemovedFromHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtRoomRemovedFromHearing.class);
        final UUID hearingId = UUID.fromString(courtRoomRemovedFromHearing.getHearingId());
        hearingRepository.updateCourtRoomId(null, hearingId);
    }
}
