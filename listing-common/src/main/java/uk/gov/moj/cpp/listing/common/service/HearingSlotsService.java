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
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S6813"})
@ApplicationScoped
public class HearingSlotsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingSlotsService.class);

    public static final String HEARING_DATE = "hearingDate";

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

    private static final String COURTSCHEDULER_EXTEND_MULTIDAY = "application/vnd.courtscheduler.extend.multiday.hearing+json";

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

    public Response extendMultiDayHearing(final JsonObject payload) {
        final String hearingId = payload.getString("hearingId");
        return patch(HEARINGS_RESOURCE + "/" + hearingId, COURTSCHEDULER_EXTEND_MULTIDAY, payload);
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
            final URIBuilder uriBuilder = new URIBuilder(baseUri + urlPath);
            params.forEach(uriBuilder::addParameter);
            final HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.addHeader(ACCEPT, acceptHeader);
            httpGet.addHeader(CJS_CPP_UID, getUserId().toString());

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

            final HttpResponse httpResponse = execute(httpPost);
            final String responseBody = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());
            final Object entity = responseBody == null || responseBody.isBlank()
                    ? Json.createObjectBuilder().build()
                    : stringToJsonObjectConverter.convert(responseBody);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (isOk(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Retrieve {} successfully", contentTypeHeader);
                }
            } else {
                LOGGER.error("Retrieve {} failed with status code:{}", contentTypeHeader, statusCode);
            }
            return Response
                    .status(statusCode)
                    .entity(entity)
                    .build();
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to Retrieving %s".formatted(contentTypeHeader), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    private Response patch(final String urlPath, final String contentTypeHeader, final JsonObject payload) {
        if (LOGGER.isInfoEnabled() && Objects.nonNull(payload)) {
            LOGGER.info("{} PATCH in CourtScheduler S & L with payload '{}'", contentTypeHeader, payload);
        }
        if (payload == null || payload.isEmpty()) {
            throw new DataValidationException("Payload for PATCH %s is null or empty ....".formatted(contentTypeHeader));
        }
        try {
            final HttpPatch httpPatch = new HttpPatch(new URIBuilder(baseUri + urlPath).build());
            httpPatch.addHeader(CONTENT_TYPE, contentTypeHeader);
            httpPatch.addHeader(CJS_CPP_UID, getUserId().toString());
            httpPatch.setEntity(new StringEntity(payload.toString()));

            final HttpResponse httpResponse = execute(httpPatch);
            final String responseBody = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());
            final Object entity = responseBody == null || responseBody.isBlank()
                    ? Json.createObjectBuilder().build()
                    : stringToJsonObjectConverter.convert(responseBody);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (isOk(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("PATCH {} successfully", contentTypeHeader);
                }
            } else {
                LOGGER.error("PATCH {} failed with status code:{}", contentTypeHeader, statusCode);
            }
            return Response
                    .status(statusCode)
                    .entity(entity)
                    .build();
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to PATCH %s".formatted(contentTypeHeader), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    /**
     * Posts a search-and-book request to /hearings/{hearingId} with a typed JSON body.
     * Extracts "hearingId" from params map for the path; builds remaining params as a JSON body,
     * converting numeric fields (durationInMinutes) to numbers and boolean fields (isPolice) to booleans.
     */
    private Response postSearchBook(final String contentTypeHeader, final Map<String, String> params) {
        if (params == null) {
            throw new DataValidationException("Params for %s is null ....".formatted(contentTypeHeader));
        }
        final String hearingId = params.get("hearingId");
        if (hearingId == null || hearingId.isBlank()) {
            throw new DataValidationException("hearingId missing from params for %s".formatted(contentTypeHeader));
        }

        // Build typed JSON body from params (hearingId always included)
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
        final JsonObject payload = bodyBuilder.build();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} POST /hearings/{} in CourtScheduler S & L with payload '{}'", contentTypeHeader, hearingId, payload);
        }

        try {
            final HttpPost httpPost = new HttpPost(new URIBuilder(baseUri + HEARINGS_RESOURCE + "/" + hearingId).build());
            httpPost.addHeader(CONTENT_TYPE, contentTypeHeader);
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());
            httpPost.setEntity(new StringEntity(payload.toString()));

            final HttpResponse httpResponse = execute(httpPost);
            final String responseBody = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());
            final Object entity = responseBody == null || responseBody.isBlank()
                    ? Json.createObjectBuilder().build()
                    : stringToJsonObjectConverter.convert(responseBody);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (isOk(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("POST {} successfully", contentTypeHeader);
                }
            } else {
                LOGGER.error("POST {} failed with status code:{}", contentTypeHeader, statusCode);
            }
            return Response
                    .status(statusCode)
                    .entity(entity)
                    .build();
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to POST %s".formatted(contentTypeHeader), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }
}
