package uk.gov.moj.cpp.listing.command.api.service;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PtphDetailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtphDetailService.class);

    private static final String HEARING_QUERY_PTPH_DETAIL = "hearing.get-ptph-detail";
    private static final String HEARING_ID = "hearingId";
    private static final String TIER = "tier";
    private static final String LIST_TYPE = "listType";
    private static final String KEY_REASON = "keyReason";
    private static final String FINALISED = "finalised";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    /**
     * Returns the seeding hearing's tier / list type / key reason, but only when the
     * hearing context reports the record as finalised. A missing record arrives as a
     * successful response with finalised=false, so it is covered by the same check.
     * Query failures are deliberately not caught: a swallowed failure would produce a
     * blank trial hearing indistinguishable from a legitimately blank one.
     */
    public Optional<PtphDetail> getFinalisedPtphDetail(final UUID seedingHearingId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(HEARING_ID, seedingHearingId.toString()).build();

        final Envelope<JsonObject> request = Enveloper.envelop(payload)
                .withName(HEARING_QUERY_PTPH_DETAIL)
                .withMetadataFrom(envelope);

        final JsonEnvelope response = requester.requestAsAdmin(envelopeFrom(request.metadata(), request.payload()));
        final JsonObject responsePayload = response.payloadAsJsonObject();

        if (!responsePayload.getBoolean(FINALISED, false)) {
            LOGGER.info("No finalised tier/list type for seeding hearing {}", seedingHearingId);
            return Optional.empty();
        }

        return Optional.of(new PtphDetail(
                stringOrNull(responsePayload, TIER),
                stringOrNull(responsePayload, LIST_TYPE),
                stringOrNull(responsePayload, KEY_REASON)));
    }

    private String stringOrNull(final JsonObject payload, final String field) {
        return payload.containsKey(field) && !payload.isNull(field) ? payload.getString(field) : null;
    }
}
