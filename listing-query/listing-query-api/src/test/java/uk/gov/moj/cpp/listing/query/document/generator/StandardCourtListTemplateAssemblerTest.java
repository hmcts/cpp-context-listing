package uk.gov.moj.cpp.listing.query.document.generator;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.moj.cpp.listing.domain.CourtListType.BENCH;
import static uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails.courtCentreDetails;
import static uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails.courtRoomDetails;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.utils.ZonedDateTimeFormatter;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.CourtRoom;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Defendant;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Hearing;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.HearingDate;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Offence;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.StandardCourtList;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Timeslot;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.MapUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class )
public class StandardCourtListTemplateAssemblerTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String START_DATE = "2019-01-29";
    private static final String COURT_CENTRE_NAME = STRING.next();
    private static final String WELSH = "Welsh";
    private static final String ADDRESS_1 = STRING.next();
    private static final String ADDRESS_2 = STRING.next();
    private static final String ADDRESS_3 = STRING.next();
    private static final String ADDRESS_4 = STRING.next();
    private static final String ADDRESS_5 = STRING.next();
    private static final String POSTCODE = STRING.next();
    private static final UUID COURT_ROOM_1_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_2_ID = UUID.randomUUID();
    private static final String COURT_ROOM_NAME_1 = "Room 1";
    private static final String COURT_ROOM_NAME_2 = "Room 2";
    private static final UUID JUDICIARY_ID = UUID.randomUUID();
    private static final UUID JUDICIARY2_ID = UUID.randomUUID();
    private static final UUID JUDICIARY3_ID = UUID.randomUUID();
    private static final String QUERY_ACTION_NAME = "listing.public.list";
    private static final String START_TIME = "2019-01-29T10:30:00.000Z";
    private static final String START_TIME2 = "2019-01-29T12:37:00.000Z";
    private static final LocalTime START_LOCAL_TIME = ZonedDateTime.parse(START_TIME).toLocalTime();
    private static final LocalTime START_LOCAL_TIME2 = ZonedDateTime.parse(START_TIME2).toLocalTime();
    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final String DATE_OF_BIRTH = "1983-05-23";
    private static final int SEQUENCE_1 = 1;
    private static final String ORGANISATION_NAME = "organisationName";
    private static final int SEQUENCE_2 = 2;
    private static final String PTP = "Plea & Trial Preparation";
    private static final String CPS = "CPS";
    private static final String DEFENDANT_STRING = "Defendant";
    private static final UUID CASE_ID1 = UUID.randomUUID();
    private static final UUID CASE_ID2 = UUID.randomUUID();
    private static final UUID DEFENDANT_ID1 = UUID.randomUUID();
    private static final UUID DEFENDANT_ID2 = UUID.randomUUID();
    private static final UUID DEFENDANT_ID3 = UUID.randomUUID();


    private static final String TOP_LEVEL_COURT_CENTRE_ID = randomUUID().toString();
    private static final String TOP_LEVEL_COURT_ROOM_ID = randomUUID().toString();
    private static final String OTHER_COURT_CENTRE_ID = randomUUID().toString();
    private static final String OTHER_COURT_ROOM_ID = randomUUID().toString();
    private static final ZoneId UK_TIME = ZoneId.of("Europe/London");
    private static final String HH_MM = "HH:mm";
    private static final String FIRST_NAME_1 = "James";
    private static final String LAST_NAME_1 = "Thomas";
    private static final String FIRST_NAME_2 = "Erica";
    private static final String LAST_NAME_2 = "Larsen";
    private static final String FIRST_NAME_3 = "Lindsey";
    private static final String LAST_NAME_3 = "Craft";
    private static final String CASE_URN_1 = STRING.next();
    private static final String CASE_URN_2 = STRING.next();
    private static final String ORGANISATION_NAME_1 = "Barker and Sons";
    private static final String QUERY_NAME = "listing.search.court.list";
    private static final LocalDate HEARING_DATE = LocalDates.from("2018-11-21");
    private final UUID COURT_ROOM_ID = randomUUID();
    private static final String COURT_ROOM_NAME = "Room 3";
    private static final String COURT_ROOM_NAME_WELSH = "Room 3 Welsh";
    private static final String COURT_CENTRE_NAME_1 = "Liverpool Crown Court";
    private static final String COURT_CENTRE_NAME_WELSH = "Liverpool Crown Court Welsh Name";
    private static final String ADDRESS_01 = "22 Liverpool Street";
    private static final String POST_CODE = "LV12 9XA";
    private static final String ADDRESS_1_WELSH = "22 Welsh Street";
    private static final String REPORTING_RESTRICTION="RestrictionApplied";

    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Mock
    private JudiciaryNameMapper judiciaryNameMapper;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private StandardPublicCourtListTemplateAssembler assembler;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Test
    public void shouldAssembleDataForStandardAndBenchCourtListTemplateWithNullCourtRoomId() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());

        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingData()), COURT_CENTRE_ID.toString(), null, courtListType, FALSE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        //verify prosecutor is picked for prosecutorType if prosecutor is present
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(0).getHearings().get(1).getProsecutorType(), is("TFL"));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(2));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertCourtRoom(actualCourtRoom, 2);

        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(2));
        Hearing actualHearing = actualTimeslot.getHearings().get(0);
        assertHearing(actualHearing);

        Defendant actualDefendant = actualHearing.getDefendants().get(0);
        assertDefendant(actualDefendant,2);
        assertOffence(actualDefendant.getOffences().get(0));

        assertThat(actualTimeslot.getHearings().get(0).getSequence(), lessThan(actualTimeslot.getHearings().get(1).getSequence()));

        Timeslot actualTimeslot2 = actualCourtRoom.getTimeslots().get(1);
        assertThat(actualTimeslot2.getHearings().size(), is(1));

        assertThat(actualTimeslot2.getHearings().get(0).getStartTime(), is(START_LOCAL_TIME2.toString()));

        CourtRoom actualCourtRoom2 = actualHearingDate.getCourtRooms().get(1);
        assertThat(actualCourtRoom2.getCourtRoomName(), is(COURT_ROOM_NAME_2));
    }

    // Warning - this suppression of warnings is solely to get sonar to pass so the junit5
    // upgrade can be merged. The @SuppressWarnings needs to be removed and the test refactored
    @SuppressWarnings("java:S5961")
    @Test
    public void shouldAssembleDataForStandardAndBenchCourtListTemplateWithCourtRoomId() {
        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataForBenchList()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, FALSE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertCourtRoom(actualCourtRoom, 1);

        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(2));
        Hearing actualHearing = actualTimeslot.getHearings().get(0);
        assertHearing(actualHearing);

        Defendant actualDefendant = actualHearing.getDefendants().get(0);
        assertDefendant(actualDefendant,2);
        assertOffence(actualDefendant.getOffences().get(0));

        CourtRoom actualCourtRoom1 = actualHearingDate.getCourtRooms().get(0);
        assertThat(actualCourtRoom1.getCourtRoomName(), is(COURT_ROOM_NAME_1));

        List<String> caseNumbers = asList("42GD8525719", "42GD8525720");

        final List<Defendant> defendants = actualCourtList.getHearingDates().stream()
                .map(hd -> hd.getCourtRooms())
                .flatMap(List::stream)
                .collect(Collectors.toList())
                .stream()
                .map(cr -> cr.getTimeslots())
                .flatMap(List::stream)
                .collect(Collectors.toList())
                .stream()
                .map(c -> c.getHearings())
                .flatMap(List::stream)
                .collect(Collectors.toList()).stream().filter(h -> caseNumbers.contains(h.getCaseNumber()))
                .map(Hearing::getDefendants).flatMap(List::stream).collect(Collectors.toList());


        if(courtListType == BENCH) {
            //Verify Defence Counsel
            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Cheyanne"))
                    .map(d -> d.getDefenceCounsels()).flatMap(List::stream).collect(Collectors.toList()).stream()
                    .anyMatch(dc -> (dc.getFirstName().equals("Rosy") && dc.getLastName().equals("Mary"))), is(true));

            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Rodger"))
                    .map(d -> d.getDefenceCounsels()).flatMap(List::stream).collect(Collectors.toList()).isEmpty(), is(true));

            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Mayyen"))
                    .map(d -> d.getDefenceCounsels()).flatMap(List::stream).collect(Collectors.toList()).stream()
                    .anyMatch(dc -> (dc.getFirstName().equals("Sam") && dc.getLastName().equals("Lory"))), is(true));

            //Verify Prosecution Case
            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Cheyanne"))
                    .map(d -> d.getProsecutionCounsels()).flatMap(List::stream).collect(Collectors.toList()).stream()
                    .anyMatch(dc -> (dc.getFirstName().equals("John") && dc.getLastName().equals("Turkey"))), is(true));

            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Rodger"))
                    .map(d -> d.getProsecutionCounsels()).flatMap(List::stream).collect(Collectors.toList()).stream()
                    .anyMatch(dc -> (dc.getFirstName().equals("John") && dc.getLastName().equals("Turkey"))), is(true));

            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Mayyen"))
                    .map(d -> d.getProsecutionCounsels()).flatMap(List::stream).collect(Collectors.toList()).stream()
                    .anyMatch(dc -> (dc.getFirstName().equals("Barry") && dc.getLastName().equals("tony"))), is(true));
        } else {
            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Cheyanne")).collect(Collectors.toList()).get(0).getDefenceCounsels(), nullValue());
            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Rodger")).collect(Collectors.toList()).get(0).getDefenceCounsels(), nullValue());
            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Mayyen")).collect(Collectors.toList()).get(0).getDefenceCounsels(), nullValue());

            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Cheyanne")).collect(Collectors.toList()).get(0).getProsecutionCounsels(), nullValue());
            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Rodger")).collect(Collectors.toList()).get(0).getProsecutionCounsels(), nullValue());
            assertThat(defendants.stream().filter(d -> d.getFirstName().equals("Mayyen")).collect(Collectors.toList()).get(0).getProsecutionCounsels(), nullValue());

        }

    }

    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWithCourtRoomId() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingData()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, FALSE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertCourtRoom(actualCourtRoom, 2);

        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(2));
        Hearing actualHearing = actualTimeslot.getHearings().get(0);
        assertHearing(actualHearing);

        Defendant actualDefendant = actualHearing.getDefendants().get(0);
        assertDefendant(actualDefendant,2);
        assertOffence(actualDefendant.getOffences().get(0));

        assertThat(actualTimeslot.getHearings().get(0).getSequence(), lessThan(actualTimeslot.getHearings().get(1).getSequence()));

        Timeslot actualTimeslot2 = actualCourtRoom.getTimeslots().get(1);
        assertThat(actualTimeslot2.getHearings().size(), is(1));

        assertThat(actualTimeslot2.getHearings().get(0).getStartTime(), is(START_LOCAL_TIME2.toString()));

        CourtRoom actualCourtRoom1 = actualHearingDate.getCourtRooms().get(0);
        assertThat(actualCourtRoom1.getCourtRoomName(), is(COURT_ROOM_NAME_1));
    }

    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWithOneShadowListedOffence() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildCourtListWithOneShadowListedOffence()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);

        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(1));
        Hearing actualHearing = actualTimeslot.getHearings().get(0);

        Defendant actualDefendant = actualHearing.getDefendants().get(0);
        assertDefendant(actualDefendant,1);
        assertOffence(actualDefendant.getOffences().get(0));

        assertThat(actualDefendant.getReportingRestrictions().size(), is(1));
        assertThat(actualDefendant.getReportingRestrictions().stream().findFirst().get().getLabel(), is(REPORTING_RESTRICTION));

        CourtRoom actualCourtRoom1 = actualHearingDate.getCourtRooms().get(0);
        assertThat(actualCourtRoom1.getCourtRoomName(), is(COURT_ROOM_NAME_1));
    }

    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWhenThereIsNoJudiciary() {
        final CourtListType courtListType = BENCH;
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());

        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataForNoJudiciary()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, FALSE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);

        assertThat(actualHearingDate.getCourtRooms().size(), is(1));

        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertThat(actualCourtRoom.getTimeslots().size(), is(1));


        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(1));
    }

    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWithApplication() {
        final CourtListType courtListType = BENCH;
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());

        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataForNoJudiciaryWithApplication()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, FALSE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);

        assertThat(actualHearingDate.getCourtRooms().size(), is(1));

        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertThat(actualCourtRoom.getTimeslots().size(), is(1));

        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(2));
    }

    @Test
    public void shouldAssembleDataForRestrictedCaseStandardCourtListTemplateWithCourtRoomId() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataRestrictedCase()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);

        Timeslot actualTimeSlot = actualCourtRoom.getTimeslots().get(0);
        Hearing actualHearing = actualTimeSlot.getHearings().get(0);
        assertThat(actualHearing.getDefendants().size(), is(2));
        assertThat(actualHearing.getProsecutorType(), is("CPS"));
        assertThat(actualHearing.getReportingRestrictionReason(), is(EMPTY));
        assertThat(actualHearing.getStartTime(), is(START_LOCAL_TIME.toString()));
        assertThat(actualHearing.getHearingType(), is("Plea & Trial Preparation"));
        assertThat(actualHearing.getSequence(), is(SEQUENCE_2));
    }

    @Test
    public void shouldAssembleDataForRestrictedDefendantStandardCourtListTemplateWithCourtRoomId() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataRestrictedDefendant()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);

        Timeslot actualTimeSlot = actualCourtRoom.getTimeslots().get(0);
        Hearing actualHearing = actualTimeSlot.getHearings().get(0);
        assertThat(actualHearing.getDefendants().size(), is(2));
        assertThat(actualHearing.getCaseNumber(), is(notNullValue()));
        assertThat(actualHearing.getProsecutorType(), is(CPS));
        assertThat(actualHearing.getStartTime(), is(START_LOCAL_TIME.toString()));
        assertThat(actualHearing.getHearingType(), is(PTP));
        assertThat(actualHearing.getSequence(), is(SEQUENCE_2));
    }

    @Test
    public void shouldAssembleDataForRestrictedMultipleDefendantStandardCourtListTemplateWithCourtRoomId() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataRestrictedMultipleDefendant()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);

        Timeslot actualTimeSlot = actualCourtRoom.getTimeslots().get(0);
        Hearing actualHearing = actualTimeSlot.getHearings().get(0);
        assertThat(actualHearing.getDefendants().size(), is(2));
        assertThat(actualHearing.getCaseNumber(), is(notNullValue()));
        assertThat(actualHearing.getProsecutorType(), is(CPS));
        assertThat(actualHearing.getStartTime(), is(START_LOCAL_TIME.toString()));
        assertThat(actualHearing.getHearingType(), is(PTP));
        assertThat(actualHearing.getSequence(), is(SEQUENCE_2));
    }

    @Test
    public void shouldAssembleDataForRestrictedOffenceStandardCourtListTemplate() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataRestrictedOffence()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);

        Timeslot actualTimeSlot = actualCourtRoom.getTimeslots().get(0);
        Hearing actualHearing = actualTimeSlot.getHearings().get(0);
        assertThat(actualHearing.getDefendants().size(), is(2));
        assertThat(actualHearing.getCaseNumber(), is(notNullValue()));
        assertThat(actualHearing.getProsecutorType(), is(CPS));
        assertThat(actualHearing.getStartTime(), is(START_LOCAL_TIME.toString()));
        assertThat(actualHearing.getHearingType(), is(PTP));
        assertThat(actualHearing.getSequence(), is(SEQUENCE_2));
        assertThat(actualHearing.getDefendants().get(0).getOffences().size(), is(2));
        assertThat(actualHearing.getDefendants().get(1).getOffences().size(), is(2));
    }

    private void assertRestrictedDefendant(Defendant defendant, String nameString, int offenceCount) {
        assertThat(defendant.getSurname(), is(nameString));
        assertThat(defendant.getFirstName(), is(EMPTY));
        assertThat(defendant.getAge(), is(EMPTY));
        assertThat(defendant.getDateOfBirth(), is(EMPTY));
        assertThat(defendant.getOffences().size(), is(offenceCount));
    }

    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWithLegalEntityDefendant() {

        final CourtListType courtListType = BENCH;
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());

        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildCourtListWithLegalEntityDefendant()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, FALSE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        Defendant actualDefendant = actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(0).getHearings().get(0).getDefendants().get(0);
        assertThat(actualDefendant.getOrganisationName(), is(notNullValue()));
        assertThat(actualDefendant.getOrganisationName(), is(ORGANISATION_NAME));
    }

    @Test
    public void shouldAssembleDataForRestrictedCourtListTemplateWithLegalEntityDefendant() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildRestrictedCourtListWithLegalEntityDefendant()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));

        Defendant actualDefendant1 = actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(0).getHearings().get(0).getDefendants().get(0);
        Defendant actualDefendant2 = actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(0).getHearings().get(0).getDefendants().get(1);

        assertThat(actualDefendant1.getOrganisationName(), is(notNullValue()));
        assertThat(actualDefendant2.getOrganisationName(), is(ORGANISATION_NAME));

    }

    @Test
    public void shouldAssembleDataForShadowedAndRestrictedCaseStandardCourtListTemplateWithCourtRoomId() {

        final CourtListType courtListType = BENCH;
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn("Mr Recorder Ainsworth suffix");
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataShadowedAndRestrictedCase()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), courtListType, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(courtListType.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);

        assertThat(actualCourtRoom.getTimeslots().size(), is(0));
    }

    @Test
    public void shouldAssembleDataForPublicCourtListForHearingsWithMultipleCourtCentres_1() {
        final LocalDate date = LocalDate.now(UTC);
        final LocalTime time = LocalTime.now(UTC);

        doReturn(getCourtCentreDetails(false, fromString(TOP_LEVEL_COURT_CENTRE_ID), fromString(TOP_LEVEL_COURT_ROOM_ID)))
                .when(courtCentreFactory).getCourtCentre(eq(fromString(TOP_LEVEL_COURT_CENTRE_ID)), any(JsonEnvelope.class));
        doReturn(generateJsonEnvelope()).when(referenceDataService).getJudiciariesByIdList(anyList(), any(JsonEnvelope.class));
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelopeForHearingWithMultipleCourtCentres(date, time, fromString(TOP_LEVEL_COURT_CENTRE_ID)), TOP_LEVEL_COURT_CENTRE_ID, TOP_LEVEL_COURT_ROOM_ID, CourtListType.STANDARD, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(CourtListType.STANDARD.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME_1));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(ADDRESS_01 + SPACE));
        assertThat(actualCourtList.getHearingDates().size(), is(1));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().size(), is(1));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().size(), is(2));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(0).getHearings().size(), is(2));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(1).getHearings().size(), is(2));

    }

    @Test
    public void shouldAssembleDataForPublicCourtListForHearingsWithMultipleCourtCentres_2() {
        final LocalDate date = LocalDate.now(UTC);
        final LocalTime time = LocalTime.now(UTC);
        final ZonedDateTime hearingDateTime = ZonedDateTimeFormatter.adjustDateTime(ZonedDateTime.of(date, time, UK_TIME));

        doReturn(getCourtCentreDetails(false, fromString(OTHER_COURT_CENTRE_ID), fromString(OTHER_COURT_ROOM_ID)))
                .when(courtCentreFactory).getCourtCentre(eq(fromString(OTHER_COURT_CENTRE_ID)), any(JsonEnvelope.class));
        doReturn(generateJsonEnvelope()).when(referenceDataService).getJudiciariesByIdList(anyList(), any(JsonEnvelope.class));
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelopeForHearingWithMultipleCourtCentres(date, time, fromString(OTHER_COURT_CENTRE_ID)), OTHER_COURT_CENTRE_ID, OTHER_COURT_ROOM_ID, CourtListType.STANDARD, TRUE);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(CourtListType.STANDARD.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME_1));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(ADDRESS_01 + SPACE));
        assertThat(actualCourtList.getHearingDates().size(), is(1));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().size(), is(1));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().size(), is(1));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(0).getHearings().size(), is(2));
        assertThat(actualCourtList.getHearingDates().get(0).getCourtRooms().get(0).getTimeslots().get(0).getHearings().get(0).getStartTime(), is(hearingDateTime.toLocalTime().plusMinutes(30).format(DateTimeFormatter.ofPattern(HH_MM))));
    }

    private void assertCourtRoom(final CourtRoom actualCourtRoom, final int numberOfTimeSlots) {
        assertThat(actualCourtRoom.getCourtRoomName(), is(COURT_ROOM_NAME_1));
        assertThat(actualCourtRoom.getJudiciaryNames(), CoreMatchers.containsString("Ainsworth"));

        assertThat(actualCourtRoom.getTimeslots().size(), is(numberOfTimeSlots));

    }

    private void assertHearing(Hearing actualHearing) {
        assertThat(actualHearing.getCaseNumber(), is(notNullValue()));
        assertThat(actualHearing.getCaseId(), anyOf(is(CASE_ID1), is(CASE_ID2), is(fromString("6bfc68d4-5748-47de-9f7f-d7713c025d1a"))));
        assertThat(actualHearing.getHearingType(), is(notNullValue()));
        assertThat(actualHearing.getProsecutorType(), is(notNullValue()));
        assertThat(actualHearing.getSequence(), is(SEQUENCE_1));
        assertThat(actualHearing.getStartTime(), is(START_LOCAL_TIME.toString()));
        assertThat(actualHearing.getDefendants().size(), is(2));
        assertThat(actualHearing.getId().toString(), anyOf(is("2b784cde-aec2-4998-b556-11f728cd13e5"), is("4c034f6c-bca3-4fe4-a5b3-9cdb0a25837b")));
        assertThat(actualHearing.getPanel(), is("ADULT"));
    }

    private void assertDefendant(Defendant actualDefendant,int offencesCount) {
        assertThat(actualDefendant.getDateOfBirth(), is(LocalDate.parse(DATE_OF_BIRTH).format(DOB_FORMATTER)));
        assertThat(actualDefendant.getFirstName(), is(notNullValue()));
        assertThat(actualDefendant.getSurname(), is(notNullValue()));
        assertThat(actualDefendant.getAge(), is(notNullValue()));
        assertThat(actualDefendant.getOffences().size(), is(offencesCount));
        assertThat(actualDefendant.getId(), anyOf(is(DEFENDANT_ID1), is(DEFENDANT_ID2), is(DEFENDANT_ID3), is(fromString("b5e9200e-6b2f-4197-95f1-1ccf578c599f")), is(fromString("6d98fd20-e994-477f-bc75-bb68ca3b29ca")) ));
    }

    private void assertOffence(Offence actualOffence) {
        assertThat(actualOffence.getOffenceTitle(), is(notNullValue()));
        assertThat(actualOffence.getOffenceWording(), is(notNullValue()));
        assertThat(actualOffence.getId().toString(), anyOf(is("b5bfc980-2efb-4eb3-83af-033a77694f51"), is("09844038-4709-49c1-9c4f-2300c9be7b32"), is("77d5da0e-4d97-4668-8933-780e38b9dd98")));
    }


    private CourtCentreDetails generateCourtCentreDetails() {
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
                .withWelsh(false)
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

    private JsonEnvelope generateJsonEnvelope() {
        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_ACTION_NAME)
                        .withUserId(UUID.randomUUID().toString())
                        .build(),
                buildJudiciaryListData());
    }


    private JsonObject returnAsJsonObject(final String jsonString) {
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildJudiciaryListData() {
        String jsonString = FileUtil.getPayload("stubbed.referenceData.getJudiciariesByIdList-StandardListScenario.json")
                .replace("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replace("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replace("JUDICIARY3_ID", JUDICIARY3_ID.toString());
        return returnAsJsonObject(jsonString);
    }


    private JsonArray buildHearingData() {
        String jsonString = FileUtil.getPayload("stubbed.hearingRepository.findHearingsForPublicList-StandardListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildHearingDataForBenchList() {
        String jsonString = FileUtil.getPayload("stubbed.hearingRepository.findHearingsForBenchListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("DEFENDANT_ID1", DEFENDANT_ID1.toString())
                .replaceAll("DEFENDANT_ID2", DEFENDANT_ID2.toString())
                .replaceAll("DEFENDANT_ID3", DEFENDANT_ID3.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }


    private JsonArray buildHearingDataForNoJudiciary() {
        String jsonString = FileUtil.getPayload("stubbed.hearingRepository.findHearingsForPublicList-StandardList-NoJudiciaryScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildHearingDataForNoJudiciaryWithApplication() {
        String jsonString = FileUtil.getPayload("stubbed.hearingRepository.findHearingsForPublicList-StandardList-StandaloneApplication.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildHearingDataRestrictedMultipleDefendant() {
        String jsonString = FileUtil.getPayload("restrict/stubbed.restrictedMultipleDefendantForPublicStandardListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildHearingDataRestrictedOffence() {
        String jsonString = FileUtil.getPayload("restrict/stubbed.restrictedOffenceForPublicStandardListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildCourtListWithLegalEntityDefendant() {
        String jsonString = FileUtil.getPayload("stubbed.queryView.getCourtListContentForStandardList-LegalEntityDefendant.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildCourtListWithOneShadowListedOffence() {
        String jsonString = FileUtil.getPayload("restrict/stubbed.shadowListedandrestrictedOffenceForStandardListScenario.json")
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID",JUDICIARY3_ID.toString())
                .replaceAll("COURT_CENTRE_ID",COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID",COURT_ROOM_1_ID.toString())
                .replaceAll("DATE_OF_BIRTH",DATE_OF_BIRTH);
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildRestrictedCourtListWithLegalEntityDefendant() {
        String jsonString = FileUtil.getPayload("restrict/stubbed.restrictedLegalEntityDefendantForPublicStandardListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildHearingDataShadowedAndRestrictedCase() {

        String jsonString = FileUtil.getPayload("restrict/stubbed.shadowListedAndRestrictedCasesForPublicStandardListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildHearingDataRestrictedCase() {
        String jsonString = FileUtil.getPayload("restrict/stubbed.restrictedCasesForPublicStandardListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonArray buildHearingDataRestrictedDefendant() {
        String jsonString = FileUtil.getPayload("restrict/stubbed.restrictedDefendantForPublicStandardListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("JUDICIARY2_ID", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY3_ID", JUDICIARY3_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("COURT_ROOM_2_ID", COURT_ROOM_2_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readArray();
        }
    }

    private JsonEnvelope buildRequestEnvelope(JsonArray hearingData) {
        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", hearingData)
                .build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_ACTION_NAME)
                        .withUserId(UUID.randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonEnvelope buildRequestEnvelopeForHearingWithMultipleCourtCentres(final LocalDate hearingDate, final LocalTime hearingTime, final UUID courtCentreId) {
        final JsonObject queryPayload = createObjectBuilder().add("hearings", createArrayBuilder().add(createObjectBuilder()
                .add("judiciary", createArrayBuilder().add(createObjectBuilder().add("judicialId", randomUUID().toString())))
                .add("courtCentreId", courtCentreId.toString())
                .add("hearingsByCourtCentreId", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", hearingDate.toString())
                        .add("hearingsByHearingDate", createArrayBuilder().add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(Stream.of(new String[][]{
                                                        {"startDate", hearingDate.toString()},
                                                        {"endDate", hearingDate.plusDays(1).toString()},
                                                        {"courtRoomId", TOP_LEVEL_COURT_ROOM_ID},
                                                        {"courtCentreId", TOP_LEVEL_COURT_CENTRE_ID}
                                                }).collect(Collectors.toMap(e -> e[0], e -> e[1])))
                                                        .add("allocated", true)
                                                        .add("hearingDays", generateHearingDaysForMultipleCourtCentres(new HashMap<LocalDate, Map<String, String>>(){{
                                                            put(hearingDate, Stream.of(new String[][]{
                                                                    {"hearingTime", hearingTime.toString()},
                                                                    {"courtRoomId", TOP_LEVEL_COURT_ROOM_ID},
                                                                    {"courtCentreId", TOP_LEVEL_COURT_CENTRE_ID}
                                                            }).collect(Collectors.toMap(e -> e[0], e -> e[1])));

                                                            put(hearingDate.plusDays(1), Stream.of(new String[][]{
                                                                    {"hearingTime", hearingTime.toString()},
                                                                    {"courtRoomId", OTHER_COURT_ROOM_ID},
                                                                    {"courtCentreId", OTHER_COURT_CENTRE_ID}
                                                            }).collect(Collectors.toMap(e -> e[0], e -> e[1])));
                                                        }}))
                                                        .add("listedCases", generateListedCases(false, false, false))
                                        )
                                ).add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(Stream.of(new String[][]{
                                                        {"startDate", hearingDate.toString()},
                                                        {"endDate", hearingDate.plusDays(1).toString()},
                                                        {"courtRoomId", OTHER_COURT_ROOM_ID},
                                                        {"courtCentreId", OTHER_COURT_CENTRE_ID}
                                                }).collect(Collectors.toMap(e -> e[0], e -> e[1])))
                                                        .add("allocated", true)
                                                        .add("hearingDays", generateHearingDaysForMultipleCourtCentres(new HashMap<LocalDate, Map<String, String>>(){{
                                                            put(hearingDate, Stream.of(new String[][]{
                                                                    {"hearingTime", hearingTime.plusMinutes(30).toString()},
                                                                    {"courtRoomId", OTHER_COURT_ROOM_ID},
                                                                    {"courtCentreId", OTHER_COURT_CENTRE_ID}
                                                            }).collect(Collectors.toMap(e -> e[0], e -> e[1])));

                                                            put(hearingDate.plusDays(1), Stream.of(new String[][]{
                                                                    {"hearingTime", hearingTime.plusMinutes(30).toString()},
                                                                    {"courtRoomId", TOP_LEVEL_COURT_ROOM_ID},
                                                                    {"courtCentreId", TOP_LEVEL_COURT_CENTRE_ID}
                                                            }).collect(Collectors.toMap(e -> e[0], e -> e[1])));
                                                        }}))
                                                        .add("listedCases", generateListedCases(false, false, false))
                                        )
                                ).add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(Stream.of(new String[][]{
                                                        {"startDate", hearingDate.toString()},
                                                        {"endDate", hearingDate.plusDays(1).toString()},
                                                        {"courtRoomId", TOP_LEVEL_COURT_ROOM_ID},
                                                        {"courtCentreId", TOP_LEVEL_COURT_CENTRE_ID}
                                                }).collect(Collectors.toMap(e -> e[0], e -> e[1])))
                                                        .add("allocated", true)
                                                        .add("hearingDays", generateHearingDaysForMultipleCourtCentres(new HashMap<LocalDate, Map<String, String>>(){{
                                                            put(hearingDate, Stream.of(new String[][]{
                                                                    {"hearingTime", hearingTime.plusMinutes(60).toString()},
                                                                    {"courtRoomId", TOP_LEVEL_COURT_ROOM_ID},
                                                                    {"courtCentreId", TOP_LEVEL_COURT_CENTRE_ID}
                                                            }).collect(Collectors.toMap(e -> e[0], e -> e[1])));

                                                            put(hearingDate.plusDays(1), Stream.of(new String[][]{
                                                                    {"hearingTime", hearingTime.plusMinutes(60).toString()},
                                                                    {"courtRoomId", TOP_LEVEL_COURT_ROOM_ID},
                                                                    {"courtCentreId", TOP_LEVEL_COURT_CENTRE_ID}
                                                            }).collect(Collectors.toMap(e -> e[0], e -> e[1])));
                                                        }}))
                                                        .add("listedCases", generateListedCases(false, false, false))
                                        )
                                )
                        )
                        .build()).build()))).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonArrayBuilder generateHearingDaysForMultipleCourtCentres(final Map<LocalDate, Map<String, String>> hearingDayDetails) {
        final JsonArrayBuilder builder = createArrayBuilder();
        hearingDayDetails.entrySet().stream().forEach(day -> {
            builder.add(createObjectBuilder()
                    .add("courtCentreId", day.getValue().get("courtCentreId"))
                    .add("courtRoomId", day.getValue().get("courtRoomId"))
                    .add("startTime", ZonedDateTime.of(day.getKey(), LocalTime.parse(day.getValue().get("hearingTime")), UTC).toString())
                    .add("endTime", ZonedDateTime.of(day.getKey(), LocalTime.parse(day.getValue().get("hearingTime")).plusMinutes(30), UTC).toString())
                    .add("hearingDate", day.getKey().toString())
                    .add("durationMinutes", 30)
                    .add("sequence", integer(1, 10).next()));
        });

        return builder;
    }

    private JsonObjectBuilder getHearingBuilder(final Map<String, String> hearingDetails) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("id", randomUUID().toString());

        if (MapUtils.isEmpty(hearingDetails)) {
            objectBuilder.add("startDate", LocalDates.to(HEARING_DATE))
                    .add("endDate", LocalDates.to(HEARING_DATE))
                    .add("courtRoomId", COURT_ROOM_ID.toString())
                    .add("courtCentreId", COURT_CENTRE_ID.toString());
        } else {
            hearingDetails.entrySet().forEach(e -> objectBuilder.add(e.getKey(), e.getValue()));
        }

        objectBuilder.add("judiciary", createArrayBuilder().add(createObjectBuilder().add("judicialId", randomUUID().toString())));
        objectBuilder.add("type", createObjectBuilder().add("id", randomUUID().toString()).add("description", "Plea & Trial Preparation").add("welshDescription", "Welsh Plea & Trial Preparation"));
        return objectBuilder;
    }

    private JsonArrayBuilder generateListedCases(final boolean caseRestricted, final boolean defendantRestricted, final boolean legalEntity) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN_1).add("authorityCode", "B01YB"))
                .add("prosecutor", createObjectBuilder().add("prosecutorId", randomUUID().toString()).add("prosecutorCode", "CPS"))
                .add("restrictFromCourtList", FALSE)
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_ID1.toString())
                                .add("firstName", FIRST_NAME_1)
                                .add("lastName", LAST_NAME_1)
                                .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE)
                                .add("offences", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString())
                                        .add("offenceWording", "Wording").add("statementOfOffence", createObjectBuilder().add("title", "Title"))
                                        .add("listingNumber" , 1))))
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_ID2.toString())
                                .add("firstName", FIRST_NAME_2)
                                .add("lastName", LAST_NAME_2)
                                .add("restrictFromCourtList", FALSE)
                                .add("offences", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString())
                                        .add("offenceWording", "Wording").add("statementOfOffence", createObjectBuilder().add("title", "Title"))
                                        .add("listingNumber" , 1))))
                ))
                .add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN_2).add("authorityCode", "B01YB"))
                        .add("prosecutor", createObjectBuilder().add("prosecutorId", randomUUID().toString()).add("prosecutorCode", "CPS"))
                        .add("restrictFromCourtList", caseRestricted ? TRUE : FALSE)
                        .add("offences", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString())
                                .add("offenceWording", "Wording").add("statementOfOffence", createObjectBuilder().add("title", "Title"))))
                        .add("defendants", legalEntity ? createArrayBuilder().add(createObjectBuilder()
                                .add("organisationName", ORGANISATION_NAME_1)
                                .add("restrictFromCourtList", FALSE)) :
                                createArrayBuilder().add(createObjectBuilder()
                                        .add("id", DEFENDANT_ID3.toString())
                                        .add("firstName", FIRST_NAME_3)
                                        .add("lastName", LAST_NAME_3)
                                        .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE)
                                        .add("offences", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString())
                                                .add("offenceWording", "Wording").add("statementOfOffence", createObjectBuilder().add("title", "Title"))
                                                .add("listingNumber" , 1)))
                                )
                        )
                );
    }

    private CourtCentreDetails getCourtCentreDetails(final Boolean welsh, final UUID courtCentreId, final UUID courtRoomId) {
        final CourtRoomDetails courtRoomDetails = courtRoomDetails()
                .withCourtRoomName(COURT_ROOM_NAME).withWelshCourtRoomName(COURT_ROOM_NAME_WELSH)
                .withId(courtRoomId).build();
        Map<UUID, CourtRoomDetails> courtRooms = new HashMap<>();
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        return courtCentreDetails()
                .withCourtCentreName(COURT_CENTRE_NAME_1)
                .withId(courtCentreId)
                .withWelshCourtCentreName(COURT_CENTRE_NAME_WELSH)
                .withAddress1(ADDRESS_01)
                .withPostcode(POST_CODE)
                .withWelshAddress1(ADDRESS_1_WELSH)
                .withCourtRooms(courtRooms)
                .withWelsh(welsh)
                .build();
    }

}
