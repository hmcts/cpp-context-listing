package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.CourtCentreChangedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;


@Component
public class CourtCentreEventListener implements ListenerPort {

    private static final String COURT_CENTRE_ID_FIELD = "courtCentreId";

    private HearingRepository hearingRepository;

    private HearingSearchSyncService hearingSearchSyncService;
    public CourtCentreEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(CourtCentreChangedForHearing.class, this::courtCentreChangedForHearing);
    }

    public void courtCentreChangedForHearing(final RaisedEvent<CourtCentreChangedForHearing> event) {
        final CourtCentreChangedForHearing courtCentreChangedForHearing = event.payload();
        final UUID courtCentreId = courtCentreChangedForHearing.getCourtCentreId();
        final UUID hearingId = courtCentreChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .put(COURT_CENTRE_ID_FIELD, courtCentreId)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
}
