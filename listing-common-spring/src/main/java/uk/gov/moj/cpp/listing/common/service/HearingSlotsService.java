package uk.gov.moj.cpp.listing.common.service;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;

import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Calls to listingcourtscheduler (P2.1: the query-side calls; P2.3.2: the command-side ones —
 * search-and-book, list hearings in sessions, move to past date, change court room, release
 * sessions). Same URLs, media types and headers as the JEE {@code HearingSlotsService}
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
    private static final String HEARINGS_RESOURCE = "/hearings";
    private static final String COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS = "application/vnd.courtscheduler.list.hearings-in-sessions+json";
    private static final String COURTSCHEDULER_DELETE_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.release.sessions+json";
    private static final String COURTSCHEDULER_MAGS_SEARCH_BOOK = "application/vnd.courtscheduler.mags.search.and.book+json";
    private static final String COURTSCHEDULER_CROWN_SEARCH_BOOK = "application/vnd.courtscheduler.crown.search.and.book+json";
    private static final String COURTSCHEDULER_MOVE_TO_PAST_DATE = "application/vnd.courtscheduler.move-hearing-to-past-date+json";
    public static final String COURTSCHEDULER_CHANGE_COURT_ROOM_MULTIDAY = "application/vnd.courtscheduler.change-court-room-for-multiday-hearing+json";
    public static final String HEARING_DATE = "hearingDate";
    private static final String HEARING_ID = "hearingId";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
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

    public HttpResult searchBookSlots(final Map<String, String> params) {
        return postSearchBook(COURTSCHEDULER_MAGS_SEARCH_BOOK, params);
    }

    public HttpResult multiDaySearchAndBook(final Map<String, String> params) {
        return postSearchBook(COURTSCHEDULER_CROWN_SEARCH_BOOK, params);
    }

    public HttpResult crownFallbackSearchAndBook(final Map<String, String> params) {
        return postSearchBook(COURTSCHEDULER_CROWN_SEARCH_BOOK, params);
    }

    /**
     * POST /hearings (list hearings in sessions): the payload POJO as framework JSON. A 200 body is
     * parsed; any other status keeps the raw body, as before.
     */
    public HttpResult listHearingInCourtSessions(final Object payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("HearingSlots slots list update in CourtScheduler S & L with slot details '{}'", payload);
        }
        try {
            final String body = OBJECT_MAPPER.writeValueAsString(payload);
            return restClient.post()
                    .uri(URI.create(properties.courtschedulerBaseUrl() + HEARINGS_RESOURCE))
                    .contentType(MediaType.parseMediaType(COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS))
                    .header(CJS_CPP_UID, properties.requiredSystemUserId().toString())
                    .body(body)
                    .exchange((request, response) -> {
                        final String responseBody = new String(response.getBody().readAllBytes());
                        final int status = response.getStatusCode().value();
                        if (status == 200) {
                            LOGGER.info("HearingSlots list updated successfully");
                            return new HttpResult(status, parse(responseBody));
                        }
                        LOGGER.error("HearingSlots list update failed with status code : {} and response message: {}", status, responseBody);
                        return new HttpResult(status, responseBody);
                    });
        } catch (final RestClientException | JsonProcessingException | JsonException ex) {
            LOGGER.error("Exception thrown on trying to Update Hearing Slots", ex);
            return new HttpResult(500, ex.getMessage());
        }
    }

    /** POST /hearings/{hearingId} (move-hearing-to-past-date): the body parsed when not blank, else no entity. */
    public HttpResult moveHearingToPastDate(final UUID hearingId, final JsonObject payload) {
        LOGGER.info("move-hearing-to-past-date for hearing id '{}'", hearingId);
        return postToHearing(hearingId, COURTSCHEDULER_MOVE_TO_PAST_DATE, payload, "move-hearing-to-past-date");
    }

    /** POST /hearings/{hearingId} (change-court-room-for-multiday-hearing). */
    public HttpResult changeCourtRoomForMultidayHearing(final UUID hearingId, final JsonObject payload) {
        LOGGER.info("change-court-room-for-multiday-hearing for hearing id '{}'", hearingId);
        return postToHearing(hearingId, COURTSCHEDULER_CHANGE_COURT_ROOM_MULTIDAY, payload, "change-court-room-for-multiday-hearing");
    }

    /**
     * DELETE /sessions/{hearingId} (release the hearing's sessions). Failures (not 202, or an
     * exception) are logged and swallowed, as before.
     */
    public void delete(final UUID hearingId) {
        LOGGER.info("Delete HearingSlots in CourtScheduler S & L with hearing id '{}'", hearingId);
        try {
            final int status = restClient.delete()
                    .uri(URI.create(properties.courtschedulerBaseUrl() + SESSIONS_RESOURCE + "/" + hearingId))
                    .header("Content-Type", COURTSCHEDULER_DELETE_HEARING_SLOTS_TYPE)
                    .header(CJS_CPP_UID, properties.requiredSystemUserId().toString())
                    .exchange((request, response) -> response.getStatusCode().value());
            if (status == 202) {
                LOGGER.info("Delete HearingSlots successfully");
            } else {
                LOGGER.error("Delete HearingSlots failed with status code:{}", status);
            }
        } catch (final RestClientException ex) {
            LOGGER.error("Exception thrown on trying to Delete Hearing Slots", ex);
        }
    }

    private HttpResult postToHearing(final UUID hearingId, final String contentType, final JsonObject payload, final String operation) {
        try {
            return restClient.post()
                    .uri(URI.create(properties.courtschedulerBaseUrl() + HEARINGS_RESOURCE + "/" + hearingId))
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(CJS_CPP_UID, properties.requiredSystemUserId().toString())
                    .body(payload.toString())
                    .exchange((request, response) -> {
                        final String body = new String(response.getBody().readAllBytes());
                        final int status = response.getStatusCode().value();
                        LOGGER.info("{} returned status {}", operation, status);
                        return new HttpResult(status, body.isBlank() ? null : parse(body));
                    });
        } catch (final RestClientException | JsonException ex) {
            LOGGER.error("Exception thrown on trying to " + operation, ex);
            return new HttpResult(500, ex.getMessage());
        }
    }

    /**
     * POST /hearings/{hearingId} with the params (minus hearingId, which travels in the path) as a
     * typed JSON body (durationInMinutes as a number, isPolice as a boolean).
     */
    private HttpResult postSearchBook(final String contentTypeHeader, final Map<String, String> params) {
        if (params == null) {
            throw new IllegalArgumentException("Params for %s is null ....".formatted(contentTypeHeader));
        }
        final String hearingId = params.get(HEARING_ID);
        if (hearingId == null || hearingId.isBlank()) {
            throw new IllegalArgumentException("hearingId missing from params for %s".formatted(contentTypeHeader));
        }
        final Map<String, String> bodyParams = new HashMap<>(params);
        bodyParams.remove(HEARING_ID);
        final JsonObject payload = buildTypedJsonBody(bodyParams);
        LOGGER.info("{} POST /hearings/{} in CourtScheduler S & L with payload '{}'", contentTypeHeader, hearingId, payload);
        try {
            return restClient.post()
                    .uri(URI.create(properties.courtschedulerBaseUrl() + HEARINGS_RESOURCE + "/" + hearingId))
                    .contentType(MediaType.parseMediaType(contentTypeHeader))
                    .header(CJS_CPP_UID, properties.requiredSystemUserId().toString())
                    .body(payload.toString())
                    .exchange((request, response) -> {
                        final String body = new String(response.getBody().readAllBytes());
                        final int status = response.getStatusCode().value();
                        if (status == 200) {
                            LOGGER.info("POST {} successfully", contentTypeHeader);
                        } else {
                            LOGGER.error("POST {} failed with status code:{}", contentTypeHeader, status);
                        }
                        return new HttpResult(status, body.isBlank() ? Json.createObjectBuilder().build() : parse(body));
                    });
        } catch (final RestClientException | JsonException ex) {
            LOGGER.error("Exception thrown on trying to POST %s".formatted(contentTypeHeader), ex);
            return new HttpResult(500, ex.getMessage());
        }
    }

    static JsonObject buildTypedJsonBody(final Map<String, String> params) {
        final JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
        params.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            if ("durationInMinutes".equals(key)) {
                try {
                    bodyBuilder.add(key, Integer.parseInt(value));
                } catch (final NumberFormatException e) {
                    bodyBuilder.add(key, value);
                }
            } else if ("isPolice".equals(key)) {
                bodyBuilder.add(key, Boolean.parseBoolean(value));
            } else {
                bodyBuilder.add(key, value);
            }
        });
        return bodyBuilder.build();
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
