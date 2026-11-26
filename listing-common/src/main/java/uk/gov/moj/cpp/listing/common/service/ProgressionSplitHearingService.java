package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forwards a converted split-hearing request to progression and hands back its response verbatim.
 *
 * <p>The framework's generated command client cannot be used here. Its
 * {@code DefaultRestClientProcessor.post} throws a bare {@code RuntimeException} for any status
 * other than 202, so progression's 400/404/409 all reach the front end as 500 and a stale request
 * is indistinguishable from progression being down. This mirrors
 * {@link HearingSlotsService#changeCourtRoomForMultidayHearing}, which calls courtscheduler the
 * same way for the same reason.
 *
 * <p>The hearing id fills the URI template and is not part of the body. Metadata travels in the
 * headers the framework client would have set, so progression's REST adapter builds an envelope
 * carrying the acting user rather than listing's system user.
 */
@SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S6813"})
@ApplicationScoped
public class ProgressionSplitHearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionSplitHearingService.class);

    public static final String PROGRESSION_SPLIT_HEARING_TYPE = "application/vnd.progression.split-hearing+json";

    private static final String SPLIT_RESOURCE = "/hearing/%s/split";
    private static final String CJS_CPP_UID = "CJSCPPUID";
    private static final String CPP_SID = "CPPSID";
    private static final String CPP_CLIENT_CORRELATION_ID = "CPPCLIENTCORRELATIONID";
    private static final String CPP_CAUSATION = "CPPCAUSATION";

    @Inject
    @Value(key = "progression.base.url", defaultValue = "http://localhost:8080/progression-command-api/command/api/rest/progression")
    protected String baseUri;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    public Response splitHearing(final String hearingId, final JsonObject payload, final Metadata metadata) {
        try {
            final HttpPost httpPost = new HttpPost(new URL(baseUri + format(SPLIT_RESOURCE, hearingId)).toString());
            httpPost.addHeader(CONTENT_TYPE, PROGRESSION_SPLIT_HEARING_TYPE);
            addIfPresent(httpPost, CJS_CPP_UID, metadata.userId());
            addIfPresent(httpPost, CPP_SID, metadata.sessionId());
            addIfPresent(httpPost, CPP_CLIENT_CORRELATION_ID, metadata.clientCorrelationId());
            if (!metadata.causation().isEmpty()) {
                httpPost.addHeader(CPP_CAUSATION, metadata.causation().stream()
                        .map(UUID::toString)
                        .collect(joining(",")));
            }
            httpPost.setEntity(new StringEntity(payload.toString(), UTF_8));

            final HttpResponse httpResponse = execute(httpPost);
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            final String body = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());

            LOGGER.info("split-hearing forwarded to progression for hearing {} returned status {}", hearingId, statusCode);

            return Response
                    .status(statusCode)
                    .entity(body.isBlank() ? null : stringToJsonObjectConverter.convert(body))
                    .build();
        } catch (IOException ex) {
            LOGGER.error("Exception thrown forwarding split-hearing to progression for hearing " + hearingId, ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(null)
                    .build();
        }
    }

    private static void addIfPresent(final HttpPost httpPost, final String header, final Optional<String> value) {
        value.ifPresent(present -> httpPost.addHeader(header, present));
    }

    private static CloseableHttpResponse execute(final HttpRequestBase httpRequest) throws IOException {
        return HttpClientBuilder
                .create()
                .build()
                .execute(httpRequest);
    }
}
