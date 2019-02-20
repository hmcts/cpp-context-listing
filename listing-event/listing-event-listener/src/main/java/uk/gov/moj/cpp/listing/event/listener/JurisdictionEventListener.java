package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.JurisdictionChangedForHearing;
import uk.gov.justice.listing.events.JurisdictionType;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class JurisdictionEventListener {

    private static final String JURISDICTION_TYPE_FIELD = "jurisdictionType";

    private HearingRepository hearingRepository;

    @Inject
    public JurisdictionEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.jurisdiction-changed-for-hearing")
    public void courtRoomAssignedToHearing(final Envelope<JurisdictionChangedForHearing> event) {
        final JurisdictionChangedForHearing payload = event.payload();
        final UUID hearingId = payload.getHearingId();
        final JurisdictionType jurisdictionType = payload.getJurisdictionType();
        using(hearingRepository)
                .find(hearingId)
                .put(JURISDICTION_TYPE_FIELD, jurisdictionType.toString())
                .save();
    }
}
