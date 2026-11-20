package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static javax.json.Json.createObjectBuilder;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.resourceToString;

import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;

import javax.json.Json;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class ProgressionServiceStub {

    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "/progression-service/query/api/rest/progression/prosecutioncases/.*";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.prosecutioncase+json";

    // SPRDT-1363: listing proxies split-hearing to progression, which owns the split decision.

    // haproxy rewrites /progression-command-api/... to /progression-service/... before it reaches
    // the stub, so this must be the post-rewrite path, not the client's BASE_URI.
    private static final String PROGRESSION_SPLIT_HEARING =
            "/progression-service/command/api/rest/progression/hearing/[^/]+/split";

    public static void stubSplitHearing() {
        stubFor(WireMock.post(urlMatching(PROGRESSION_SPLIT_HEARING))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())));
    }

    /**
     * SPRDT-1411. Progression rejects a split it cannot apply - a stale request, an unknown hearing,
     * a payload it will not accept. Registered after {@link #stubSplitHearing()}, so it wins.
     */
    public static void stubSplitHearingRejectedWith(final int status, final String errorCode, final String message) {
        stubFor(WireMock.post(urlMatching(PROGRESSION_SPLIT_HEARING))
                .willReturn(aResponse().withStatus(status)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(createObjectBuilder()
                                .add("errorCode", errorCode)
                                .add("message", message)
                                .build()
                                .toString())));
    }

    /**
     * Progression refuses a split of a resulted hearing with its own shape - the reason under
     * {@code error} and the hearing under {@code id}, with no {@code message} or {@code errorCode}.
     */
    public static void stubSplitHearingRejectedAsResulted(final String reason, final String hearingId) {
        stubFor(WireMock.post(urlMatching(PROGRESSION_SPLIT_HEARING))
                .willReturn(aResponse().withStatus(HttpStatus.SC_CONFLICT)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(createObjectBuilder()
                                .add("error", reason)
                                .add("id", hearingId)
                                .build()
                                .toString())));
    }

    /** The split requests listing forwarded for a given source hearing. */
    public static List<LoggedRequest> splitRequestsForHearing(final String hearingId) {
        return findAll(postRequestedFor(urlMatching(PROGRESSION_SPLIT_HEARING)))
                .stream()
                .filter(request -> request.getUrl().contains(hearingId))
                .toList();
    }

    public static void stubProgressionServiceCivilCase() {
        stubFor(get(urlMatching(PROGRESSION_QUERY_PROSECUTION_CASE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/progression.query.prosecutioncase-civil-case.json"))));
    }

    public static void stubProgressionServiceCivilCaseSummons() {
        stubFor(get(urlMatching(PROGRESSION_QUERY_PROSECUTION_CASE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/progression.query.prosecutioncase-civil-case-summons.json"))));
    }
}
