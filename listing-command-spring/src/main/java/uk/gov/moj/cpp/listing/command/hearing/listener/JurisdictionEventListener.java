package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.JurisdictionChangedForHearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;


@Component
public class JurisdictionEventListener implements ListenerPort {

    private static final String JURISDICTION_TYPE_FIELD = "jurisdictionType";

    private HearingRepository hearingRepository;

    private HearingSearchSyncService hearingSearchSyncService;
    public JurisdictionEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(JurisdictionChangedForHearing.class, this::courtRoomAssignedToHearing);
    }

    public void courtRoomAssignedToHearing(final RaisedEvent<JurisdictionChangedForHearing> event) {
        final JurisdictionChangedForHearing payload = event.payload();
        final UUID hearingId = payload.getHearingId();
        final JurisdictionType jurisdictionType = payload.getJurisdictionType();
        using(hearingRepository)
                .find(hearingId)
                .put(JURISDICTION_TYPE_FIELD, jurisdictionType.toString())
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
}
