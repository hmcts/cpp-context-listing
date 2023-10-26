package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory.clientBuilder;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import java.util.UUID;

import javax.json.Json;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Utility class for setting stubs.
 */
public class WireMockStubUtils {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String MEDIA_TYPE_QUERY_GROUPS = "application/vnd.usersgroups.groups+json";
    private static final String MEDIA_TYPE_QUERY_PROGRESSION_CASE_ID_BY_URN = "application/vnd.progression.query.case-exists-by-caseurn+json";
    private static final String BASE_URI = "http://" + HOST + ":8080";

    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void setupAsAuthorisedUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), MEDIA_TYPE_QUERY_GROUPS);
    }

    public static void setupAsUnauthorisedUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usersgroups.get-unauthorised-group-by-user.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), MEDIA_TYPE_QUERY_GROUPS);
    }

    public static void setupProsecutionCaseByCaseUrn() {
        stubPingFor("progression-service");

        stubFor(get(urlPathEqualTo(format("/progression-service/query/api/rest/progression/search")))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build().toString())));

        waitForStubToBeReady(format("/progression-service/query/api/rest/progression/search"), MEDIA_TYPE_QUERY_PROGRESSION_CASE_ID_BY_URN);
    }

    public static void setupAsAuthorizedUserToQueryCaseByDefendantAndHearingDate(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usergroups-get-for-cases-by-defendant-and-hearingdate.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), MEDIA_TYPE_QUERY_GROUPS);
    }


    public static void waitForPutStubToBeReady(final String resource, final String contentType, final Response.Status expectedStatus) {
        waitAtMost(TEN_SECONDS)
                .until(() -> put(BASE_URI + resource, contentType)
                        .getStatus() == expectedStatus.getStatusCode());
    }

    private static Response put(final String url, final String contentType) {
        return clientBuilder().build()
                .target(url)
                .request()
                .put(entity("", MediaType.valueOf(contentType)));
    }

    static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType, final Status expectedStatus) {
        poll(requestParams(format("{0}/{1}", getBaseUri(), resource), mediaType)).until(status().is(expectedStatus));
    }

}
