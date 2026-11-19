package uk.gov.moj.cpp.listing.api.skeleton;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <b>P2.0 SKELETON — TEST-ONLY. DELETE in P2.3 (commands).</b>
 *
 * <p>A canned command endpoint on a real spec path, so the P2.0 integration tests can drive the
 * command-side filter chain (routing guard, schema validation, cp-auth-rules-filter with the
 * ported DRL, audit, error handler) before the command controllers exist. It does not touch any
 * data. P2.1 removed the canned query endpoint ({@code GET /hearings/{id}}): the real
 * HearingQueriesApi serves it.
 * Only created when {@code listing.p20-skeleton.enabled=true}, which only the {@code integration}
 * profile sets; the default is {@code false}, so production never serves them.
 *
 * <p>Deliberately not implementing the generated {@code *OpenApi} interfaces: the real controllers
 * implement those (all operations of a tag) in P2.3.
 */
@RestController
@ConditionalOnProperty(name = "listing.p20-skeleton.enabled", havingValue = "true")
public class P20SkeletonApi {

    static final String CREATE_LISTING_NOTE = "application/vnd.listing.command.create-listing-note+json";

    @PostMapping(value = "/command/api/rest/listing/listing-note", consumes = CREATE_LISTING_NOTE)
    public ResponseEntity<Void> createListingNote(@RequestBody final Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
