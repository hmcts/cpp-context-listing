package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.Type;
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

    private static final String TYPE_FIELD = "type";

    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.type-changed-for-hearing")
    public void typeChangedForHearing(final Envelope<TypeChangedForHearing> event) {
        final TypeChangedForHearing typeChangedForHearing =   event.payload();
        final Type type = typeChangedForHearing.getType();
        final UUID hearingId = typeChangedForHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .putObject(TYPE_FIELD, type)
                .save();
    }
}
