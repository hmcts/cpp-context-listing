package uk.gov.moj.cpp.listing.query.document.generator;

import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;

import java.io.StringReader;
import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PublicCourtListAssemblerTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String START_DATE1 = "2018-11-21";
    private static final String START_DATE2 = "2018-11-22";
    private static final String START_TIME1 = "2018-11-21T12:37:00.000Z";
    private static final String START_TIME2 = "2018-11-22T10:30:00.000Z";
    private static final String COURT_CENTRE_NAME = STRING.next();
    private static final String ADDRESS_1 = STRING.next();
    private static final String ADDRESS_2 = STRING.next();
    private static final String REPORTING_RESTRICTION_REASON = STRING.next();
    private static final String ADDRESS_3 = STRING.next();
    private static final String ADDRESS_4 = STRING.next();
    private static final String ADDRESS_5 = STRING.next();
    private static final String POSTCODE = STRING.next();
    private static final UUID COURT_ROOM_1_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_2_ID = UUID.randomUUID();
    private static final String COURT_ROOM_NAME_1 = STRING.next();
    private static final String COURT_ROOM_NAME_2 = STRING.next();
    private static final String HEARING_TYPE = STRING.next();
    private static final String CASE_REFERENCE = STRING.next();
    private static final String CASE_REFERENCE2 = STRING.next();
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String FIRST_NAME = STRING.next();
    private static final String LAST_NAME = STRING.next();
    private static final String OFFENCE_TITLE = STRING.next();
    private static final UUID JUDICIARY_ID = UUID.randomUUID();
    private static final String QUERY_ACTION_NAME = "listing.public.list";
    private static final String SPACE = " ";
    private static final boolean CHECK_JUDICIARY = true;
    private static final boolean DONT_CHECK_JUDICIARY = false;
    private static final String WELSH = "Welsh";
    private static final boolean NOT_WELSH = false;
    private static final boolean IS_WELSH = true;
    private static final String HEARING_STRING = "Hearing";
    private static final String DEFENDANT_STRING = "Defendant";

    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private StandardPublicCourtListTemplateAssembler publicListService;


    @Before
    public void setup() throws IllegalAccessException {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldBuildDataForPublicCourtList() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(Arrays.asList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelope(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, TRUE).get();

        assertPublicCourtListPayload(publicListData, CHECK_JUDICIARY,false);

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is(START_DATE2));
        JsonObject courtRoomsJo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME2.substring(11, 16)));
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE2));
    }

    @Test
    public void shouldBuildDataForPublicCourtListWithLegalEntityDefendant() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(Arrays.asList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelope(false,true), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, Boolean.FALSE).get();

        assertPublicCourtListPayload(publicListData, CHECK_JUDICIARY,true);

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is(START_DATE2));
        JsonObject courtRoomsJo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME2.substring(11, 16)));
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE2));
    }

    @Test
    public void shouldBuildDataForRestrictedPublicCourtList() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(Arrays.asList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelopeRestrictedPublicList(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, TRUE).get();

        assertRestrictedCasePublicCourtListPayload(publicListData);

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is(START_DATE2));
        JsonObject courtRoomsJo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME2.substring(11, 16)));
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE2));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE));
        assertThat(hearing.getString("reportingRestrictionReason"), is(notNullValue()));
        assertThat(hearing.getJsonArray("defendants").size(), is(2));
        JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("firstName"), is(EMPTY));
        assertThat(defendant.getString("surname"), is( DEFENDANT_STRING));
        assertThat(defendant.getJsonArray("offences").size(), is( 1));
        defendant = hearing.getJsonArray("defendants").getJsonObject(1);
        assertThat(defendant.getString("firstName"), is(FIRST_NAME));
        assertThat(defendant.getString("surname"), is( LAST_NAME));
        assertThat(defendant.getJsonArray("offences").size(), is( 1));

    }


    @Test
    public void shouldBuildDataForPublicCourtListBST() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(Arrays.asList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelopeForBST(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC,TRUE).get();

        JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is("2018-07-21"));
        JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);


        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is("2018-07-21T13:37:00.000Z".substring(11, 16)));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE));
        assertThat(hearing.getString("reportingRestrictionReason"), is(REPORTING_RESTRICTION_REASON));

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is("2018-07-22"));
        JsonObject courtRooms2Jo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        JsonObject timeslot2 = courtRooms2Jo.getJsonArray("timeslots").getJsonObject(0);


        JsonObject hearing2 = timeslot2.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing2.getString("startTime"), is("2018-07-22T11:30:00.000Z".substring(11, 16)));
        assertThat(hearing2.getString("hearingType"), is(HEARING_TYPE));
        assertThat(hearing2.getString("reportingRestrictionReason"), is(REPORTING_RESTRICTION_REASON));
    }

    @Test
    public void shouldBuildDataForPublicCourtListStandAloneApplication() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(Arrays.asList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelope(true, false), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, TRUE).get();

        assertPublicCourtListPayload(publicListData, CHECK_JUDICIARY, false);

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDate2Jo.getString("hearingDate"), is(START_DATE1));
        JsonObject courtRoomsJo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME1.substring(11, 16)));
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE));
    }


    @Test
    public void shouldBuildDataForPublicCourtListWhenJudiciaryIsEmpty() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelopeWithNoJudiciary(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, TRUE).get();
        assertPublicCourtListPayload(publicListData, DONT_CHECK_JUDICIARY, false);
    }

    @Test
    public void shouldBuildDataForWelshPublicCourtList() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(IS_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(Arrays.asList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonEnvelope envelope = buildRequestEnvelope();
        String s1 = envelope.toString();
        String s = envelope.payloadAsJsonObject().toString();
        JsonObject publicListData = publicListService.assemble(envelope, COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, TRUE).get();

        assertWelshPublicCourtListPayload(publicListData);


    }
    private void assertWelshPublicCourtListPayload(JsonObject publicListData) {
        assertThat(publicListData.getString("listType"), is(CourtListType.PUBLIC.toString().toLowerCase()));
        assertThat(publicListData.getString("welshCourtCentreName"), is(WELSH + COURT_CENTRE_NAME));
        assertThat(publicListData.getString("welshCourtCentreAddress1"), is(WELSH + ADDRESS_1 + SPACE + WELSH + ADDRESS_2));
        assertThat(publicListData.getString("welshCourtCentreAddress2"), is(WELSH + ADDRESS_3 + SPACE + WELSH + ADDRESS_4 + SPACE + WELSH + ADDRESS_5 + SPACE + POSTCODE));

        JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is(START_DATE1));

        JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("welshJudiciaryNames"), is("Welsh_Mr Welsh_Recorder Ainsworth Welsh_suffix"));
        assertThat(courtRoomsJo.getString("welshCourtRoomName"), is(WELSH + COURT_ROOM_NAME_1));

        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);


        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME1.substring(11, 16)));
        assertThat(hearing.getString("welshHearingType"), is(HEARING_TYPE)); //waiting for welsh hearingType description to supplied as part of listCourtHearing in 2.5
        assertThat(hearing.getString("welshReportingRestrictionReason"), is(WELSH + REPORTING_RESTRICTION_REASON));

        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE));

        JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("firstName"), is(FIRST_NAME));
        assertThat(defendant.getString("surname"), is(LAST_NAME));

        JsonObject offence = defendant.getJsonArray("offences").getJsonObject(0);
        assertThat(offence.getString("welshOffenceTitle"), is(WELSH + OFFENCE_TITLE));
    }


    private void assertPublicCourtListPayload(JsonObject publicListData, boolean checkJudiciary, final boolean checkLegalEntity) {
        assertThat(publicListData.getString("listType"), is(CourtListType.PUBLIC.toString().toLowerCase()));
        assertThat(publicListData.getString("courtCentreName"), is(COURT_CENTRE_NAME));
        assertThat(publicListData.getString("courtCentreAddress1"), is(ADDRESS_1 + SPACE + ADDRESS_2));
        assertThat(publicListData.getString("courtCentreAddress2"), is(ADDRESS_3 + SPACE + ADDRESS_4 + SPACE + ADDRESS_5 + SPACE + POSTCODE));

        JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is(START_DATE1));

        JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);
        if (checkJudiciary) {
            assertThat(courtRoomsJo.getString("judiciaryNames"), is("Mr Recorder Ainsworth suffix"));
        }

        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));

        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);


        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME1.substring(11, 16)));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE));
        assertThat(hearing.getString("reportingRestrictionReason"), is(REPORTING_RESTRICTION_REASON));


        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE));

        JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);

        if (checkLegalEntity) {
            assertThat(defendant.getString("organisationName"), is(ORGANISATION_NAME));
        } else {
            assertThat(defendant.getString("firstName"), is(FIRST_NAME));
            assertThat(defendant.getString("surname"), is(LAST_NAME));
        }

        JsonObject offence = defendant.getJsonArray("offences").getJsonObject(0);
        assertThat(offence.getString("offenceTitle"), is(OFFENCE_TITLE));
    }

    private void assertRestrictedCasePublicCourtListPayload(JsonObject publicListData) {
        assertThat(publicListData.getString("listType"), is(CourtListType.PUBLIC.toString().toLowerCase()));
        assertThat(publicListData.getString("courtCentreName"), is(COURT_CENTRE_NAME));
        assertThat(publicListData.getString("courtCentreAddress1"), is(ADDRESS_1 + SPACE + ADDRESS_2));
        assertThat(publicListData.getString("courtCentreAddress2"), is(ADDRESS_3 + SPACE + ADDRESS_4 + SPACE + ADDRESS_5 + SPACE + POSTCODE));

        JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is(START_DATE1));

        JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);

        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));

        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);


        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME1.substring(11, 16)));
        assertThat(hearing.getString("hearingType"), is(HEARING_STRING));
        assertThat(hearing.getString("reportingRestrictionReason"), is(EMPTY));


        assertThat(hearing.getString("caseNumber"), is(EMPTY));
        assertThat(hearing.getJsonArray("defendants").size(), is(0));
    }

    private CourtCentreDetails generateCourtCentreDetails(Boolean isWelsh) {
        return CourtCentreDetails.courtCentreDetails()
                .withId(COURT_CENTRE_ID)
                .withCourtCentreName(COURT_CENTRE_NAME)
                .withWelshCourtCentreName(WELSH + COURT_CENTRE_NAME)
                .withAddress1(ADDRESS_1)
                .withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3)
                .withAddress4(ADDRESS_4)
                .withAddress5(ADDRESS_5)
                .withWelshAddress1(WELSH + ADDRESS_1)
                .withWelshAddress2(WELSH + ADDRESS_2)
                .withWelshAddress3(WELSH + ADDRESS_3)
                .withWelshAddress4(WELSH + ADDRESS_4)
                .withWelshAddress5(WELSH + ADDRESS_5)
                .withPostcode(POSTCODE)
                .withWelsh(isWelsh)
                .withCourtRooms(
                        ImmutableMap.of(
                                COURT_ROOM_1_ID,
                                CourtRoomDetails.courtRoomDetails()
                                        .withId(COURT_ROOM_1_ID)
                                        .withCourtRoomName(COURT_ROOM_NAME_1)
                                        .withWelshCourtRoomName(WELSH + COURT_ROOM_NAME_1)
                                        .build(),
                                COURT_ROOM_2_ID,
                                CourtRoomDetails.courtRoomDetails()
                                        .withId(COURT_ROOM_2_ID)
                                        .withCourtRoomName(COURT_ROOM_NAME_2)
                                        .withWelshCourtRoomName(WELSH + COURT_ROOM_NAME_2)
                                        .build()
                        ))
                .build();
    }


    private JsonEnvelope generateJudiciaryEnvelope() {
        return buildJsonEnvelope(buildJudiciaryListData());
    }

    private JsonObject buildJudiciaryListData() {
        String jsonString = FileUtil.getPayload("stubbed.referenceData.getJudiciariesByIdList.json")
                .replace("JUDICIARY_ID", JUDICIARY_ID.toString());
        return convertToJsonObject(jsonString);
    }

    private JsonEnvelope buildRequestEnvelope() {
        //final JsonObject queryPayload = createRequestPayload();
        return buildRequestEnvelope(false, false);
    }

    private JsonEnvelope buildRequestEnvelopeRestrictedPublicList() {
        final JsonObject queryPayload = createRestrictedRequestPayload();
        return buildJsonEnvelope(queryPayload);
    }
    private JsonEnvelope buildRequestEnvelope(final boolean withStandAloneApplication, final boolean withLegalEntityDefendant){
        if(withStandAloneApplication) {
            return buildJsonEnvelope(createRequestPayloadWithStandAloneApplication());
        } else if(withLegalEntityDefendant){
            return buildJsonEnvelope(createRequestPayloadWithLegalEntityDefendant());
        }
        else {
            return buildJsonEnvelope(createRequestPayload());
        }
    }

    private JsonEnvelope buildRequestEnvelopeForBST() {
        String path = FileUtil.getPayload("stubbed.queryView.getCourtListContentForPublicListBST.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("START_DATE1", "2018-07-21")
                .replaceAll("START_DATE2", "2018-07-22")
                .replaceAll("START_TIME1", "2018-07-21T12:37:00.000Z")
                .replaceAll("START_TIME2", "2018-07-22T10:30:00.000Z")
                .replaceAll("REPORTING_RESTRICTION_REASON", REPORTING_RESTRICTION_REASON)
                .replaceAll("HEARING_TYPE", HEARING_TYPE)
                .replaceAll("CASE_REFERENCE1", CASE_REFERENCE)
                .replaceAll("CASE_REFERENCE2", CASE_REFERENCE2)
                .replaceAll("FIRST_NAME", FIRST_NAME)
                .replaceAll("LAST_NAME", LAST_NAME)
                .replaceAll("OFFENCE_TITLE", OFFENCE_TITLE)
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString());
        JsonObject queryPayload = convertToJsonObject(path);

        return buildJsonEnvelope(queryPayload);
    }

    private JsonObject createRequestPayloadWithLegalEntityDefendant() {
        String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentForPublicList-LegalEntityDefendant.json")
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString());
        return convertToJsonObject(payload);
    }

    private JsonObject createRequestPayloadWithStandAloneApplication() {
        String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentForPublicList-StandaloneApplication.json")
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString());
        return convertToJsonObject(payload);
    }

    private JsonEnvelope buildRequestEnvelopeWithNoJudiciary() {
        final JsonObject queryPayload = createRequestPayloadWithNoJudiciary();
        return buildJsonEnvelope(queryPayload);
    }

    private JsonEnvelope buildJsonEnvelope(final JsonObject payload) {
        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_ACTION_NAME)
                        .withUserId(UUID.randomUUID().toString())
                        .build(),
                payload);
    }

    private JsonObject createRequestPayload() {
        String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentForPublicList.json")
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString());
        return convertToJsonObject(payload);
    }

    private JsonObject createRestrictedRequestPayload() {
        String payload = getFileContentWithCommonFieldsReplaced("restrict/stubbed.restrictedCourtListContentForPublicList.json")
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString());
        return convertToJsonObject(payload);
    }
    private JsonObject createRequestPayloadWithNoJudiciary() {
        String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentWithNoJudiciary.json");
        return convertToJsonObject(payload);
    }

    private String getFileContentWithCommonFieldsReplaced(String fileName) {
        return FileUtil.getPayload(fileName)
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("START_DATE1", START_DATE1)
                .replaceAll("START_DATE2", START_DATE2)
                .replaceAll("START_TIME1", START_TIME1)
                .replaceAll("START_TIME2", START_TIME2)
                .replaceAll("REPORTING_RESTRICTION_REASON", REPORTING_RESTRICTION_REASON)
                .replaceAll("HEARING_TYPE", HEARING_TYPE)
                .replaceAll("CASE_REFERENCE1", CASE_REFERENCE)
                .replaceAll("CASE_REFERENCE2", CASE_REFERENCE2)
                .replaceAll("FIRST_NAME", FIRST_NAME)
                .replaceAll("LAST_NAME", LAST_NAME)
                .replaceAll("OFFENCE_TITLE", OFFENCE_TITLE);
    }

    private JsonObject convertToJsonObject(final String jsonString) {
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }
}
