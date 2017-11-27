package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.NotBeforeSelectedForHearing;
import uk.gov.moj.cpp.listing.event.NotBeforeUnselectedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class NotBeforeForHearingEventListener {

    private static final boolean NOT_BEFORE_UNSELECTED = false;
    private static final boolean NOT_BEFORE_SELECTED = true;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("listing.events.not-before-selected-for-hearing")
    public void notBeforeSelectedForHearing(final JsonEnvelope event) {
        final NotBeforeSelectedForHearing notBeforeSelectedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), NotBeforeSelectedForHearing.class);
        final UUID hearingId = UUID.fromString(notBeforeSelectedForHearing.getHearingId());
        hearingRepository.updateNotBefore(NOT_BEFORE_SELECTED, hearingId);
    }

    @Handles("listing.events.not-before-unselected-for-hearing")
    public void notBeforeUnselectedForHearing(final JsonEnvelope event) {
        final NotBeforeUnselectedForHearing notBeforeSelectedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), NotBeforeUnselectedForHearing.class);
        final UUID hearingId = UUID.fromString(notBeforeSelectedForHearing.getHearingId());
        hearingRepository.updateNotBefore(NOT_BEFORE_UNSELECTED, hearingId);
    }
}
