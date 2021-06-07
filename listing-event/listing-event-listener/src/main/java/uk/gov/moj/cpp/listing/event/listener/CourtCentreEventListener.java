package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.CourtCentreChangedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class CourtCentreEventListener {

    private static final String COURT_CENTRE_ID_FIELD = "courtCentreId";

    private HearingRepository hearingRepository;

    private HearingSearchSyncService hearingSearchSyncService;

    @Inject
    public CourtCentreEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Handles("listing.events.court-centre-changed-for-hearing")
    public void courtCentreChangedForHearing(final Envelope<CourtCentreChangedForHearing> event) {
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
