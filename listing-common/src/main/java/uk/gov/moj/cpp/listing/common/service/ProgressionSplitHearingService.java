package uk.gov.moj.cpp.listing.common.service;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forwards a converted split-hearing request to progression, which owns the split decision
 * (ADR-031 Option 4 refined). Listing enriches nothing, touches no aggregate and emits no events on
 * this path, so progression's status is returned to the caller unchanged and a rejected split
 * surfaces synchronously in the UI.
 *
 * <p>Hand-written, as every other outbound client in this context is (see
 * {@link HearingSlotsService}). The request body is the already-published
 * {@code progression.list-new-hearing.json} shape — {@code {listNewHearing, sendNotificationToParties}}
 * — which the split endpoint reuses unchanged, so there is no contract left to pin down.
 */
@ApplicationScoped
public class ProgressionSplitHearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionSplitHearingService.class);

    private static final String SPLIT_HEARING_MEDIA_TYPE = "application/vnd.progression.split-hearing+json";
    private static final String CJS_CPP_UID = "CJSCPPUID";

    @Inject
    @Value(key = "progression.command.base.url",
            defaultValue = "http://localhost:8080/progression-command-api/command/api/rest/progression")
    protected String baseUri;

    @Inject
    SystemUserProvider systemUserProvider;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    public Response split(final String hearingId, final JsonObject splitRequest) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Forwarding split-hearing for hearing {} to progression", hearingId);
        }
        try {
            final HttpPost httpPost = new HttpPost(
                    new URIBuilder("%s/hearing/%s/split".formatted(baseUri, hearingId)).build());
            httpPost.addHeader(CONTENT_TYPE, SPLIT_HEARING_MEDIA_TYPE);
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());
            httpPost.setEntity(new StringEntity(splitRequest.toString()));

            final HttpResponse httpResponse = HttpClientBuilder.create().build().execute(httpPost);
            final String responseBody = httpResponse.getEntity() == null
                    ? ""
                    : EntityUtils.toString(httpResponse.getEntity());
            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
                LOGGER.error("progression split-hearing for hearing {} failed with status {}", hearingId, statusCode);
            }

            return Response
                    .status(statusCode)
                    .entity(responseBody == null || responseBody.isBlank()
                            ? Json.createObjectBuilder().build()
                            : stringToJsonObjectConverter.convert(responseBody))
                    .build();
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception forwarding split-hearing for hearing %s".formatted(hearingId), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
