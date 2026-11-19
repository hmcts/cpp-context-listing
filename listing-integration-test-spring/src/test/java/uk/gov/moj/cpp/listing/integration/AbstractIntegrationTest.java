package uk.gov.moj.cpp.listing.integration;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.web.client.RestTemplate;

/**
 * Shared helpers for HTTP integration tests against the dockerised Spring Boot app. Ported from
 * courtscheduler's {@code AbstractIntegrationTest}.
 *
 * <p>System properties supplied by the Gradle {@code test} task:</p>
 * <ul>
 *   <li>{@code app.baseUrl} – {@code http://localhost:8084/listing-service} (servlet context path, ADR 008)</li>
 *   <li>{@code wiremock.baseUrl} – {@code http://localhost:8191}</li>
 * </ul>
 */
public abstract class AbstractIntegrationTest {

    public static final String BASE_URL = System.getProperty("app.baseUrl", "http://localhost:8084/listing-service");
    public static final String WIREMOCK_BASE_URL = System.getProperty("wiremock.baseUrl", "http://localhost:8191");

    /** The two legacy context roots, both served under {@link #BASE_URL}. */
    public static final String COMMAND_API = "/command/api/rest/listing";
    public static final String QUERY_API = "/query/api/rest/listing";

    /** Group "Listing Officers" — {@code wiremock/mappings/identity-listing-officer-user.json}. */
    public static final UUID LISTING_OFFICER_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** Group "System Users" — {@code wiremock/mappings/identity-system-user.json}. */
    public static final UUID SYSTEM_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    /** No groups — {@code wiremock/mappings/identity-denied-user.json}; denied everywhere. */
    public static final UUID DENIED_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    /** Group "Defence Lawyers" — {@code wiremock/mappings/identity-defence-lawyer-user.json}. */
    public static final UUID DEFENCE_LAWYER_USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    public static final String SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    public static final String CREATE_LISTING_NOTE = "application/vnd.listing.command.create-listing-note+json";

    /** A hearing of the P2.1 viewstore fixture (fixtures/viewstore.sql), served by the real query. */
    public static final String HEARING_ID = "2f6f2b0a-0b0c-4f43-9d1c-5f4e7f6d8a01";
    public static final String HEARING_PATH = QUERY_API + "/hearings/" + HEARING_ID;
    /** The create-listing-note command (P2.3: the real endpoint; it writes a note and publishes public.listing.created-listing-note). */
    public static final String LISTING_NOTE_PATH = COMMAND_API + "/listing-note";
    public static final String VALID_LISTING_NOTE =
            "{\"hearingDate\":\"2020-01-09\",\"courtRoomId\":\"a299cf49-d14a-496f-895f-59c46859b4a9\",\"noteDescription\":\"note\"}";

    protected final RestTemplate rest = newRestTemplate();

    /** The viewstore fixture, seeded once per JVM (P2.1: the query endpoints read real data). */
    @BeforeAll
    static void seedViewstore() {
        ViewstoreFixtures.seedOnce();
    }

    private static RestTemplate newRestTemplate() {
        final RestTemplate template = new RestTemplate();
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(final ClientHttpResponse response) {
                // Surface 4xx/5xx as ResponseEntity rather than throwing, so tests can assert on them.
                return false;
            }
        });
        return template;
    }

    protected ResponseEntity<String> get(final String path, final UUID userId, final String acceptMediaType) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, acceptMediaType);
        if (userId != null) {
            headers.set("CJSCPPUID", userId.toString());
        }
        return exchange(path, HttpMethod.GET, new HttpEntity<>(headers));
    }

    protected ResponseEntity<String> post(final String path, final UUID userId, final String contentType, final String body) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        if (userId != null) {
            headers.set("CJSCPPUID", userId.toString());
        }
        return exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers));
    }

    protected ResponseEntity<String> exchange(final String path, final HttpMethod method, final HttpEntity<?> entity) {
        return rest.exchange(URI.create(BASE_URL + path), method, entity, String.class);
    }
}
