package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.givenPayload;

import java.io.IOException;

public class AzureScheduleServiceStub {

    private static final String ROTA_SL_ENDPOINT_URL = "https://api-ste-ccm-scs.azure-api.net/fa-ste-ccm-scsl/";

    private static final String PATH = "updateAvailableHearingSlots";

    public static void stubUpdateAvailableHearingSlots() throws IOException {
        stubFor(post(urlPathMatching(ROTA_SL_ENDPOINT_URL + PATH))
                .withRequestBody(equalToJson(getSlotDetail()))
                .willReturn(aResponse().withStatus(OK.getStatusCode())));

    }

    private static String getSlotDetail() throws IOException {
        return givenPayload("/stub-data/listing.update.available.hearing.slots.json").toString();
    }
}
