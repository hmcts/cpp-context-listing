package uk.gov.moj.cpp.listing.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class ListingEventProcessor {

    static final String CASE_SENT_FOR_LISTING_PUB_EVENT = "listing.case-sent-for-listing";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("listing.events.case-sent-for-listing")
    public void handleCaseSentForListingMessage(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final CaseSentForListing event = jsonObjectConverter.convert(payload, CaseSentForListing.class);
        sender.send(enveloper.withMetadataFrom(envelope, CASE_SENT_FOR_LISTING_PUB_EVENT).apply(event));
    }
}
