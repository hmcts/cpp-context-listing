package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;

import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Forwards a converted split-hearing request to progression and hands back its response verbatim
 * (MBD refactor P2.3.3: the JEE {@code ProgressionSplitHearingService} of listing-common, Apache
 * HttpClient → Spring {@link RestClient}). {@code POST <progression>/hearing/{hearingId}/split} with
 * Content-Type {@code application/vnd.progression.split-hearing+json}; the caller's metadata travels
 * in the {@code CJSCPPUID}, {@code CPPSID}, {@code CPPCLIENTCORRELATIONID} and {@code CPPCAUSATION}
 * headers (the acting user, not listing's system user). A JSON body is parsed (a blank body is no
 * entity); an I/O failure is a 500 with no entity, as before.
 */
@Component
public class ProgressionSplitHearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionSplitHearingService.class);

    public static final String PROGRESSION_SPLIT_HEARING_TYPE = "application/vnd.progression.split-hearing+json";

    private static final String SPLIT_RESOURCE = "/hearing/%s/split";
    private static final String CJS_CPP_UID = "CJSCPPUID";
    private static final String CPP_SID = "CPPSID";
    private static final String CPP_CLIENT_CORRELATION_ID = "CPPCLIENTCORRELATIONID";
    private static final String CPP_CAUSATION = "CPPCAUSATION";

    private final RestClient restClient;
    private final ListingClientsProperties properties;

    public ProgressionSplitHearingService(final RestClient cppRestClient, final ListingClientsProperties properties) {
        this.restClient = cppRestClient;
        this.properties = properties;
    }

    public HttpResult splitHearing(final String hearingId, final JsonObject payload, final Optional<String> userId,
                                   final Optional<String> sessionId, final Optional<String> clientCorrelationId,
                                   final List<UUID> causation) {
        try {
            final RestClient.RequestBodySpec request = restClient.post()
                    .uri(URI.create(properties.progressionCommandBaseUrl() + format(SPLIT_RESOURCE, hearingId)))
                    .contentType(MediaType.parseMediaType(PROGRESSION_SPLIT_HEARING_TYPE));
            userId.ifPresent(value -> request.header(CJS_CPP_UID, value));
            sessionId.ifPresent(value -> request.header(CPP_SID, value));
            clientCorrelationId.ifPresent(value -> request.header(CPP_CLIENT_CORRELATION_ID, value));
            if (!causation.isEmpty()) {
                request.header(CPP_CAUSATION, causation.stream().map(UUID::toString).collect(joining(",")));
            }
            return request.body(payload.toString())
                    .exchange((req, response) -> {
                        final int statusCode = response.getStatusCode().value();
                        final String body = new String(response.getBody().readAllBytes());
                        LOGGER.info("split-hearing forwarded to progression for hearing {} returned status {}", hearingId, statusCode);
                        return new HttpResult(statusCode, body.isBlank() ? null : parse(body));
                    });
        } catch (final RestClientException ex) {
            LOGGER.error("Exception thrown forwarding split-hearing to progression for hearing " + hearingId, ex);
            return new HttpResult(500, null);
        }
    }

    private static JsonObject parse(final String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            return reader.readObject();
        }
    }
}
