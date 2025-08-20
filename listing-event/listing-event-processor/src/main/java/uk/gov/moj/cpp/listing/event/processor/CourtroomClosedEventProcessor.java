package uk.gov.moj.cpp.listing.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.CacheRefDataCourtroomView;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtroomClosedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtroomClosedEventProcessor.class);
    private static final String PUBLIC_REFERENCE_COURTROOM_CLOSED = "public.referencedata.event.courtroom-closed";
    private static final String LISTING_UPDATE_CLOSE_COURTROOM = "listing.update.close-courtroom";

    @Inject
    @ServiceComponent(QUERY_API)
    private CacheRefDataCourtroomView cacheRefDataCourtroomView;

    @Handles(PUBLIC_REFERENCE_COURTROOM_CLOSED)
    public void closeReferenceDataCourtRoom(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("{} event received with payload {}",
                PUBLIC_REFERENCE_COURTROOM_CLOSED,
                jsonEnvelope.payloadAsJsonObject());

        cacheRefDataCourtroomView.closeRefDataCourtroom(envelopeFrom(metadataFrom(jsonEnvelope.metadata())
                        .withName(LISTING_UPDATE_CLOSE_COURTROOM),
                jsonEnvelope.payloadAsJsonObject()));
    }
}