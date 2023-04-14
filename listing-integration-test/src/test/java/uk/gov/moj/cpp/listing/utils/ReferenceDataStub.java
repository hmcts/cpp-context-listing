package uk.gov.moj.cpp.listing.utils;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;

import java.time.LocalTime;
import java.util.UUID;


public class ReferenceDataStub {

    private static final String REFERENCE_DATA_COURT_MAPPINGS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/cp-xhibit-court-mappings";
    private static final String REFERENCE_DATA_COURT_MAPPINGS_MEDIA_TYPE = "application/vnd.referencedata.query.cp-xhibit-court-mappings+json";
    private static final String REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/cp-xhibit-courtroom-mappings";
    private static final String REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_MEDIA_TYPE = "application/vnd.referencedata.query.cp-xhibit-courtroom-mappings+json";
    private static final String REFERENCE_DATA_COURT_CENTRE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms/.*";
    private static final String REFERENCE_DATA_COURT_ROOM_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms/%s";
    private static final String REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE = "application/vnd.referencedata.courtroom+json";
    private static final String REFERENCE_DATA_ORGANISATION_UNIT_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/organisation-units/%s";
    private static final String REFERENCE_DATA_ORGANISATION_UNIT_MEDIA_TYPE = "application/vnd.referencedata.query.organisation-unit+json";
    private static final String REFERENCE_DATA_ALL_CROWN_COURT_CENTRE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms";
    private static final String REFERENCE_DATA_JUDICIARIES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/judiciaries";
    private static final String REFERENCE_DATA_JUDICIARIES_MEDIA_TYPE = "application/vnd.referencedata.judiciaries+json";
    private static final String REFERENCE_DATA_HEARING_TYPES_URL = "/referencedata-service/query/api/rest/referencedata/hearing-types";
    private static final String REFERENCE_DATA_HEARING_TYPES_MEDIA_TYPE = "application/vnd.referencedata.query.all-hearing-types+json";
    private static final String REFERENCE_DATA_GET_COURTROOM_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms";
    private static final String REFERENCE_DATA_GET_COURTROOM_MAPPINGS_MEDIA_TYPE = "application/vnd.referencedata.ou-courtroom+json";
    private static final String REFERENCE_DATA_ORGANISATIONAL_UNITS_URL = "/referencedata-service/query/api/rest/referencedata/organisationunits";
    private static final String REFERENCE_DATA_ORGANISATIONAL_UNITS_MEDIA_TYPE = "application/vnd.referencedata.query.organisationunits+json";
    private static final String REFERENCE_DATA_OU_COURTROOM_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms";
    private static final String REFERENCE_DATA_OU_COURTROOM_MEDIA_TYPE = "application/vnd.referencedata.ou-courtroom+json";

