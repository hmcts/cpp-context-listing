package uk.gov.moj.cpp.listing.command.api.service;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.HearingQueryClient;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PtphDetailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtphDetailService.class);

    private static final String TIER = "tier";
    private static final String LIST_TYPE = "listType";
    private static final String KEY_REASON = "keyReason";
    private static final String FINALISED = "finalised";

    @Inject
    private HearingQueryClient hearingQueryClient;

    /**
     * Returns the seeding hearing's tier / list type / key reason, but only when the hearing
     * context reports the record as finalised. A missing record arrives either as a 404 or as a
     * successful response with {@code finalised=false}, so both are covered.
     *
     * <p>Query failures are deliberately not caught: {@link HearingQueryClient} throws, and a
     * swallowed failure would produce a blank trial hearing indistinguishable from a legitimately
     * blank one.
     *
     * <p>The envelope is no longer needed — the client authenticates as the context system user
     * rather than propagating the caller's metadata — but is kept in the signature so callers and
     * their tests are unaffected.
     */
    public Optional<PtphDetail> getFinalisedPtphDetail(final UUID seedingHearingId, final JsonEnvelope envelope) {
        final Optional<JsonObject> response = hearingQueryClient.getPtphDetail(seedingHearingId);

        if (response.isEmpty() || !response.get().getBoolean(FINALISED, false)) {
            LOGGER.info("No finalised tier/list type for seeding hearing {}", seedingHearingId);
            return Optional.empty();
        }

        final JsonObject payload = response.get();
        return Optional.of(new PtphDetail(
                stringOrNull(payload, TIER),
                stringOrNull(payload, LIST_TYPE),
                stringOrNull(payload, KEY_REASON)));
    }

    private String stringOrNull(final JsonObject payload, final String field) {
        return payload.containsKey(field) && !payload.isNull(field) ? payload.getString(field) : null;
    }
}
