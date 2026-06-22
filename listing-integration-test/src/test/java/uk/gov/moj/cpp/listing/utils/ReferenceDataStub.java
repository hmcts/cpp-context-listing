package uk.gov.moj.cpp.listing.utils;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;



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
    private static final String REFERENCE_DATA_ORGANISATIONAL_UNITS_URL = "/referencedata-service/query/api/rest/referencedata/organisationunits";
    private static final String REFERENCE_DATA_ORGANISATIONAL_UNITS_MEDIA_TYPE = "application/vnd.referencedata.query.organisationunits+json";
    private static final String REFERENCE_DATA_OU_COURTROOM_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms";
    private static final String REFERENCE_DATA_OU_COURTROOM_MEDIA_TYPE = "application/vnd.referencedata.ou-courtroom+json";
    private static final String REFERENCE_DATA_OU_COURTROOMS_MEDIA_TYPE = "application/vnd.referencedata.ou-courtrooms+json";
    private static final String REFERENCE_DATA_PROSECUTOR_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/prosecutors/%s";
    private static final String REFERENCE_DATA_PROSECUTOR_MEDIA_TYPE = "application/vnd.referencedata.query.prosecutor+json";

    private static final Map<UUID, String> COURT_CENTER_IDS = new HashMap<>(){{
        put(fromString("9b583616-049b-30f9-a14f-028a53b7cfe8"), "Liverpool Crown Court");
        put(fromString("e3e762ed-8271-3454-b59b-8a13f7cc8870"), "Manchester Crown Court");
        put(fromString("8de7f2e2-5705-3be5-af9a-321a891ab708"), "Newcastle upon Tyne Crown Court");
        put(fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08"), "Preston Crown Court");
    }};

    public static final Map<UUID, Integer> COURT_ROOM_IDS = new HashMap<>(){{
        put(fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19"), 1962);
        put(fromString("18982e9c-2475-36a4-a852-09ab720acfc9"), 1963);
        put(fromString("28b922c3-0396-3c68-970f-5b805c7ab1bb"), 1964);
        put(fromString("02d9847e-00e9-3c6c-b25c-1adbf5355a52"), 1965);

    }};

    public static List<UUID> getCourtCenterIds() {
        return new ArrayList<>(COURT_CENTER_IDS.keySet());
    }

    public static String getCourtCenterName(UUID courtCenterId) {
        return COURT_CENTER_IDS.get(courtCenterId);
    }

    public static UUID getRandomCourtCenterId() {
        return getRandomCourtCenterId(emptyList());
    }

    public static UUID getRandomCourtCenterId(List<UUID> exceptionList) {
        var availableList = new ArrayList<>(COURT_CENTER_IDS.keySet());
        availableList.removeAll(exceptionList);

        if (availableList.isEmpty()) {
            throw new IllegalStateException("No available court centers after exclusions");
        }

        var index = (int)(Math.random() * availableList.size());
        return availableList.get(index);
    }

    public static UUID getRandomCourtRoomId() {
        return getRandomCourtRoomId(emptyList());
    }

    public static UUID getRandomCourtRoomId(List<UUID> exceptionList) {
        var availableList = new ArrayList<>(COURT_ROOM_IDS.keySet());
        availableList.removeAll(exceptionList);

        if (availableList.isEmpty()) {
            throw new IllegalStateException("No available court id after exclusions");
        }

        int randomIndex = (int)(Math.random() * availableList.size());
        return availableList.get(randomIndex);
    }

    public static void stubGetReferenceDataCourtMappings(final CourtCentreData courtReferenceData) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        String payload = getPayload("stub-data/referencedata.query.cp-xhibit-court-mappings.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_COURT_MAPPINGS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_MAPPINGS_MEDIA_TYPE)
                        .withBody(payload)));
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
    }

    public static void stubGetReferenceDataCpCourtRooms() {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        String payload = getPayload("stub-data/referencedata.ou-courtrooms.json");

        stubFor(get(urlPathMatching(REFERENCE_DATA_OU_COURTROOM_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_OU_COURTROOMS_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static void stubGetReferenceDataCourtCentre(final CourtCentreData courtReferenceData) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.courtroom.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString())
                .replace("DEFAULT_START_TIME", courtReferenceData.getDefaultStartTime().toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", courtReferenceData.getDefaultDurationHoursMins())
                .replace("COURT_ROOM_ID", courtReferenceData.getCourtRoomId() != null ? courtReferenceData.getCourtRoomId().toString() : getRandomCourtRoomId().toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_COURT_CENTRE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static void stubGetReferenceDataCourtCentres(CourtCentreData... courtCenters) {
        stubPingForReferenceDataService();
        final JsonArrayBuilder jsonArrBuilder = createArrayBuilder();
        stream(courtCenters).toList().forEach( cc -> {
                    String payload = getPayload("stub-data/referencedata.query.courtroom.json")
                            .replace("COURT_CENTRE_ID", cc.getCourtCentreId().toString())
                            .replace("DEFAULT_START_TIME", cc.getDefaultStartTime().toString())
                            .replace("DEFAULT_DURATION_HOURS_MINS", cc.getDefaultDurationHoursMins())
                            .replace("COURT_ROOM_ID", cc.getCourtRoomId() != null ? cc.getCourtRoomId().toString() : getRandomCourtRoomId().toString());
                    jsonArrBuilder.add(createReader(new java.io.StringReader(payload)).readObject());
                });
        final JsonObject rootJsonObj = createObjectBuilder().add("organisationunits", jsonArrBuilder.build()).build();
        stubFor(get(urlPathMatching(REFERENCE_DATA_GET_COURTROOM_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(rootJsonObj.toString())));
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
    }

    public static void stubGetReferenceDataCourtCentreById(final CourtCentreData courtReferenceData) {
        stubPingForReferenceDataService();

        final String urlPath = String.format(REFERENCE_DATA_COURT_ROOM_QUERY_URL, courtReferenceData.getCourtCentreId());

        String payload = getPayload("stub-data/referencedata.query.courtroom.json")
                .replace("COURT_CENTRE_ID", courtReferenceData.getCourtCentreId().toString())
                .replace("DEFAULT_START_TIME", courtReferenceData.getDefaultStartTime().toString())
                .replace("DEFAULT_DURATION_HOURS_MINS", courtReferenceData.getDefaultDurationHoursMins())
                .replace("COURT_ROOM_ID", courtReferenceData.getCourtRoomId() != null ? courtReferenceData.getCourtRoomId().toString() : getRandomCourtRoomId().toString());

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(payload)));
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
    }


    public static void stubGetReferenceDataCourtCentreById(UUID courtCentreId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = String.format(REFERENCE_DATA_ORGANISATION_UNIT_QUERY_URL, courtCentreId.toString());

        String payload = getPayload("stub-data/referencedata.query.organisation-unit-" + courtCentreId + ".json")
                .replace("COURT_CENTRE_ID", courtCentreId.toString());

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ORGANISATION_UNIT_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static void stubGetReferenceDataOrganisationUnitById(UUID courtCentreId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");

        final String urlPath = String.format(REFERENCE_DATA_ORGANISATION_UNIT_QUERY_URL, courtCentreId.toString());

        String payload = getPayload("stub-data/referencedata.query.organisation-unit.json")
                .replace("COURT_CENTRE_ID", courtCentreId.toString());

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ORGANISATION_UNIT_MEDIA_TYPE)
                        .withBody(payload)));
    }

    public static void stubGetReferenceDataJudiciaries(final UUID judiciaryId) {
        stubPingForReferenceDataService();
        String payload = getPayload("stub-data/referencedata.query.judiciaries.json")
                .replace("JUDICIARY_ID", judiciaryId.toString());

        stubFor(get(urlPathMatching(REFERENCE_DATA_JUDICIARIES_QUERY_URL))
                .withQueryParam("ids", containing(judiciaryId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_JUDICIARIES_MEDIA_TYPE)
                        .withBody(payload)));
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
    }

    public static void stubGetProsecutorPoliceFlag(final UUID prosecutorId) {
        stubPingForReferenceDataService();

        final String urlPath = String.format(REFERENCE_DATA_PROSECUTOR_QUERY_URL, prosecutorId.toString());
        String payload = getPayload("stub-data/referencedata.query.get-prosecutor.json")
                .replace("PROSECUTOR_ID",prosecutorId.toString());

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_PROSECUTOR_MEDIA_TYPE)
                        .withBody(payload)));
    }
}
