package uk.gov.moj.cpp.listing.common.client;

import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Replaces the framework {@code Requester} for remote queries (§6, modelled on courtscheduler's
 * {@code CommonPlatformQueryClient}): a GET on the endpoint the framework's generated
 * {@code Remote*} client used for the action name, with
 * <ul>
 *   <li>{@code Accept: application/vnd.<action name>+json},</li>
 *   <li>path parameters and the declared query parameters taken from the request payload,</li>
 *   <li>{@code CJSCPPUID}: the caller's user id for {@link #request}, the configured system user for
 *       {@link #requestAsAdmin} (the framework's {@code requestAsAdmin}).</li>
 * </ul>
 * Status handling is the framework's {@code DefaultRestClientProcessor}: 200 → the JSON body,
 * 404 → empty (the framework returned a {@code JsonValue.NULL} payload), 400 →
 * {@link RemoteBadRequestException}, 403 → {@link RemoteAccessDeniedException}, anything else →
 * {@link IllegalStateException}.
 */
@Component
public class CppQueryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CppQueryClient.class);
    private static final Pattern PATH_PARAM = Pattern.compile("\\{([^}]+)}");
    static final String CJSCPPUID = "CJSCPPUID";
    private static final JsonObjectToObjectConverter CONVERTER = new JsonObjectToObjectConverter();

    private final RestClient restClient;
    private final ListingClientsProperties properties;

    public CppQueryClient(final RestClient cppRestClient, final ListingClientsProperties properties) {
        this.restClient = cppRestClient;
        this.properties = properties;
    }

    /** As the framework's {@code Requester#request}: CJSCPPUID is the calling user. */
    public Optional<JsonObject> request(final String actionName, final JsonObject params, final UUID userId) {
        return get(actionName, params, userId);
    }

    /** As the framework's {@code Requester#requestAsAdmin}: CJSCPPUID is the configured system user. */
    public Optional<JsonObject> requestAsAdmin(final String actionName, final JsonObject params) {
        return get(actionName, params, properties.requiredSystemUserId());
    }

    /** {@link #requestAsAdmin(String, JsonObject)} with the payload converted (framework object mapper) to {@code type}. */
    public <T> Optional<T> requestAsAdmin(final String actionName, final JsonObject params, final Class<T> type) {
        return requestAsAdmin(actionName, params).map(json -> CONVERTER.convert(json, type));
    }

    /** {@link #request(String, JsonObject, UUID)} with the payload converted (framework object mapper) to {@code type}. */
    public <T> Optional<T> request(final String actionName, final JsonObject params, final UUID userId, final Class<T> type) {
        return request(actionName, params, userId).map(json -> CONVERTER.convert(json, type));
    }

    private Optional<JsonObject> get(final String actionName, final JsonObject params, final UUID userId) {
        final CppEndpoint endpoint = CppEndpoint.ENDPOINTS.get(actionName);
        if (endpoint == null) {
            throw new IllegalArgumentException("No remote endpoint registered for " + actionName);
        }
        final URI uri = uri(endpoint, params == null ? JsonValue.EMPTY_JSON_OBJECT : params);
        final String accept = "application/vnd." + actionName + "+json";
        LOGGER.debug("Remote query {} -> GET {}", actionName, uri);

        return restClient.get()
                .uri(uri)
                .accept(MediaType.parseMediaType(accept))
                .header(CJSCPPUID, userId == null ? null : userId.toString())
                .exchange((request, response) -> {
                    final HttpStatusCode status = response.getStatusCode();
                    final String body = new String(response.getBody().readAllBytes());
                    return switch (status.value()) {
                        case 200 -> Optional.of(parse(body));
                        case 404 -> Optional.<JsonObject>empty();
                        case 400 -> throw new RemoteBadRequestException(body);
                        case 403 -> throw new RemoteAccessDeniedException(body);
                        default -> throw new IllegalStateException(String.format(
                                "Request %s failed; expected 200 but got %s", actionName, status.value()));
                    };
                });
    }

    private URI uri(final CppEndpoint endpoint, final JsonObject params) {
        final Map<String, String> pathValues = new HashMap<>();
        final Matcher matcher = PATH_PARAM.matcher(endpoint.pathTemplate());
        while (matcher.find()) {
            final String name = matcher.group(1);
            pathValues.put(name, value(params, name));
        }
        final UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.baseUrl() + endpoint.contextRoot())
                .path(endpoint.pathTemplate());
        endpoint.queryParams().stream()
                .filter(name -> params.containsKey(name) && !params.isNull(name))
                .sorted()
                .forEach(name -> builder.queryParam(name, value(params, name)));
        return builder.buildAndExpand(pathValues).encode().toUri();
    }

    private static String value(final JsonObject params, final String name) {
        final JsonValue value = params.get(name);
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            throw new IllegalArgumentException("Missing parameter " + name);
        }
        return value instanceof JsonString string ? string.getString() : value.toString();
    }

    static JsonObject parse(final String body) {
        if (body == null || body.isBlank()) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            return reader.readObject();
        }
    }
}
