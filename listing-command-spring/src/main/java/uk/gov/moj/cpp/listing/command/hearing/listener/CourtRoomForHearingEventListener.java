package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

import java.util.UUID;


@Component
public class CourtRoomForHearingEventListener implements ListenerPort {

    private static final String COURT_ROOM_ID_FIELD = "courtRoomId";

    private static final String PANEL = "panel";

    private HearingRepository hearingRepository;
    private HearingSearchSyncService hearingSearchSyncService;
    public CourtRoomForHearingEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(CourtRoomAssignedToHearing.class, this::courtRoomAssignedToHearing)
                .on(CourtRoomChangedForHearing.class, this::courtRoomChangedForHearing)
                .on(CourtRoomRemovedFromHearing.class, this::courtRoomRemovedFromHearing);
    }

    public void courtRoomAssignedToHearing(final RaisedEvent<CourtRoomAssignedToHearing> event) {
        final CourtRoomAssignedToHearing courtRoomAssignedToHearing = event.payload();
        final UUID courtRoomId = courtRoomAssignedToHearing.getCourtRoomId();
        final UUID hearingId = courtRoomAssignedToHearing.getHearingId();
        final JsonNodeUpdater hearing = using(hearingRepository).find(hearingId);
        hearing.put(COURT_ROOM_ID_FIELD, courtRoomId);
        if(nonNull(courtRoomAssignedToHearing.getPanel())) {
            hearing.put(PANEL, courtRoomAssignedToHearing.getPanel());
        }
        hearing.save();

        hearingSearchSyncService.sync(hearingId);
    }
    public void courtRoomChangedForHearing(final RaisedEvent<CourtRoomChangedForHearing> event) {
        final CourtRoomChangedForHearing courtRoomChangedForHearing = event.payload();
        final UUID courtRoomId = courtRoomChangedForHearing.getCourtRoomId();
        final UUID hearingId = courtRoomChangedForHearing.getHearingId();
        final JsonNodeUpdater hearing = using(hearingRepository).find(hearingId);
        hearing.put(COURT_ROOM_ID_FIELD, courtRoomId);
        if(nonNull(courtRoomChangedForHearing.getPanel())) {
            hearing.put(PANEL, courtRoomChangedForHearing.getPanel());
        }
        hearing.save();

        hearingSearchSyncService.sync(hearingId);
    }
    public void courtRoomRemovedFromHearing(final RaisedEvent<CourtRoomRemovedFromHearing> event) {
        final CourtRoomRemovedFromHearing courtRoomRemovedFromHearing = event.payload();
        final UUID hearingId = courtRoomRemovedFromHearing.getHearingId();
        using(hearingRepository)
                .find(hearingId)
                .remove(COURT_ROOM_ID_FIELD)
                .remove(PANEL)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
}
