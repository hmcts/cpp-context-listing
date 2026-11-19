package uk.gov.moj.cpp.listing.command.hearing.api.service;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.moj.cpp.listing.query.view.exception.QueryNotFoundException;

import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The stored hearing, read from the listing viewstore in-process as {@code listing.search.hearing}
 * (MBD refactor P2.3.2; port of the JEE command-api {@code HearingLookupService}).
 */
@Component("hearingApiHearingLookupService")
public class HearingLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingLookupService.class);

    private final HearingQueryView hearingQueryView;

    public HearingLookupService(final HearingQueryView hearingQueryView) {
        this.hearingQueryView = hearingQueryView;
    }

    public Optional<JsonObject> findHearing(final UUID hearingId, final Envelope envelope) {
        final JsonObject payload = createObjectBuilder().add("id", hearingId.toString()).build();
        try {
            return Optional.ofNullable(hearingQueryView.getHearingById(payload));
        } catch (final QueryNotFoundException e) {
            LOGGER.debug("No hearing found for hearingId {}", hearingId, e);
            return Optional.empty();
        }
    }
}