    public static void stubGetReferenceDataCourtMappings(final CourtCentreData courtReferenceData) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        String payload = getPayload("stub-data/referencedata.query.cp-xhibit-court-mappings.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_COURT_MAPPINGS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_MAPPINGS_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_COURT_MAPPINGS_QUERY_URL, REFERENCE_DATA_COURT_MAPPINGS_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataXhibitCourtRoomMappings(final UUID courtRoomUUID) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        String payload = getPayload("stub-data/referencedata.query.cp-xhibit-courtroom-mappings.json")
                .replace("COURT_ROOM_UUID", courtRoomUUID.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_QUERY_URL, REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataXhibitCourtRoomMappings(final UUID courtRoomUUID, final UUID courtCentreId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        String payload = getPayload("stub-data/referencedata.query.cp-xhibit-courtroom-mappings.json")
                .replace("COURT_ROOM_UUID", courtRoomUUID.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_QUERY_URL))
                .withQueryParam("ouId", equalTo(courtCentreId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_QUERY_URL, REFERENCE_DATA_CP_XHIBIT_COURTROOM_MAPPINGS_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataCpCourtRooms(final UUID courtRoomUUID, final int courtRoomId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        String payload = getPayload("stub-data/referencedata.ou-courtroom.json")
                .replace("COURT_ROOM_ID", Integer.toString(courtRoomId))
                .replace("COURT_ROOM_UUID", courtRoomUUID.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_OU_COURTROOM_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_OU_COURTROOM_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_OU_COURTROOM_URL, REFERENCE_DATA_OU_COURTROOM_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataCourtCentre(final CourtCentreData courtReferenceData) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.courtroom.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString())
                .replace("DEFAULT_START_TIME", courtReferenceData.getDefaultStartTime().toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", courtReferenceData.getDefaultDurationHoursMins())
                .replace("COURT_ROOM_ID", courtReferenceData.getCourtRoomId() != null ? courtReferenceData.getCourtRoomId().toString() : randomUUID().toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_COURT_CENTRE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_COURT_CENTRE_QUERY_URL, REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataCourtCentreHmiListingEnabledWithoutCourtRoomSelection(final CourtCentreData courtReferenceData) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.courtroom.hmi.enabled.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString())
                .replace("DEFAULT_START_TIME", courtReferenceData.getDefaultStartTime().toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", courtReferenceData.getDefaultDurationHoursMins());

        stubFor(get(urlPathMatching(REFERENCE_DATA_COURT_CENTRE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_COURT_CENTRE_QUERY_URL, REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataCourtCentreHmiListingEnabled(final CourtCentreData courtReferenceData) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.courtroom.hmi.enabled.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString())
                .replace("DEFAULT_START_TIME", courtReferenceData.getDefaultStartTime().toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", courtReferenceData.getDefaultDurationHoursMins())
                .replace("COURT_ROOM_ID", courtReferenceData.getCourtRoomId() != null ? courtReferenceData.getCourtRoomId().toString() : randomUUID().toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_COURT_CENTRE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_COURT_CENTRE_QUERY_URL, REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataCourtCentreById(final CourtCentreData courtReferenceData) {
        stubPingForReferenceDataService();

        final String urlPath = String.format(REFERENCE_DATA_COURT_ROOM_QUERY_URL, courtReferenceData.getCourtCentreId());

        String payload = getPayload("stub-data/referencedata.query.courtroom.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString())
                .replace("DEFAULT_START_TIME", courtReferenceData.getDefaultStartTime().toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", courtReferenceData.getDefaultDurationHoursMins())
                .replace("COURT_ROOM_ID", courtReferenceData.getCourtRoomId() != null ? courtReferenceData.getCourtRoomId().toString() : randomUUID().toString());

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(urlPath, REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }

    public static void stubGetAllCrownCourtCentres(final UUID courtCentreIdOne, final UUID courtCentreIdTwo) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/reference.query.courtroom_crown_courts_only.json")
                .replace("$COURT_CENTRE_ID_ONE", courtCentreIdOne.toString())
                .replace("$COURT_CENTRE_ID_TWO", courtCentreIdTwo.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_ALL_CROWN_COURT_CENTRE_QUERY_URL))
                .withQueryParam("oucodeL1Code", equalTo("C"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));

        final String confirmationUrl = REFERENCE_DATA_ALL_CROWN_COURT_CENTRE_QUERY_URL + "?oucodeL1Code=C";

        waitForStubToBeReady(confirmationUrl, REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }


    public static void stubGetReferenceDataCourtCentreById(UUID courtCentreId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = String.format(REFERENCE_DATA_ORGANISATION_UNIT_QUERY_URL, courtCentreId.toString());

        String payload = getPayload("stub-data/referencedata.query.organisation-unit.json")
                .replace("COURT_CENTRE_ID", courtCentreId.toString());

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ORGANISATION_UNIT_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(urlPath, REFERENCE_DATA_ORGANISATION_UNIT_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataCourtWithHmiListingEnabledCentreById(UUID courtCentreId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = String.format(REFERENCE_DATA_ORGANISATION_UNIT_QUERY_URL, courtCentreId.toString());

        String payload = getPayload("stub-data/referencedata.query.organisation-unit-hmi-listing-enabled.json")
                .replace("COURT_CENTRE_ID", courtCentreId.toString());

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ORGANISATION_UNIT_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(urlPath, REFERENCE_DATA_ORGANISATION_UNIT_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataJudiciaries(final UUID judiciaryId) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.judiciaries.json")
                .replace("JUDICIARY_ID", judiciaryId.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_JUDICIARIES_QUERY_URL))
                .withQueryParam("ids", equalTo(judiciaryId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_JUDICIARIES_MEDIA_TYPE)
                        .withBody(payload)));
        waitForStubToBeReady(REFERENCE_DATA_JUDICIARIES_QUERY_URL + "?ids=" + judiciaryId.toString(), REFERENCE_DATA_JUDICIARIES_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataHearingTypes(final UUID hearingTypeId) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.hearing-types.json")
                .replace("HEARING_TYPE_ID", hearingTypeId.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_HEARING_TYPES_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_HEARING_TYPES_MEDIA_TYPE)
                        .withBody(payload)));
        waitForStubToBeReady(REFERENCE_DATA_HEARING_TYPES_URL, REFERENCE_DATA_HEARING_TYPES_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataCourtRoom(final UUID courtCentreIdOne, final LocalTime defaultStartTime, final String defaultDurationHoursMins, final UUID courtRoomId) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.courtroom.json")
                .replace("COURT_CENTRE_ID", courtCentreIdOne.toString())
                .replace("DEFAULT_START_TIME", defaultStartTime.toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", defaultDurationHoursMins)
                .replace("COURT_ROOM_ID", courtRoomId.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_GET_COURTROOM_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));

        waitForStubToBeReady(REFERENCE_DATA_GET_COURTROOM_URL, REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }

    private static void stubPingForReferenceDataService() {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
    }

    public static void stubOrganisationUnit(final UUID ouId) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.organisationunits.json")
                .replace("OU_ID", ouId.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_ORGANISATIONAL_UNITS_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ORGANISATIONAL_UNITS_MEDIA_TYPE)
                        .withBody(payload)));
        waitForStubToBeReady(REFERENCE_DATA_ORGANISATIONAL_UNITS_URL, REFERENCE_DATA_ORGANISATIONAL_UNITS_MEDIA_TYPE);
    }
}
