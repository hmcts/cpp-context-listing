package uk.gov.moj.cpp.listing.utils;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;

import java.util.UUID;


public class ReferenceDataStub {

    private static final String REFERENCE_DATA_COURT_CENTRE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms/{0}";
    private static final String REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE = "application/vnd.referencedata.courtroom+json";
    private static final String REFERENCE_DATA_JUDICIARIES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/judiciaries";
    private static final String REFERENCE_DATA_JUDICIARIES_MEDIA_TYPE = "application/vnd.referencedata.judiciaries+json";

    public static void stubGetReferenceDataCourtCentre(final CourtCentreData courtReferenceData) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        String payload = getPayload("stub-data/referencedata.query.courtroom.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString())
                .replace("DEFAULT_START_TIME", courtReferenceData.getDefaultStartTime().toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", courtReferenceData.getDefaultDurationHoursMins())
                .replace("COURT_ROOM_ID", courtReferenceData.getCourtRoomId() != null ? courtReferenceData.getCourtRoomId().toString() : randomUUID().toString());

        stubFor(get(urlPathEqualTo(format(REFERENCE_DATA_COURT_CENTRE_QUERY_URL, courtReferenceData.getCourtCentreId().toString())))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(format(REFERENCE_DATA_COURT_CENTRE_QUERY_URL, courtReferenceData.getCourtCentreId()), REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataJudiciaries(final UUID judiciaryId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        String payload = getPayload("stub-data/referencedata.query.judiciaries.json")
                .replace("JUDICIARY_ID", judiciaryId.toString());

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_JUDICIARIES_QUERY_URL))
                .withQueryParam("ids", equalTo(judiciaryId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_JUDICIARIES_MEDIA_TYPE)
                        .withBody(payload)));
        waitForStubToBeReady(REFERENCE_DATA_JUDICIARIES_QUERY_URL + "?ids=" + judiciaryId.toString(), REFERENCE_DATA_JUDICIARIES_MEDIA_TYPE);
    }









  
}
