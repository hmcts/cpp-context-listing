package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class CourtRoomForHearingEventListener {

    private static final String COURT_ROOM_ID_FIELD = "courtRoomId";

    private HearingRepository hearingRepository;
    private HearingSearchSyncService hearingSearchSyncService;

    @Inject
    public CourtRoomForHearingEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Handles("listing.events.court-room-assigned-to-hearing")
    public void courtRoomAssignedToHearing(final Envelope<CourtRoomAssignedToHearing> event) {
        final CourtRoomAssignedToHearing courtRoomAssignedToHearing = event.payload();
        final UUID courtRoomId = courtRoomAssignedToHearing.getCourtRoomId();
        final UUID hearingId = courtRoomAssignedToHearing.getHearingId();
        using(hearingRepository)
                .find(hearingId)
                .put(COURT_ROOM_ID_FIELD, courtRoomId)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    @Handles("listing.events.court-room-changed-for-hearing")
    public void courtRoomChangedForHearing(final Envelope<CourtRoomChangedForHearing> event) {
        final CourtRoomChangedForHearing courtRoomChangedForHearing = event.payload();
        final UUID courtRoomId = courtRoomChangedForHearing.getCourtRoomId();
        final UUID hearingId = courtRoomChangedForHearing.getHearingId();
        using(hearingRepository)
                .find(hearingId)
                .put(COURT_ROOM_ID_FIELD, courtRoomId)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    @Handles("listing.events.court-room-removed-from-hearing")
    public void courtRoomRemovedFromHearing(final Envelope<CourtRoomRemovedFromHearing> event) {
        final CourtRoomRemovedFromHearing courtRoomRemovedFromHearing = event.payload();
        final UUID hearingId = courtRoomRemovedFromHearing.getHearingId();
        using(hearingRepository)
                .find(hearingId)
                .remove(COURT_ROOM_ID_FIELD)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
}
