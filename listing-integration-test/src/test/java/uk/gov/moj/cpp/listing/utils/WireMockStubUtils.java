package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Utility class for setting stubs.
 */
public class WireMockStubUtils {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

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
    }

    public static void setupUsersGroupPermissionsForApplicationTypeStub() {
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions.*"))
                .atPriority(1)
                .withHeader("Accept", containing("application/vnd.usersgroups.is-logged-in-user-has-permission-for-action+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.valueOf(createObjectBuilder().add("permissions", createArrayBuilder().build()).build()))));

    }

    public static void setupAsUnauthorisedUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usersgroups.get-unauthorised-group-by-user.json"))));
    }

    public static void setupProsecutionCaseByCaseUrn() {
        stubPingFor("progression-service");

        stubFor(get(urlPathEqualTo(format("/progression-service/query/api/rest/progression/search")))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build().toString())));
    }

//    public static void setupProgressionNotesStubs() {
//        stubPingFor("progression-service");
//
//        stubFor(get(urlPathMatching("/progression-service/query/api/rest/progression/cases/.*/notes"))
//                .willReturn(aResponse().withStatus(OK.getStatusCode())
//                        .withHeader(ID, randomUUID().toString())
//                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
//                        .withBody(createCaseNotesResponse().toString())));
//
//        stubFor(get(urlPathMatching("/progression-service/query/api/rest/progression/applications/.*/notes"))
//                .willReturn(aResponse().withStatus(OK.getStatusCode())
//                        .withHeader(ID, randomUUID().toString())
//                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
//                        .withBody(createApplicationNotesResponse().toString())));
//    }
//
//    private static JsonObject createCaseNotesResponse() {
//        return createObjectBuilder()
//                .add("caseNotes", createArrayBuilder()
//                        .add(createObjectBuilder()
//                                .add("note", "Test case note 1")
//                                .add("isPinned", true)
//                                .add("createdDate", "2024-01-01T10:00:00Z"))
//                        .add(createObjectBuilder()
//                                .add("note", "Test case note 2")
//                                .add("isPinned", false)
//                                .add("createdDate", "2024-01-02T11:00:00Z")))
//                .build();
//    }
//
//    private static JsonObject createApplicationNotesResponse() {
//        return createObjectBuilder()
//                .add("applicationNotes", createArrayBuilder()
//                        .add(createObjectBuilder()
//                                .add("note", "Test application note 1")
//                                .add("isPinned", true)
//                                .add("createdDate", "2024-01-01T10:00:00Z"))
//                        .add(createObjectBuilder()
//                                .add("note", "Test application note 2")
//                                .add("isPinned", false)
//                                .add("createdDate", "2024-01-02T11:00:00Z")))
//                .build();
//    }

    public static void setupAsAuthorizedUserToQueryCaseByDefendantAndHearingDate(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usergroups-get-for-cases-by-defendant-and-hearingdate.json"))));
    }


    public static void waitForPutStubToBeReady(final String resource, final String contentType, final Response.Status expectedStatus) {

    }

}
