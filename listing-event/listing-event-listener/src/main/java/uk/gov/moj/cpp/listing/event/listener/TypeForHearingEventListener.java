package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.TypeChangedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class TypeForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;



    @Handles("listing.events.type-changed-for-hearing")
    public void typeChangedForHearing(final JsonEnvelope event) {
        final TypeChangedForHearing typeChangedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), TypeChangedForHearing.class);
        final UUID hearingId = UUID.fromString(typeChangedForHearing.getHearingId());
        hearingRepository.updateType(typeChangedForHearing.getType(), hearingId);
    }
}
