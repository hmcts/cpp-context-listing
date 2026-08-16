package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;

@SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S6813"})
@ApplicationScoped
public class HearingSlotsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingSlotsService.class);

    public static final String HEARING_DATE = "hearingDate";
    private static final String HEARING_ID = "hearingId";

    private static final String HEARING_RESOURCE = "/hearingslots";
    private static final String SESSIONS_RESOURCE = "/sessions";
    private static final String HEARINGS_RESOURCE = "/hearings";
    private static final String VALIDATE_SESSION_AVAILABILITY_RESOURCE = "/validate-session-availability";
    private static final String COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS_RESOURCE = HEARINGS_RESOURCE;
    private static final String COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS = "application/vnd.courtscheduler.list.hearings-in-sessions+json";
    private static final String COURTSCHEDULER_GET_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.get.hearing.slots+json";
    private static final String COURTSCHEDULER_SEARCH_COURTSCHEDULES_BY_ID = "application/vnd.courtscheduler.search.court-schedules-by-id+json";
    private static final String COURTSCHEDULER_DELETE_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.release.sessions+json";
    private static final String COUTRT_SCHEDULER_HEARING_IDS = "application/vnd.courtscheduler.get.hearing.ids+json";
    private static final String COURTSCHEDULER_MAGS_SEARCH_BOOK = "application/vnd.courtscheduler.mags.search.and.book+json";
    private static final String COURTSCHEDULER_CROWN_SEARCH_BOOK = "application/vnd.courtscheduler.crown.search.and.book+json";
    private static final String COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE = "application/vnd.courtscheduler.validate.session.availability+json";

    private static final String COURTSCHEDULER_MOVE_TO_PAST_DATE = "application/vnd.courtscheduler.move-hearing-to-past-date+json";

    public static final String COURTSCHEDULER_CHANGE_COURT_ROOM_MULTIDAY = "application/vnd.courtscheduler.change-court-room-for-multiday-hearing+json";

    private static final String CJS_CPP_UID = "CJSCPPUID";
    @Inject
    @Value(key = "courtscheduler.base.url", defaultValue = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler")
    protected String baseUri;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    SystemUserProvider systemUserProvider;
    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    public Response search(final Map<String, String> params) {
        return query(HEARING_RESOURCE, COURTSCHEDULER_GET_HEARING_SLOTS_TYPE, params);
    }

    public Response validateSessionAvailability(final JsonObject payload) {
        return post(VALIDATE_SESSION_AVAILABILITY_RESOURCE, COURTSCHEDULER_VALIDATE_SESSION_AVAILABILITY_TYPE, payload, true);
    }

    public Response searchBookSlots(final Map<String, String> params) {
        return postSearchBook(COURTSCHEDULER_MAGS_SEARCH_BOOK, params);
    }

    public Response listHearingInCourtSessions(final Object payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("HearingSlots slots list update in CourtScheduler S & L with slot details '{}'", payload);
        }

        try {
            final HttpPost httpPost = new HttpPost(new URIBuilder(baseUri + COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS_RESOURCE).build());
            httpPost.addHeader(CONTENT_TYPE, COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS);
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());

            final StringEntity requestEntity = new StringEntity(this.objectMapper.writeValueAsString(payload));
            httpPost.setEntity(requestEntity);

            final HttpResponse httpResponse = execute(httpPost);

            if (isOk(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("HearingSlots list updated successfully");
                }
                return Response
                        .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                        .entity(stringToJsonObjectConverter.convert(EntityUtils.toString(httpResponse.getEntity())))
                        .build();
            } else {
                final String entityBodyAsString = EntityUtils.toString(httpResponse.getEntity());
                LOGGER.error(format("HearingSlots list update failed with status code : %s and response message: %s",
                        httpResponse.getStatusLine().getStatusCode(), entityBodyAsString));
                return Response
                        .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                        .entity(entityBodyAsString)
                        .build();
            }
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to Update Hearing Slots", ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    public Response getCourtSchedulesById(final Map<String, String> params) {
        return query(SESSIONS_RESOURCE, COURTSCHEDULER_SEARCH_COURTSCHEDULES_BY_ID, params);
    }

    public Response multiDaySearchAndBook(final Map<String, String> params) {
        return postSearchBook(COURTSCHEDULER_CROWN_SEARCH_BOOK, params);
    }

    public Response crownFallbackSearchAndBook(final Map<String, String> params) {
        return postSearchBook(COURTSCHEDULER_CROWN_SEARCH_BOOK, params);
    }

    public Response moveHearingToPastDate(final UUID hearingId, final JsonObject payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("move-hearing-to-past-date for hearing id '{}'", hearingId);
        }

        try {
            final HttpPost httpPost = new HttpPost(new URL(baseUri + HEARINGS_RESOURCE + "/" + hearingId).toString());
            httpPost.addHeader(CONTENT_TYPE, COURTSCHEDULER_MOVE_TO_PAST_DATE);
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());

            final StringEntity requestEntity = new StringEntity(payload.toString());
            httpPost.setEntity(requestEntity);

            final HttpResponse httpResponse = execute(httpPost);
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            final String entityBodyAsString = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("move-hearing-to-past-date returned status {}", statusCode);
            }

            return Response
                    .status(statusCode)
                    .entity(entityBodyAsString.isBlank() ? null : stringToJsonObjectConverter.convert(entityBodyAsString))
                    .build();
        } catch (IOException ex) {
            LOGGER.error("Exception thrown on trying to move hearing to past date", ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    public Response changeCourtRoomForMultidayHearing(final UUID hearingId, final JsonObject payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("change-court-room-for-multiday-hearing for hearing id '{}'", hearingId);
        }

        try {
            final HttpPost httpPost = new HttpPost(new URL(baseUri + HEARINGS_RESOURCE + "/" + hearingId).toString());
            httpPost.addHeader(CONTENT_TYPE, COURTSCHEDULER_CHANGE_COURT_ROOM_MULTIDAY);
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());

            final StringEntity requestEntity = new StringEntity(payload.toString());
            httpPost.setEntity(requestEntity);

            final HttpResponse httpResponse = execute(httpPost);
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            final String entityBodyAsString = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("change-court-room-for-multiday-hearing returned status {}", statusCode);
            }

            return Response
                    .status(statusCode)
                    .entity(entityBodyAsString.isBlank() ? null : stringToJsonObjectConverter.convert(entityBodyAsString))
                    .build();
        } catch (IOException ex) {
            LOGGER.error("Exception thrown on trying to change court room for multiday hearing", ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    public void delete(final UUID hearingId) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Delete HearingSlots in CourtScheduler S & L with hearing id '{}'", hearingId);
        }

        try {
            final HttpDelete httpDelete = new HttpDelete(new URIBuilder(baseUri + SESSIONS_RESOURCE + "/" + hearingId).build());
            httpDelete.addHeader(CONTENT_TYPE, COURTSCHEDULER_DELETE_HEARING_SLOTS_TYPE);
            httpDelete.addHeader(CJS_CPP_UID, getUserId().toString());

            final HttpResponse httpResponse = execute(httpDelete);

            if (isAccepted(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Delete HearingSlots successfully");
                }
            } else {
                LOGGER.error("Delete HearingSlots failed with status code:{}",
                        httpResponse.getStatusLine().getStatusCode());
            }

        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to Delete Hearing Slots", ex);
        }
    }

    public Response getCourtSchedulerHearingIds(final Map<String, String> params) {
        return query(HEARING_RESOURCE, COUTRT_SCHEDULER_HEARING_IDS, params);
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }

    static boolean isOk(HttpResponse httpResponse) {
        return httpResponse.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode();
    }

    private boolean isAccepted(HttpResponse httpResponse) {
        return httpResponse.getStatusLine().getStatusCode() == Response.Status.ACCEPTED.getStatusCode();
    }

    private static CloseableHttpResponse execute(final HttpRequestBase httpRequest) throws IOException {
        return HttpClientBuilder
                .create()
                .build()
                .execute(httpRequest);
    }

    private Response query(final String urlPath, final String acceptHeader, final Map<String, String> params) {
        if (LOGGER.isInfoEnabled() && Objects.nonNull(params)) {
            params.forEach((key, value) -> LOGGER.info("{} in CourtScheduler S & L with params '{}-{}'", acceptHeader, key, value));
        }

        if (params == null) {
            throw new DataValidationException("Params for search %s is null ....".formatted(acceptHeader));
        }

        try {
            final HttpGet httpGet = new HttpGet(new URL(baseUri + urlPath).toString());
            httpGet.addHeader(ACCEPT, acceptHeader);
            httpGet.addHeader(CJS_CPP_UID, getUserId().toString());

            final URIBuilder uriBuilder = new URIBuilder(httpGet.getURI());
            params.forEach(uriBuilder::addParameter);
            httpGet.setURI(uriBuilder.build());

            final HttpResponse httpResponse = execute(httpGet);

            if (isOk(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Retrieve {} successfully", acceptHeader);
                }
                return Response
                        .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                        .entity(stringToJsonObjectConverter.convert(EntityUtils.toString(httpResponse.getEntity())))
                        .build();
            } else {
                LOGGER.error("Retrieve {} failed with status code:{}", acceptHeader,
                        httpResponse.getStatusLine().getStatusCode());
                return Response
                        .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                        .entity(EntityUtils.toString(httpResponse.getEntity()))
                        .build();
            }
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to Retrieving %s".formatted(acceptHeader), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    private Response post(final String urlPath, final String contentTypeHeader, final JsonObject payload, final boolean addAcceptJson) {
        if (LOGGER.isInfoEnabled() && Objects.nonNull(payload)) {
            LOGGER.info("{} in CourtScheduler S & L with payload '{}'", contentTypeHeader, payload);
        }
        if (payload == null || payload.isEmpty()) {
            throw new DataValidationException("Payload for %s is null or empty ....".formatted(contentTypeHeader));
        }
        try {
            final HttpPost httpPost = new HttpPost(new URIBuilder(baseUri + urlPath).build());
            httpPost.addHeader(CONTENT_TYPE, contentTypeHeader);
            if (addAcceptJson) {
                httpPost.addHeader(ACCEPT, "application/json");
            }
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());
            httpPost.setEntity(new StringEntity(payload.toString()));
            return executeAndBuildResponse(httpPost, contentTypeHeader, "POST");
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to Retrieving %s".formatted(contentTypeHeader), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }


    /**
     * Posts a search-and-book request to /hearings/{hearingId} with a typed JSON body.
     * Extracts "hearingId" from params map for the path; builds the remaining params as a JSON body,
     * converting numeric fields (durationInMinutes) to numbers and boolean fields (isPolice) to booleans.
     *
     * <p>hearingId travels ONLY in the {@code /hearings/{hearingId}} path — the courtscheduler
     * crown/mags search-and-book request schemas are {@code additionalProperties:false} and no longer
     * carry hearingId, so it MUST be excluded from the body (courtscheduler's REST adapter injects it
     * from the path). Leaving it in the body triggers a 400 schema-validation rejection. This mirrors
     * {@link #moveHearingToPastDate} and {@link CourtSchedulerServiceAdapter#moveHearingToPastDate}.</p>
     */
    private Response postSearchBook(final String contentTypeHeader, final Map<String, String> params) {
        if (params == null) {
            throw new DataValidationException("Params for %s is null ....".formatted(contentTypeHeader));
        }
        final String hearingId = params.get(HEARING_ID);
        if (hearingId == null || hearingId.isBlank()) {
            throw new DataValidationException("hearingId missing from params for %s".formatted(contentTypeHeader));
        }

        final Map<String, String> bodyParams = new HashMap<>(params);
        bodyParams.remove(HEARING_ID);
        final JsonObject payload = buildTypedJsonBody(bodyParams);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} POST /hearings/{} in CourtScheduler S & L with payload '{}'", contentTypeHeader, hearingId, payload);
        }

        try {
            final HttpPost httpPost = new HttpPost(new URIBuilder(baseUri + HEARINGS_RESOURCE + "/" + hearingId).build());
            httpPost.addHeader(CONTENT_TYPE, contentTypeHeader);
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());
            httpPost.setEntity(new StringEntity(payload.toString()));
            return executeAndBuildResponse(httpPost, contentTypeHeader, "POST");
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to POST %s".formatted(contentTypeHeader), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    /**
     * Builds a typed JSON body from a params map. Converts "durationInMinutes" values to JSON numbers
     * and "isPolice" values to JSON booleans; all other entries are added as strings.
     * Null values are silently omitted.
     */
    static JsonObject buildTypedJsonBody(final Map<String, String> params) {
        final javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
        params.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            if ("durationInMinutes".equals(key)) {
                try {
                    bodyBuilder.add(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
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

    /**
     * Executes an already-configured HTTP request and builds a JAX-RS Response from the outcome.
     * Logs success or failure at INFO/ERROR respectively. On IOException or URISyntaxException
     * the caller's catch block handles the 500 — this method only handles the execute + response
     * building path (no try/catch here).
     */
    private Response executeAndBuildResponse(final HttpRequestBase request, final String contentTypeHeader, final String method)
            throws IOException {
        final HttpResponse httpResponse = execute(request);
        final String responseBody = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());
        final Object entity = responseBody == null || responseBody.isBlank()
                ? Json.createObjectBuilder().build()
                : stringToJsonObjectConverter.convert(responseBody);

        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (isOk(httpResponse)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("{} {} successfully", method, contentTypeHeader);
            }
        } else {
            LOGGER.error("{} {} failed with status code:{}", method, contentTypeHeader, statusCode);
        }
        return Response
                .status(statusCode)
                .entity(entity)
                .build();
    }
}
