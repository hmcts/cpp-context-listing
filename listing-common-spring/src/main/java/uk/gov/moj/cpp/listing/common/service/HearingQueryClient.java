package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;

import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;

import java.io.StringReader;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads from the hearing context's query API over HTTP (MBD refactor P2.3.3: the JEE
 * {@code HearingQueryClient} of listing-common, Apache HttpClient → Spring {@link RestClient}).
 * Same path, {@code Accept} media type and {@code CJSCPPUID} (the context system user); the root is
 * {@code listing.clients.hearing-query-base-url} (legacy {@code hearing.query.base.url}).
 *
 * <p>{@link #get(String, String)}: a 404 is {@code Optional.empty()}; any other non-200, or an I/O
 * failure, throws {@link HearingQueryException}: a swallowed failure would be indistinguishable from
 * a genuine absence.
 */
@Component
public class HearingQueryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryClient.class);

    private static final String PTPH_DETAIL_PATH = "/hearings/%s/ptph-detail";
    private static final String PTPH_DETAIL_MEDIA_TYPE = "application/vnd.hearing.get-ptph-detail+json";

    private static final String CJS_CPP_UID = "CJSCPPUID";

    private final RestClient restClient;
    private final ListingClientsProperties properties;

    public HearingQueryClient(final RestClient cppRestClient, final ListingClientsProperties properties) {
        this.restClient = cppRestClient;
        this.properties = properties;
    }

    /**
     * The tier, list type and key reason recorded against a hearing, or empty when the hearing
     * context has no record for it.
     */
    public Optional<JsonObject> getPtphDetail(final UUID hearingId) {
        return get(format(PTPH_DETAIL_PATH, hearingId), PTPH_DETAIL_MEDIA_TYPE);
    }

    /**
     * Issues a GET against the hearing query API and returns the response body.
     *
     * @param path      resource path, already URL-safe, relative to the configured base URI
     * @param mediaType the vendor media type to send as {@code Accept}
     */
    public Optional<JsonObject> get(final String path, final String mediaType) {
        final UUID userId = properties.systemUserId();
        if (userId == null) {
            throw new IllegalStateException("contextSystemUserId missing!!!");
        }
        try {
            return restClient.get()
                    .uri(URI.create(properties.hearingQueryBaseUrl() + path))
                    .header("Accept", mediaType)
                    .header(CJS_CPP_UID, userId.toString())
                    .exchange((request, response) -> {
                        final int status = response.getStatusCode().value();
                        final String body = new String(response.getBody().readAllBytes());
                        if (status == 404) {
                            LOGGER.info("Hearing query {} returned 404", path);
                            return Optional.<JsonObject>empty();
                        }
                        if (status != 200) {
                            throw new HearingQueryException(format("Hearing query %s failed with status %d", path, status));
                        }
                        try (JsonReader reader = Json.createReader(new StringReader(body))) {
                            return Optional.of(reader.readObject());
                        }
                    });
        } catch (final RestClientException ex) {
            throw new HearingQueryException(format("Hearing query %s failed", path), ex);
        }
    }
}
