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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
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
    private static final String COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS_RESOURCE = "/list/hearingslots";
    private static final String HEARING_SEARCH_BOOK_RESOURCE = "/searchlist/hearingslots";
    private static final String COURTSCHEDULES_RESOURCE = "/courtschedule/search.court-schedules-by-id";
    private static final String COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS = "application/vnd.courtscheduler.list.hearings-in-court-sessions+json";
    private static final String COURTSCHEDULER_GET_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.get.hearing.slots+json";
    private static final String COURTSCHEDULER_SEARCH_COURTSCHEDULES_BY_ID = "application/vnd.courtscheduler.search.courtschedules.by.id+json";
    private static final String COURTSCHEDULER_DELETE_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.remove.hearing.slots+json";
    private static final String COUTRT_SCHEDULER_HEARING_IDS = "application/vnd.courtscheduler.get.hearing.ids+json";
    private static final String COURTSCHEDULER_SEARCH_BOOK_COURTSCHEDULES = "application/vnd.courtscheduler.search.book.hearing.slots+json";

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

    public Response searchBookSlots(final Map<String, String> params) {
        return query(HEARING_SEARCH_BOOK_RESOURCE, COURTSCHEDULER_SEARCH_BOOK_COURTSCHEDULES, params);
    }

    public Response listHearingInCourtSessions(final Object payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("HearingSlots slots list update in CourtScheduler S & L with slot details '{}'", payload);
        }

        try {
            final HttpPut httpPut = new HttpPut(new URL(baseUri + COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS_RESOURCE).toString());
            httpPut.addHeader(CONTENT_TYPE, COURTSCHEDULER_LIST_HEARING_IN_COURT_SESSIONS);
            httpPut.addHeader(CJS_CPP_UID, getUserId().toString());

            final StringEntity requestEntity = new StringEntity(this.objectMapper.writeValueAsString(payload));
            httpPut.setEntity(requestEntity);

            final HttpResponse httpResponse = execute(httpPut);

            if (isOk(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("HearingSlots list updated successfully");
                }
                return Response
                        .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                        .entity(stringToJsonObjectConverter.convert(EntityUtils.toString(httpResponse.getEntity())))
                        .build();
            } else {
                LOGGER.error(format("HearingSlots list update failed with status code : %s and response message: %s",
                        httpResponse.getStatusLine().getStatusCode(),
                        EntityUtils.toString(httpResponse.getEntity())));
                return Response
                        .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                        .entity(EntityUtils.toString(httpResponse.getEntity()))
                        .build();
            }
        } catch (IOException ex) {
            LOGGER.error("Exception thrown on trying to Update Hearing Slots", ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    public Response getCourtSchedulesById(final Map<String, String> params) {
        return query(COURTSCHEDULES_RESOURCE, COURTSCHEDULER_SEARCH_COURTSCHEDULES_BY_ID, params);
    }

    public void delete(final UUID hearingId) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Delete HearingSlots in CourtScheduler S & L with hearing id '{}'", hearingId);
        }

        try {
            final HttpDelete httpDelete = new HttpDelete(new URL(baseUri + HEARING_RESOURCE + "/" + hearingId.toString()).toString());
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

        } catch (IOException ex) {
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
}
