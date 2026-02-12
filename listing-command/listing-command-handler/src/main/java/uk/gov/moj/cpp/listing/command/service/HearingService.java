package uk.gov.moj.cpp.listing.command.service;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class HearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingService.class);
    private static final String HEARING_QUERY_BY_HEARING_ID = "listing.search.hearing";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_API)
    private HearingQueryView hearingQueryView;
    @SuppressWarnings({"squid:CallToDeprecatedMethod"})
    public JsonEnvelope getHearingById(final UUID hearingId, final JsonEnvelope event) {
        final JsonObject payload = createObjectBuilder().add("id", hearingId.toString()).build();
        LOGGER.info("listing.search.hearing request with payload {}", payload);

        final Metadata metaData = metadataFrom(event.metadata()).withName(HEARING_QUERY_BY_HEARING_ID).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metaData,payload);
        final JsonEnvelope request = enveloper.withMetadataFrom(jsonEnvelope, HEARING_QUERY_BY_HEARING_ID).apply(payload);

        return hearingQueryView.getHearingById(request);
    }

    @VisibleForTesting
    void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }

}
