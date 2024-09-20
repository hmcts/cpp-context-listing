package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import java.util.UUID;


public class DefenceServiceStub {


    public static void stubDefenceQueryApiForSearchCasesByPersonDefendant(String caseId, String defendantId) {
        stubPingFor("defence-service");
        final String url="/defence-service/query/api/rest/defence/case/person-defendant?.*";
        String payload = getPayload("stub-data/defence.query.case-by-defendant.json");

        stubFor(get(urlMatching(url))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.defence.query.get-case-by-person-defendant+json")
                        .withBody(payload
                                .replaceAll("CASE_ID", caseId).replaceAll("DEFENDANT_ID", defendantId))));
    }

    public static void stubDefenceQueryApiForSearchCasesByOrganisationDefendant(String caseId, String defendantId) {
        stubPingFor("defence-service");
        final String url="/defence-service/query/api/rest/defence/case/organisation-defendant?.*";
        String payload = getPayload("stub-data/defence.query.case-by-defendant.json");

        stubFor(get(urlMatching(url))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.defence.query.get-case-by-organisation-defendant+json")
                        .withBody(payload
                                .replaceAll("CASE_ID", caseId).replaceAll("DEFENDANT_ID", defendantId))));
    }
}
