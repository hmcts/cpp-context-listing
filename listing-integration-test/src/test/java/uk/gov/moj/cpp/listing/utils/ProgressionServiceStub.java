package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.resourceToString;

import java.util.UUID;

public class ProgressionServiceStub {

    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "/progression-service/query/api/rest/progression/prosecutioncases/.*";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.prosecutioncase+json";


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
