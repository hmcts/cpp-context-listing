package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class TypeForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;


    @Handles("listing.events.type-changed-for-hearing")
    public void typeChangedForHearing(final Envelope<TypeChangedForHearing> event) {
        final TypeChangedForHearing typeChangedForHearing =   event.payload();
        final UUID hearingId = typeChangedForHearing.getHearingId();
        hearingRepository.updateType(typeChangedForHearing.getType(), hearingId);
    }
}
