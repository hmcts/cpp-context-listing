package uk.gov.moj.cpp.listing.command.api.service;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronous, in-process lookup of a hearing from the listing viewstore, for command-api-side
 * existence/validation checks that must happen before a command is sent (e.g.
 * move-hearing-to-past-date's unknown-hearingId 422). Mirrors the
 * {@code uk.gov.moj.cpp.listing.command.service.HearingService} pattern already used from
 * listing-command-handler, moved into command-api since the check has to happen here to be able
 * to reject with a synchronous 422 before anything is sent.
 */
@ApplicationScoped
public class HearingLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingLookupService.class);
    private static final String HEARING_QUERY_BY_HEARING_ID = "listing.search.hearing";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_API)
    private HearingQueryView hearingQueryView;

    public Optional<JsonObject> findHearing(final UUID hearingId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("id", hearingId.toString()).build();

        final Metadata metadata = metadataFrom(envelope.metadata()).withName(HEARING_QUERY_BY_HEARING_ID).build();
        final JsonEnvelope query = JsonEnvelope.envelopeFrom(metadata, payload);
        final JsonEnvelope request = enveloper.withMetadataFrom(query, HEARING_QUERY_BY_HEARING_ID).apply(payload);

        try {
            final JsonEnvelope response = hearingQueryView.getHearingById(request);
            return Optional.of(response.payloadAsJsonObject());
        } catch (final NotFoundException e) {
            LOGGER.debug("No hearing found for hearingId {}", hearingId, e);
            return Optional.empty();
        }
    }
}
