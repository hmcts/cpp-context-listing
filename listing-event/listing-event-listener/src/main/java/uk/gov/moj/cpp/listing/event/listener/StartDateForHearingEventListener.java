package uk.gov.moj.cpp.listing.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.StartDateChangedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class StartDateForHearingEventListener {

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Handles("listing.events.start-date-changed-for-hearing")
    public void startDateChangedForHearing(final JsonEnvelope event) {
        final StartDateChangedForHearing startDateChangedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), StartDateChangedForHearing.class);
        final LocalDate startDate = startDateChangedForHearing.getStartDate();
        final UUID hearingId = UUID.fromString(startDateChangedForHearing.getHearingId());
        hearingRepository.updateStartDate(startDate, hearingId);
    }

 
}
