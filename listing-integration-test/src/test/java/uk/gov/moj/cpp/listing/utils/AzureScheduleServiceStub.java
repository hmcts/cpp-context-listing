package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;

public class AzureScheduleServiceStub {

    private static final String ROTA_SL_ENDPOINT_URL = "https://api-ste-ccm-scs.azure-api.net/fa-ste-ccm-scsl/hearingSlots";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void stubUpdateAvailableHearingSlotsService() {
        stubFor(put(urlPathMatching(ROTA_SL_ENDPOINT_URL))
                .willReturn(aResponse().withStatus(OK.getStatusCode())));
    }

    public static void stubGetAvailableHearingSlots(final String queryString) {
        stubFor(get(urlPathMatching(format("%s?%s", ROTA_SL_ENDPOINT_URL, queryString)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                ));
    }

    public static void stubSessionStartDateEmptyRequest(final String queryString) {
        stubFor(get(urlPathMatching(format("%s?%s", ROTA_SL_ENDPOINT_URL, queryString)))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria sessionStartDate cannot be null")
                ));
    }

    public static void stubSessionEndDateEmptyRequest(final String queryString) {
        stubFor(get(urlPathMatching(format("%s?%s", ROTA_SL_ENDPOINT_URL, queryString)))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria sessionEndDate cannot be null")
                ));
    }

    public static void stubOuCodeAndL2CodeEmptyRequest(final String queryString) {
        stubFor(get(urlPathMatching(format("%s?%s", ROTA_SL_ENDPOINT_URL, queryString)))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Either oucodeL2Code or ouCode should be entered")
                ));
    }

    public static void stubPanelEmptyRequest(final String queryString) {
        stubFor(get(urlPathMatching(format("%s?%s", ROTA_SL_ENDPOINT_URL, queryString)))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria panel cannot  be null")
                ));
    }

    public static void stubPageNumberEmptyRequest(final String queryString) {
        stubFor(get(urlPathMatching(format("%s?%s", ROTA_SL_ENDPOINT_URL, queryString)))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria pageNumber cannot  be null")
                ));
    }

    public static void stubPageSizeEmptyRequest(final String queryString) {
        stubFor(get(urlPathMatching(format("%s?%s", ROTA_SL_ENDPOINT_URL, queryString)))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria pageSize cannot be null")
                ));
    }
}
