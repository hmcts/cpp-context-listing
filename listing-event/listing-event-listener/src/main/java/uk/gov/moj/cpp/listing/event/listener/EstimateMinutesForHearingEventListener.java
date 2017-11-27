package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.EstimateMinutesChangedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class EstimateMinutesForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Handles("listing.events.estimate-minutes-changed-for-hearing")
    public void estimateMinutesChangedForHearing(final JsonEnvelope event) {
        final EstimateMinutesChangedForHearing estimateMinutesChangedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), EstimateMinutesChangedForHearing.class);
        final Integer estimateMinutes = estimateMinutesChangedForHearing.getEstimateMinutes();
        final UUID hearingId = UUID.fromString(estimateMinutesChangedForHearing.getHearingId());
        hearingRepository.updateEstimateMinutes(estimateMinutes, hearingId);
    }
}
