package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.time.LocalDate;


public class StagingHmiStub {

    private static final String STAGINGHMI_QUERY_ORGANISATION_UNIT_HMI_STATUS = "/staginghmi-service/query/api/rest/staginghmi/organisation-units/hmi-status";
    private static final String STAGINGHMI_ORGUNIT_HMI_STATUS_MEDIA_TYPE = "application/vnd.staginghmi.query.organisation-units-hmi-status+json";
    private static final String STAGINHHMI_QUERY_SESSIONS = "/staginghmi-service/query/api/rest/staginghmi/sessions";
    private static final String STAGINHMI_QUERY_MEDIA_TYPE = "application/vnd.staginghmi.query.sessions+json";

    public static void stubGetStagingIsHmiEnabled() {
        stubPingForStagingHmiService();

        String payload = getPayload("stub-data/staginghmi.query.organisation-unit-hmi-status.json");

        stubFor(get(urlPathMatching(STAGINGHMI_QUERY_ORGANISATION_UNIT_HMI_STATUS))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", STAGINGHMI_ORGUNIT_HMI_STATUS_MEDIA_TYPE)
                        .withBody(payload)));
    }


    public static void stubHmiMagsSession() {
        stubStagingHmiGetSessions("MAGISTRATES", LocalDate.now().plusDays(2), "stub-data/staginghmi.query.sessions.json");
    }

    public static void stubHmiNoSessionsAvailable() {
        stubStagingHmiGetSessions("MAGISTRATES", LocalDate.now().plusDays(2), "stub-data/staginghmi.query.nosessions.json");
    }

    private static void stubStagingHmiGetSessions(final String jurisdictionType, final LocalDate sessionStartDate, final String payloadPath) {
        stubPingForStagingHmiService();

        String payload = getPayload(payloadPath)
                .replaceAll("OUCODE", jurisdictionType.equalsIgnoreCase("MAGISTRATES") ? "B43OX00" : "C22WC00")
                .replaceAll("SESSION_DATE", sessionStartDate.toString())
                .replaceAll("SESSION_START_TIME", sessionStartDate.atStartOfDay().plusHours(10).toString())
                .replaceAll("VENUE_ID", jurisdictionType.equalsIgnoreCase("MAGISTRATES") ? "392" : "435");


        stubFor(get(urlPathMatching(STAGINHHMI_QUERY_SESSIONS))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", STAGINHMI_QUERY_MEDIA_TYPE)
                        .withBody(payload)));
    }


    private static void stubPingForStagingHmiService() {
        InternalEndpointMockUtils.stubPingFor("staginghmi-service");
    }

}
