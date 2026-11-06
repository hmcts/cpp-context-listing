package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.resourceToString;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * CAD-1760: serves a civil case Listing has never listed from Progression, with every offence's
 * civilOffence.isExParte set to the given value.
 */
public class ProgressionCivilCaseStub {

    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "/progression-service/query/api/rest/progression/prosecutioncases/";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.prosecutioncase+json";

    private ProgressionCivilCaseStub() {
    }

    public static void stubProgressionCivilCaseWithExParte(final UUID caseId, final boolean isExParte) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode response = objectMapper.readTree(resourceToString("stub-data/progression.query.prosecutioncase-civil-case.json"));
            final ObjectNode prosecutionCase = (ObjectNode) response.get("prosecutionCase");
            prosecutionCase.put("id", caseId.toString());
            prosecutionCase.get("defendants").forEach(defendant -> defendant.get("offences").forEach(offence ->
                    ((ObjectNode) offence).putObject("civilOffence").put("isExParte", isExParte)));

            stubFor(get(urlPathEqualTo(PROGRESSION_QUERY_PROSECUTION_CASE + caseId))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader("CPPID", UUID.randomUUID().toString())
                            .withHeader("Content-Type", PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE)
                            .withBody(objectMapper.writeValueAsString(response))));
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to stub progression prosecution case " + caseId, e);
        }
    }
}
