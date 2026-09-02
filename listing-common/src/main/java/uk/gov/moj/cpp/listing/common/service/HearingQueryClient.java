package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads from the hearing context's query API over HTTP.
 *
 * <p>Deliberately hand-written rather than generated. Generating a remote client requires the
 * {@code hearing-query-api} RAML artifact on this module's build path, and the hearing context
 * already depends on {@code listing} — so that dependency closes a build cycle across the two
 * contexts. Calling the endpoint directly keeps the dependency one-way and at runtime only.
 *
 * <p>{@link #get(String, String)} is the reusable part: any further hearing query needs only a
 * thin method naming its path and media type, as {@link #getPtphDetail(UUID)} does. It follows the
 * pattern already used in this package for the court scheduler
 * ({@code ProvisionalBookingService}, {@code CourtSchedulerService}).
 */
@SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S6813"})
@ApplicationScoped
public class HearingQueryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryClient.class);

    private static final String PTPH_DETAIL_PATH = "/hearings/%s/ptph-detail";
    private static final String PTPH_DETAIL_MEDIA_TYPE = "application/vnd.hearing.get-ptph-detail+json";

    private static final String CJS_CPP_UID = "CJSCPPUID";

    @Inject
    @Value(key = "hearing.query.base.url", defaultValue = "http://localhost:8080/hearing-query-api/query/api/rest/hearing")
    protected String baseUri;

    @Inject
    SystemUserProvider systemUserProvider;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

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
     * <p>A 404 is {@code Optional.empty()} — the hearing context answering "no such thing" is a
     * legitimate outcome. Anything else non-2xx throws: a swallowed failure here would be
     * indistinguishable from a genuine absence, and callers use absence to mean "nothing to
     * inherit".
     *
     * @param path      resource path, already URL-safe, relative to the configured base URI
     * @param mediaType the vendor media type to send as {@code Accept}
     */
    public Optional<JsonObject> get(final String path, final String mediaType) {
        final HttpGet httpGet = new HttpGet(baseUri + path);
        httpGet.addHeader(ACCEPT, mediaType);
        httpGet.addHeader(CJS_CPP_UID, userId().toString());

        try (final CloseableHttpResponse response = HttpClientBuilder.create().build().execute(httpGet)) {
            final int status = response.getStatusLine().getStatusCode();

            if (status == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.info("Hearing query {} returned 404", path);
                consumeQuietly(response.getEntity());
                return Optional.empty();
            }
            if (status != Response.Status.OK.getStatusCode()) {
                consumeQuietly(response.getEntity());
                throw new HearingQueryException(format("Hearing query %s failed with status %d", path, status));
            }

            // Read the body inside the try so the connection is released either way. An
            // unconsumed entity keeps the connection leased and eventually exhausts the pool.
            return Optional.of(stringToJsonObjectConverter.convert(EntityUtils.toString(response.getEntity())));
        } catch (final IOException ex) {
            throw new HearingQueryException(format("Hearing query %s failed", path), ex);
        }
    }

    private static void consumeQuietly(final HttpEntity entity) {
        EntityUtils.consumeQuietly(entity);
    }

    private UUID userId() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
