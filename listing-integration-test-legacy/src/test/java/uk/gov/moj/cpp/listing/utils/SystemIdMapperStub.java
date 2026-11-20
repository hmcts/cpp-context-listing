package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;

import java.util.UUID;

public class SystemIdMapperStub {

    private static final String SYSTEM_ID_MAPPER_URL = "/system-id-mapper-api/rest/systemid/mappings/*";

    public static void stubIdMapperReturningExistingAssociation(final UUID associationId) {
        stubPingFor("system-id-mapper-api");
        stubFor(get(urlPathMatching(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(systemIdMappingResponseTemplate(associationId))));
    }

    private static String systemIdMappingResponseTemplate(final UUID associationId) {

        return "{\n" +
                "  \"mappingId\": \"166c0ae9-e276-4d29-b669-cb32013228b3\",\n" +
                "  \"sourceId\": \"ID01\",\n" +
                "  \"sourceType\": \"SystemACaseId\",\n" +
                "  \"targetId\": \"" + associationId + "\",\n" +
                "  \"targetType\": \"caseId\",\n" +
                "  \"createdAt\": \"2016-09-07T14:30:53.294Z\"\n" +
                "}";
    }
}
