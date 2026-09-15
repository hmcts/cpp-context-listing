package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.resourceToString;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class ProgressionServiceStub {

    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "/progression-service/query/api/rest/progression/prosecutioncases/.*";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.prosecutioncase+json";

    // SPRDT-1363: listing proxies split-hearing to progression, which owns the split decision.
    private static final String PROGRESSION_COMMAND_ENDPOINT = "/progression-command-api/command/api/rest/progression";

    public static String splitHearingPath(final String hearingId) {
        return "%s/hearing/%s/split".formatted(PROGRESSION_COMMAND_ENDPOINT, hearingId);
    }

    public static void stubSplitHearing(final String hearingId, final int status) {
        stubSplitHearing(hearingId, status, "");
    }

    public static void stubSplitHearing(final String hearingId, final int status, final String body) {
        stubFor(WireMock.post(urlEqualTo(splitHearingPath(hearingId)))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    /**
     * The body progression actually received, so an IT can assert the conversion field by field
     * rather than trusting the stub was merely hit.
     */
    public static JsonObject splitHearingRequestBody(final String hearingId) {
        final List<LoggedRequest> requests = findAll(postRequestedFor(urlEqualTo(splitHearingPath(hearingId))));
        if (requests.isEmpty()) {
            throw new AssertionError("progression split-hearing was never called for hearing " + hearingId);
        }
        try (var reader = Json.createReader(new java.io.StringReader(requests.get(0).getBodyAsString()))) {
            return reader.readObject();
        }
    }

    public static int splitHearingCallCount(final String hearingId) {
        return findAll(postRequestedFor(urlEqualTo(splitHearingPath(hearingId)))).size();
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
