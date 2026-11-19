package uk.gov.moj.cpp.listing.common.service;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;

import java.io.StringReader;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Calls to listingcourtscheduler (P2.1: the query-side calls only; the command-side ones —
 * search-and-book, list hearings in sessions, move to past date, change court room — come with
 * the commands in P2.3). Same URLs, media types and headers as the JEE {@code HearingSlotsService}
 * (Apache HttpClient), now on Spring {@link RestClient}; CJSCPPUID is the context system user.
 * Non-200 answers are returned, not thrown, as before: a JSON body is parsed for 200 (GET) and for
 * every status (POST), otherwise the raw body string is kept.
 */
@Component
public class HearingSlotsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingSlotsService.class);

    private static final String HEARING_RESOURCE = "/hearingslots";
    private static final String SESSIONS_RESOURCE = "/sessions";
    private static final String VALIDATE_SESSION_AVAILABILITY_RESOURCE = "/validate-session-availability";
    private static final String COURTSCHEDULER_GET_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.get.hearing.slots+json";
    private static final String COURTSCHEDULER_SEARCH_COURTSCHEDULES_BY_ID = "application/vnd.courtscheduler.search.court-schedules-by-id+json";
    private static final String COURTSCHEDULER_HEARING_IDS = "application/vnd.courtscheduler.get.hearing.ids+json";
    private static final String COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE = "application/vnd.courtscheduler.validate.session.availability+json";
    private static final String CJS_CPP_UID = "CJSCPPUID";
    private static final String CPP_ACTION = "CPP-ACTION";

    private final RestClient restClient;
    private final ListingClientsProperties properties;

    public HearingSlotsService(final RestClient cppRestClient, final ListingClientsProperties properties) {
        this.restClient = cppRestClient;
        this.properties = properties;
    }

    public HttpResult search(final Map<String, String> params) {
        return query(HEARING_RESOURCE, COURTSCHEDULER_GET_HEARING_SLOTS_TYPE, params);
    }

    public HttpResult getCourtSchedulesById(final Map<String, String> params) {
        return query(SESSIONS_RESOURCE, COURTSCHEDULER_SEARCH_COURTSCHEDULES_BY_ID, params);
    }

    public HttpResult getCourtSchedulerHearingIds(final Map<String, String> params) {
        return query(HEARING_RESOURCE, COURTSCHEDULER_HEARING_IDS, params);
    }

    public HttpResult validateSessionAvailability(final JsonObject payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Payload for %s is null or empty ....".formatted(COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE));
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} in CourtScheduler S & L with payload '{}'", COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE, payload);
        }
        try {
            return restClient.post()
                    .uri(URI.create(properties.courtschedulerBaseUrl() + VALIDATE_SESSION_AVAILABILITY_RESOURCE))
                    .contentType(MediaType.parseMediaType(COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE))
                    .accept(MediaType.APPLICATION_JSON)
                    .header(CJS_CPP_UID, properties.requiredSystemUserId().toString())
                    .body(payload.toString())
                    .exchange((request, response) -> {
                        final String body = new String(response.getBody().readAllBytes());
                        final int status = response.getStatusCode().value();
                        if (status != 200) {
                            LOGGER.error("POST {} failed with status code:{}", COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE, status);
                        }
                        return new HttpResult(status, body.isBlank() ? Json.createObjectBuilder().build() : parse(body));
                    });
        } catch (final RestClientException ex) {
            LOGGER.error("Exception thrown on trying to Retrieving %s".formatted(COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE), ex);
            return new HttpResult(500, ex.getMessage());
        }
    }

    private HttpResult query(final String urlPath, final String acceptHeader, final Map<String, String> params) {
        if (LOGGER.isInfoEnabled() && Objects.nonNull(params)) {
            params.forEach((key, value) -> LOGGER.info("{} in CourtScheduler S & L with params '{}-{}'", acceptHeader, key, value));
        }
        if (params == null) {
            throw new IllegalArgumentException("Params for search %s is null ....".formatted(acceptHeader));
        }
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.courtschedulerBaseUrl() + urlPath);
            params.forEach(builder::queryParam);
            return restClient.get()
                    .uri(builder.encode().build().toUri())
                    .accept(MediaType.parseMediaType(acceptHeader))
                    .header(CJS_CPP_UID, properties.requiredSystemUserId().toString())
                    .header(CPP_ACTION, toActionName(acceptHeader))
                    .exchange((request, response) -> {
                        final String body = new String(response.getBody().readAllBytes());
                        final int status = response.getStatusCode().value();
                        if (status == 200) {
                            LOGGER.info("Retrieve {} successfully", acceptHeader);
                            return new HttpResult(status, parse(body));
                        }
                        LOGGER.error("Retrieve {} failed with status code:{}", acceptHeader, status);
                        return new HttpResult(status, body);
                    });
        } catch (final RestClientException | JsonException ex) {
            LOGGER.error("Exception thrown on trying to Retrieving %s".formatted(acceptHeader), ex);
            return new HttpResult(500, ex.getMessage());
        }
    }

    private static String toActionName(final String mediaType) {
        return mediaType.replaceFirst("^application/vnd\\.", "").replaceFirst("\\+json$", "");
    }

    private static JsonObject parse(final String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            return reader.readObject();
        }
    }
}
