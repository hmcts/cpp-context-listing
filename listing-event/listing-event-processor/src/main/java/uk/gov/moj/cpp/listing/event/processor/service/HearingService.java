package uk.gov.moj.cpp.listing.event.processor.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.justice.listing.events.Hearing;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingService.class);
    private static final String HEARING_QUERY_BY_HEARING_ID = "listing.search.hearing";

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    @ServiceComponent(QUERY_API)
    private HearingQueryView hearingQueryView;

    public JsonEnvelope getHearingById(final UUID hearingId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", hearingId.toString()).build();
        LOGGER.info("listing.search.hearing request with payload {}", payload);

        final Metadata metaData = metadataFrom(event.metadata()).withName(HEARING_QUERY_BY_HEARING_ID).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metaData,payload);
        final JsonEnvelope request = enveloper.withMetadataFrom(jsonEnvelope, HEARING_QUERY_BY_HEARING_ID).apply(payload);

        return hearingQueryView.getHearingById(request);
    }

    public Hearing getHearing(final UUID hearingId, final JsonEnvelope envelope){
        final JsonEnvelope hearingEnvelope = getHearingById(hearingId, envelope);
        final JsonObject jsonObject = hearingEnvelope.payloadAsJsonObject();
        return jsonObjectToObjectConverter.convert(jsonObject, Hearing.class);
    }

    @VisibleForTesting
    void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }

}
