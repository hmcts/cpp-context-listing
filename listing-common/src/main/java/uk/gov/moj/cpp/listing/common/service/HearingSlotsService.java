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
    private static final String HEARING_RESOURCE = "/hearingslots";
    private static final String COURTSCHEDULER_UPDATE_HEARING_SLOTS = "application/vnd.courtscheduler.update.hearing.slots+json";
    private static final String COURTSCHEDULER_GET_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.get.hearing.slots+json";
    private static final String COURTSCHEDULER_DELETE_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.remove.hearing.slots+json";
    private static final String COUTRT_SCHEDULER_HEARING_IDS = "application/vnd.courtscheduler.get.hearing.ids+json";

    private static final String CJS_CPP_UID = "CJSCPPUID";
    @Inject
    @Value(key = "courtscheduler.base.url", defaultValue = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler")
    private String baseUri;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    SystemUserProvider systemUserProvider;
    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    public Response search(final Map<String, String> params) {
        return query(HEARING_RESOURCE, COURTSCHEDULER_GET_HEARING_SLOTS_TYPE, params);
    }

    public void update(final Object payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Update HearingSlots slots in CourtScheduler S & L with slot details '{}'", payload);
        }

        try {
            final HttpPut httpPut = new HttpPut(new URL(baseUri + HEARING_RESOURCE).toString());
            httpPut.addHeader(CONTENT_TYPE, COURTSCHEDULER_UPDATE_HEARING_SLOTS);
            httpPut.addHeader(CJS_CPP_UID, getUserId().toString());

            final StringEntity requestEntity = new StringEntity(this.objectMapper.writeValueAsString(payload));
            httpPut.setEntity(requestEntity);

            final HttpResponse httpResponse = execute(httpPut);

            if (isOkay(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Update HearingSlots successfully");
                }
            } else {
                LOGGER.error(format("Update HearingSlots failed with statud code : %s andresponse message: %s",
                        httpResponse.getStatusLine().getStatusCode(),
                        EntityUtils.toString(httpResponse.getEntity())));
            }
        } catch (IOException ex) {
            LOGGER.error("Exception thrown on trying to Update Hearing Slots", ex);
        }
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

            if (isOkay(httpResponse)) {
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

    private boolean isOkay(HttpResponse httpResponse) {
        return httpResponse.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode();
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

            if (isOkay(httpResponse)) {
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
