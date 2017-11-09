package uk.gov.moj.cpp.listing.event.listener;


import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.listing.event.HearingUpdatedForListing;
import uk.gov.moj.cpp.listing.event.converter.HearingConverter;
import uk.gov.moj.cpp.listing.event.converter.HearingUpdatedConverter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Set;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingConverter hearingConverter;

    @Inject
    private HearingUpdatedConverter hearingUpdatedConverter;


    @Handles("listing.events.case-sent-for-listing")
    public void caseSentForListing(final JsonEnvelope event) {
        final Set<Hearing> hearings = hearingConverter.convert(jsonObjectConverter
                .convert(event.payloadAsJsonObject(), CaseSentForListing.class));
        hearings.stream().forEach(hearingRepository::save);

    }

    @Handles("listing.events.hearing-updated-for-listing")
    public void hearingUpdatedForListing(final JsonEnvelope event) {
        final Hearing hearing = hearingUpdatedConverter.convert(jsonObjectConverter
                .convert(event.payloadAsJsonObject(), HearingUpdatedForListing.class));
        hearingRepository.save(hearing);

    }
}
