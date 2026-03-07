package uk.gov.moj.cpp.listing.query.document.generator;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;

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
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublicCourtListAssemblerTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String START_DATE1 = "2018-11-21";
    private static final String START_DATE2 = "2018-11-22";
    private static final String START_DATE2_WELSH = "22 Tachwedd 2018";
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
    private static final String HEARING_TYPE1 = STRING.next();
    private static final String HEARING_TYPE2 = STRING.next();
    private static final String CASE_REFERENCE = STRING.next();
    private static final String CASE_REFERENCE2 = STRING.next();
    private static final String FIRST_NAME1 = STRING.next();
    private static final String LAST_NAME1 = STRING.next();
    private static final String FIRST_NAME2 = STRING.next();
    private static final String LAST_NAME2 = STRING.next();
    private static final String FIRST_NAME3 = STRING.next();
    private static final String LAST_NAME3 = STRING.next();
    private static final String OFFENCE_TITLE = STRING.next();
    private static final UUID JUDICIARY_ID = UUID.randomUUID();
    private static final String QUERY_ACTION_NAME = "listing.public.list";
    private static final String SPACE = " ";
    private static final boolean CHECK_JUDICIARY = true;
    private static final boolean DONT_CHECK_JUDICIARY = false;
    private static final String WELSH = "Welsh";
    private static final boolean NOT_WELSH = false;
    private static final boolean IS_WELSH = true;
    private static final String DEFENDANT_STRING = "Defendant";
    private static final String WELSH_DEFENDANT_STRING = "Diffynnydd";
    private static final String APPLICATION_TYPE = STRING.next();
    private static final String APPLICATION_PARTICULARS = STRING.next();
    private static final String REPORTING_RESTRICTION="RestrictionApplied";

    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Mock
    private JudiciaryNameMapper judiciaryNameMapper;

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<JsonObject> argumentCaptor;

    @InjectMocks
    private StandardPublicCourtListTemplateAssembler publicListService;


    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldBuildDataForPublicCourtList() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        JsonObject publicListData = publicListService.assemble(buildRequestEnvelope(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), PUBLIC, TRUE, false).get();

        assertPublicCourtListPayload(publicListData, CHECK_JUDICIARY);

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is(START_DATE2));
        JsonObject courtRoomsJo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME2.substring(11, 16)));
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE2));
        verify(judiciaryNameMapper, times(2)).getName(argumentCaptor.capture());
        final JsonObject judiciary = argumentCaptor.getValue();
        assertThat(judiciary, is(notNullValue()));
    }

    @Test
    public void shouldBuildDataForBulkCasePublicCourtList() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        JsonObject publicListData = publicListService.assemble(buildBulkRequestEnvelope(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), PUBLIC, TRUE, FALSE).get();

        assertBulkPublicCourtListPayload(publicListData, CHECK_JUDICIARY);

        verify(judiciaryNameMapper, times(1)).getName(argumentCaptor.capture());
        final JsonObject judiciary = argumentCaptor.getValue();
        assertThat(judiciary, is(notNullValue()));
    }

    @Test
    public void shouldBuildDataForRestrictedCasePublicCourtList() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelopeRestrictedPublicList(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, FALSE, false).get();

        assertRestrictedCasePublicCourtListPayload(publicListData);

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is(START_DATE2));
        JsonObject courtRoomsJo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME2.substring(11, 16)));
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE2));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE1));
        assertThat(hearing.getString("reportingRestrictionReason"), is(notNullValue()));
        assertThat(hearing.getJsonArray("defendants").size(), is(2));
        JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("firstName"), is(EMPTY));
        assertThat(defendant.getString("surname"), is(DEFENDANT_STRING));
        assertThat(defendant.getJsonArray("offences").size(), is(1));
        assertThat(defendant.getJsonArray("reportingRestrictions").size(),is(1));
        JsonObject reportingRestriction = defendant.getJsonArray("reportingRestrictions").getJsonObject(0);
        assertThat(reportingRestriction.getString("label"),is(REPORTING_RESTRICTION));
        defendant = hearing.getJsonArray("defendants").getJsonObject(1);
        assertThat(defendant.getString("firstName"), is(FIRST_NAME1));
        assertThat(defendant.getString("surname"), is(LAST_NAME1));
        assertThat(defendant.getJsonArray("offences").size(), is(1));
        assertThat(defendant.getJsonArray("reportingRestrictions").size(),is(2));
        JsonObject reportingRestriction1 = defendant.getJsonArray("reportingRestrictions").getJsonObject(0);
        assertThat(reportingRestriction1.getString("label"),is(REPORTING_RESTRICTION));

    }

    @Test
    public void shouldBuildDataForRestrictedPublicCourtListWelshCourt() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(IS_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelopeRestrictedPublicList(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, TRUE, FALSE).get();

        assertRestrictedCasePublicCourtListPayload(publicListData);

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is(START_DATE2));
        JsonObject courtRoomsJo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME2.substring(11, 16)));
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE2));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE1));
        assertThat(hearing.getString("reportingRestrictionReason"), is(notNullValue()));
        assertThat(hearing.getJsonArray("defendants").size(), is(2));
        JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("firstName"), is(EMPTY));
        assertThat(defendant.getString("surname"), is(DEFENDANT_STRING));
        assertThat(defendant.getString("welshSurname"), is(WELSH_DEFENDANT_STRING));
        assertThat(defendant.getJsonArray("offences").size(), is(1));
        assertThat(defendant.getJsonArray("reportingRestrictions").size(),is(1));
        JsonObject reportingRestriction = defendant.getJsonArray("reportingRestrictions").getJsonObject(0);
        assertThat(reportingRestriction.getString("label"),is(REPORTING_RESTRICTION));
        defendant = hearing.getJsonArray("defendants").getJsonObject(1);
        assertThat(defendant.getString("firstName"), is(FIRST_NAME1));
        assertThat(defendant.getString("surname"), is(LAST_NAME1));
        assertThat(defendant.getString("welshSurname"), is(LAST_NAME1));
        assertThat(defendant.getJsonArray("offences").size(), is(1));
        assertThat(defendant.getJsonArray("reportingRestrictions").size(),is(2));
        JsonObject reportingRestriction1 = defendant.getJsonArray("reportingRestrictions").getJsonObject(0);
        assertThat(reportingRestriction1.getString("label"),is(REPORTING_RESTRICTION));

    }

    public void shouldNotBuildDataForHideCasePublicCourtList() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        JsonObject publicListData = publicListService.assemble(buildCaseApplicationHearingRestrictedCasePublicCourtListData(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), PUBLIC, TRUE, false).get();

        assertRestrictedCasePublicCourtListPayload(publicListData);
    }

    @Test
    public void shouldBuildOnlyApplicationHearingRestrictedCasePublicCourtListData() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());
        JsonObject publicListData = publicListService.assemble(buildOnlyApplicationHearingRestrictedCasePublicCourtListData(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, FALSE, false).get();

        assertRestrictedCasePublicCourtListPayload(publicListData);
    }

    @Test
    public void shouldBuildOnlyApplicationHearingPublicCourtListData() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());
        JsonObject publicListData = publicListService.assemble(buildOnlyApplicationHearingPublicCourtListData(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, FALSE, FALSE).get();

        assertApplicationHearingPublicCourtListPayload(publicListData);
        assertRestrictedDefendant(publicListData, false);
    }

    @Test
    public void shouldBuildOnlyApplicationHearingPublicCourtListDataWithRestrictedDefendant() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());
        JsonObject publicListData = publicListService.assemble(buildOnlyApplicationHearingPublicCourtListDataWithRestrictedDefendant(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.PUBLIC, FALSE, false).get();

        assertApplicationHearingPublicCourtListPayload(publicListData);

        assertRestrictedDefendant(publicListData, true);
    }

    @Test
    public void shouldBuildDataForPublicCourtListBST() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelopeForBST(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), PUBLIC, TRUE, FALSE).get();

        JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is("2018-07-21"));
        JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);
        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);


        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is("2018-07-21T13:37:00.000Z".substring(11, 16)));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE1));
        assertThat(hearing.getString("reportingRestrictionReason"), is(REPORTING_RESTRICTION_REASON));

        JsonObject hearingDate2Jo = publicListData.getJsonArray("hearingDates").getJsonObject(1);
        assertThat(hearingDate2Jo.getString("hearingDate"), is("2018-07-22"));
        JsonObject courtRooms2Jo = hearingDate2Jo.getJsonArray("courtRooms").getJsonObject(0);
        JsonObject timeslot2 = courtRooms2Jo.getJsonArray("timeslots").getJsonObject(0);


        JsonObject hearing2 = timeslot2.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing2.getString("startTime"), is("2018-07-22T11:30:00.000Z".substring(11, 16)));
        assertThat(hearing2.getString("hearingType"), is(HEARING_TYPE1));
        assertThat(hearing2.getString("reportingRestrictionReason"), is(REPORTING_RESTRICTION_REASON));
    }

    @Test
    public void shouldBuildDataForPublicCourtListStandAloneApplication() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");

        final JsonEnvelope envelope = buildRequestEnvelope(true, false);
        final JsonObject publicListData = publicListService.assemble(envelope, COURT_CENTRE_ID.toString(), null, PUBLIC, TRUE, false).get();

        assertPublicCourtListPayload(publicListData, CHECK_JUDICIARY);

        assertThat(publicListData.toString(), isJson(allOf(
                withJsonPath("$.hearingDates[1].hearingDate", equalTo(START_DATE2)),
                withJsonPath("$.hearingDates[1].hearingDateWelsh", equalTo(START_DATE2_WELSH)),
                withJsonPath("$.hearingDates[1].courtRooms[0].courtRoomName", equalTo(COURT_ROOM_NAME_1)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].startTime", equalTo(START_TIME2.substring(11, 16))),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].caseNumber", equalTo(CASE_REFERENCE2)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].prosecutorType", equalTo(format("%s, %s", LAST_NAME2.toUpperCase(), FIRST_NAME2))),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].hearingType", equalTo(HEARING_TYPE2)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].welshHearingType", equalTo(HEARING_TYPE2)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].defendants[0].firstName", equalTo(FIRST_NAME3)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].defendants[0].surname", equalTo(LAST_NAME3)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[0].offenceTitle", equalTo(APPLICATION_TYPE)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].defendants[0].offences[0].offenceWording", equalTo(APPLICATION_PARTICULARS)),
                withJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].applicationOffences[0].id", equalTo("785c2a46-175c-4032-ad26-4c79642048ad")),
                withoutJsonPath("$.hearingDates[1].courtRooms[0].timeslots[0].hearings[0].applicationOffences[0].listingNumber")
        )));
        verify(judiciaryNameMapper, times(2)).getName(argumentCaptor.capture());
        final JsonObject judiciary = argumentCaptor.getValue();
        assertThat(judiciary, is(notNullValue()));
    }

    @Test
    public void shouldBuildDataForPublicCourtListWhenJudiciaryIsEmpty() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(NOT_WELSH));

        JsonObject publicListData = publicListService.assemble(buildRequestEnvelopeWithNoJudiciary(), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), PUBLIC, TRUE, false).get();
        assertPublicCourtListPayload(publicListData, DONT_CHECK_JUDICIARY);
    }

    @Test
    public void shouldBuildDataForWelshPublicCourtList() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails(IS_WELSH));
        when(referenceDataService.getJudiciariesByIdList(eq(singletonList(JUDICIARY_ID)), any(JsonEnvelope.class)))
                .thenReturn(generateJudiciaryEnvelope());

        final JsonEnvelope envelope = buildRequestEnvelope();
        final JsonObject publicListData = publicListService.assemble(envelope, COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), PUBLIC, TRUE, false).get();

        assertWelshPublicCourtListPayload(publicListData);
    }

    private void assertWelshPublicCourtListPayload(JsonObject publicListData) {
        assertThat(publicListData.getString("listType"), is(PUBLIC.toString().toLowerCase()));
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
        assertThat(hearing.getString("welshHearingType"), is(HEARING_TYPE1)); //waiting for welsh hearingType description to supplied as part of listCourtHearing in 2.5
        assertThat(hearing.getString("welshReportingRestrictionReason"), is(WELSH + REPORTING_RESTRICTION_REASON));

        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE));

        JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("firstName"), is(FIRST_NAME1));
        assertThat(defendant.getString("surname"), is(LAST_NAME1));

        JsonObject offence = defendant.getJsonArray("offences").getJsonObject(0);
        assertThat(offence.getString("welshOffenceTitle"), is(WELSH + OFFENCE_TITLE));
    }


    private void assertPublicCourtListPayload(JsonObject publicListData, boolean checkJudiciary) {
        assertThat(publicListData.getString("listType"), is(PUBLIC.toString().toLowerCase()));
        assertThat(publicListData.getString("courtCentreName"), is(COURT_CENTRE_NAME));
        assertThat(publicListData.getString("courtCentreAddress1"), is(ADDRESS_1 + SPACE + ADDRESS_2));
        assertThat(publicListData.getString("courtCentreAddress2"), is(ADDRESS_3 + SPACE + ADDRESS_4 + SPACE + ADDRESS_5 + SPACE + POSTCODE));

        final JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is(START_DATE1));

        final JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);
        if (checkJudiciary) {
            assertThat(courtRoomsJo.getString("judiciaryNames"), is("Mr Recorder Ainsworth suffix"));
        }

        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));

        final JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);

        final JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME1.substring(11, 16)));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE1));
        assertThat(hearing.getString("reportingRestrictionReason"), is(REPORTING_RESTRICTION_REASON));


        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE));

        final JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("firstName"), is(FIRST_NAME1));
        assertThat(defendant.getString("surname"), is(LAST_NAME1));

        final JsonObject offence = defendant.getJsonArray("offences").getJsonObject(0);
        assertThat(offence.getString("offenceTitle"), is(OFFENCE_TITLE));
    }

    private void assertBulkPublicCourtListPayload(JsonObject publicListData, boolean checkJudiciary) {
        assertThat(publicListData.getString("listType"), is(PUBLIC.toString().toLowerCase()));
        assertThat(publicListData.getString("courtCentreName"), is(COURT_CENTRE_NAME));
        assertThat(publicListData.getString("courtCentreAddress1"), is(ADDRESS_1 + SPACE + ADDRESS_2));
        assertThat(publicListData.getString("courtCentreAddress2"), is(ADDRESS_3 + SPACE + ADDRESS_4 + SPACE + ADDRESS_5 + SPACE + POSTCODE));

        final JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is(START_DATE1));

        final JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);
        if (checkJudiciary) {
            assertThat(courtRoomsJo.getString("judiciaryNames"), is("Mr Recorder Ainsworth suffix"));
        }

        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));

        final JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);

        final JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getString("startTime"), is(START_TIME1.substring(11, 16)));
        assertThat(hearing.getString("hearingType"), is(HEARING_TYPE1));
        assertThat(hearing.getString("reportingRestrictionReason"), is(REPORTING_RESTRICTION_REASON));


        assertThat(hearing.getString("caseNumber"), is(""));

        final JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("surname"), is("1000 Defendants"));

        final JsonObject offence = defendant.getJsonArray("offences").getJsonObject(0);
        assertThat(offence.getString("offenceTitle"), is(OFFENCE_TITLE));
    }

    private void assertRestrictedCasePublicCourtListPayload(JsonObject publicListData) {
        assertThat(publicListData.getString("listType"), is(PUBLIC.toString().toLowerCase()));
        assertThat(publicListData.getString("courtCentreName"), is(COURT_CENTRE_NAME));
        assertThat(publicListData.getString("courtCentreAddress1"), is(ADDRESS_1 + SPACE + ADDRESS_2));
        assertThat(publicListData.getString("courtCentreAddress2"), is(ADDRESS_3 + SPACE + ADDRESS_4 + SPACE + ADDRESS_5 + SPACE + POSTCODE));

        JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        assertThat(hearingDateJo.getString("hearingDate"), is(START_DATE1));

        JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);

        assertThat(courtRoomsJo.getString("courtRoomName"), is(COURT_ROOM_NAME_1));

        assertThat(courtRoomsJo.getJsonArray("timeslots"), Matchers.empty());
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
                .replace("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"");
        return convertToJsonObject(jsonString);
    }

    private JsonEnvelope buildOnlyApplicationHearingPublicCourtListData() {
        String jsonString = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getOnlyApplicationHearingPublicCourtListData.json")
                .replace("JUDICIARY_ID", JUDICIARY_ID.toString());;
        return buildJsonEnvelope(convertToJsonObject(jsonString));
    }

    private JsonEnvelope buildOnlyApplicationHearingPublicCourtListDataWithRestrictedDefendant() {
        String jsonString = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getOnlyApplicationHearingPublicCourtListDataWithDefendantRestricted.json")
                .replace("JUDICIARY_ID", JUDICIARY_ID.toString());;
        return buildJsonEnvelope(convertToJsonObject(jsonString));
    }

    private JsonEnvelope buildOnlyApplicationHearingRestrictedCasePublicCourtListData() {
        String jsonString = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getOnlyApplicationHearingRestrictedCasePublicCourtListData.json")
                .replace("JUDICIARY_ID", JUDICIARY_ID.toString());;
        return buildJsonEnvelope(convertToJsonObject(jsonString));
    }

    private JsonEnvelope buildCaseApplicationHearingRestrictedCasePublicCourtListData() {
        String jsonString = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCaseApplicationHearingRestrictedCasePublicCourtListData.json")
                .replace("JUDICIARY_ID", JUDICIARY_ID.toString());;
        return buildJsonEnvelope(convertToJsonObject(jsonString));
    }

    private void assertApplicationHearingPublicCourtListPayload(JsonObject publicListData) {
        assertThat(publicListData.getString("listType"), is(PUBLIC.toString().toLowerCase()));
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
        assertThat(hearing.getString("caseNumber"), is(CASE_REFERENCE));
    }

    private void assertRestrictedDefendant(JsonObject publicListData, boolean isDefendantRestricted) {

        JsonObject hearingDateJo = publicListData.getJsonArray("hearingDates").getJsonObject(0);
        JsonObject courtRoomsJo = hearingDateJo.getJsonArray("courtRooms").getJsonObject(0);

        JsonObject timeslot = courtRoomsJo.getJsonArray("timeslots").getJsonObject(0);
        JsonObject hearing = timeslot.getJsonArray("hearings").getJsonObject(0);
        JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        if(isDefendantRestricted){
            assertThat(defendant.getString("firstName"), is(EMPTY));
            assertThat(defendant.getString("surname"), is(DEFENDANT_STRING));
        } else{
            assertThat(defendant.getString("firstName"), is(FIRST_NAME1));
            assertThat(defendant.getString("surname"), is(LAST_NAME1));
        }
    }

    private JsonEnvelope buildRequestEnvelope() {
        return buildRequestEnvelope(false, false);
    }

    private JsonEnvelope buildBulkRequestEnvelope() {
        return buildBulkRequestEnvelope(false, false);
    }

    private JsonEnvelope buildRequestEnvelopeRestrictedPublicList() {
        final JsonObject queryPayload = createRestrictedRequestPayload();
        return buildJsonEnvelope(queryPayload);
    }

    private JsonEnvelope buildRequestEnvelope(final boolean withStandAloneApplication, final boolean withLegalEntityDefendant) {
        if (withStandAloneApplication) {
            return buildJsonEnvelope(createRequestPayloadWithStandAloneApplication());
        } else if (withLegalEntityDefendant) {
            return buildJsonEnvelope(createRequestPayloadWithLegalEntityDefendant());
        } else {
            return buildJsonEnvelope(createRequestPayload());
        }
    }

    private JsonEnvelope buildBulkRequestEnvelope(final boolean withStandAloneApplication, final boolean withLegalEntityDefendant) {
        if (withStandAloneApplication) {
            return buildJsonEnvelope(createRequestPayloadWithStandAloneApplication());
        } else if (withLegalEntityDefendant) {
            return buildJsonEnvelope(createRequestPayloadWithLegalEntityDefendant());
        } else {
            return buildJsonEnvelope(createBulkRequestPayload());
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
                .replaceAll("HEARING_TYPE1", HEARING_TYPE1)
                .replaceAll("CASE_REFERENCE1", CASE_REFERENCE)
                .replaceAll("CASE_REFERENCE2", CASE_REFERENCE2)
                .replaceAll("FIRST_NAME1", FIRST_NAME1)
                .replaceAll("LAST_NAME1", LAST_NAME1)
                .replaceAll("OFFENCE_TITLE", OFFENCE_TITLE)
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"");
        JsonObject queryPayload = convertToJsonObject(path);

        return buildJsonEnvelope(queryPayload);
    }

    private JsonObject createRequestPayloadWithLegalEntityDefendant() {
        String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentForPublicList-LegalEntityDefendant.json")
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"");
        return convertToJsonObject(payload);
    }

    private JsonObject createRequestPayloadWithStandAloneApplication() {
        final String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentForPublicList-StandaloneApplication.json")
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"");
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
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"");
        return convertToJsonObject(payload);
    }

    private JsonObject createBulkRequestPayload() {
        String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentForBulkPublicList.json")
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString());
        return convertToJsonObject(payload);
    }

    private JsonObject createRestrictedRequestPayload() {
        String payload = getFileContentWithCommonFieldsReplaced("restrict/stubbed.restrictedCourtListContentForPublicList.json")
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"");
        return convertToJsonObject(payload);
    }

    private JsonObject createRequestPayloadWithNoJudiciary() {
        String payload = getFileContentWithCommonFieldsReplaced("stubbed.queryView.getCourtListContentWithNoJudiciary.json");
        return convertToJsonObject(payload);
    }

    private String getFileContentWithCommonFieldsReplaced(final String fileName) {
        return FileUtil.getPayload(fileName)
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("START_DATE1", START_DATE1)
                .replaceAll("START_DATE2", START_DATE2)
                .replaceAll("START_TIME1", START_TIME1)
                .replaceAll("START_TIME2", START_TIME2)
                .replaceAll("REPORTING_RESTRICTION_REASON", REPORTING_RESTRICTION_REASON)
                .replaceAll("HEARING_TYPE1", HEARING_TYPE1)
                .replaceAll("CASE_REFERENCE1", CASE_REFERENCE)
                .replaceAll("CASE_REFERENCE2", CASE_REFERENCE2)
                .replaceAll("FIRST_NAME1", FIRST_NAME1)
                .replaceAll("LAST_NAME1", LAST_NAME1)
                .replaceAll("OFFENCE_TITLE", OFFENCE_TITLE)
                .replaceAll("ADDRESS_1", ADDRESS_1)
                .replaceAll("ADDRESS_2", ADDRESS_2)
                .replaceAll("ADDRESS_3", ADDRESS_3)
                .replaceAll("ADDRESS_4", ADDRESS_4)
                .replaceAll("ADDRESS_5", ADDRESS_5)
                .replaceAll("POSTCODE", POSTCODE)
                .replaceAll("APPLICATION_TYPE", APPLICATION_TYPE)
                .replaceAll("APPLICATION_PARTICULARS", APPLICATION_PARTICULARS)
                .replaceAll("HEARING_TYPE2", HEARING_TYPE2)
                .replaceAll("FIRST_NAME2", FIRST_NAME2)
                .replaceAll("LAST_NAME2", LAST_NAME2)
                .replaceAll("FIRST_NAME3", FIRST_NAME3)
                .replaceAll("LAST_NAME3", LAST_NAME3);
    }

    private JsonObject convertToJsonObject(final String jsonString) {
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }
}
