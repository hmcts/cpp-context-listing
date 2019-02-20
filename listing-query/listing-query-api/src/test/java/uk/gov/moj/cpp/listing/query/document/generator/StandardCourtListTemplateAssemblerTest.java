package uk.gov.moj.cpp.listing.query.document.generator;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StandardCourtListTemplateAssemblerTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String START_DATE = "2019-01-29";
    private static final String COURT_CENTRE_NAME = STRING.next();
    private static final String WELSH  = "Welsh";
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

    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private StandardPublicCourtListTemplateAssembler assembler;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWithNullCourtRoomId() throws Exception {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingData()), COURT_CENTRE_ID.toString(), null, CourtListType.STANDARD);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(CourtListType.STANDARD.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(2));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertCourtRoom(actualCourtRoom);

        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(2));
        Hearing actualHearing = actualTimeslot.getHearings().get(0);
        assertHearing(actualHearing);

        Defendant actualDefendant = actualHearing.getDefendants().get(0);
        assertDefendant(actualDefendant);
        assertOffence(actualDefendant.getOffences().get(0));

        assertThat(actualTimeslot.getHearings().get(0).getSequence(), lessThan(actualTimeslot.getHearings().get(1).getSequence()));

        Timeslot actualTimeslot2 = actualCourtRoom.getTimeslots().get(1);
        assertThat(actualTimeslot2.getHearings().size(), is(1));

        assertThat(actualTimeslot2.getHearings().get(0).getStartTime(), is(START_LOCAL_TIME2.toString()));

        CourtRoom actualCourtRoom2 = actualHearingDate.getCourtRooms().get(1);
        assertThat(actualCourtRoom2.getCourtRoomName(), is(COURT_ROOM_NAME_2));
    }

    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWithCourtRoomId() throws Exception {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataService.getJudiciariesByIdList(anyList(), any(JsonEnvelope.class)))
                .thenReturn(generateJsonEnvelope());
        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingData()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.STANDARD);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        assertThat(actualCourtList.getListType(), is(CourtListType.STANDARD.toString().toLowerCase()));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualCourtList.getCourtCentreAddress1(), is(notNullValue()));
        assertThat(actualCourtList.getCourtCentreAddress2(), is(notNullValue()));

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);
        assertThat(actualHearingDate.getHearingDate(), is(START_DATE));


        assertThat(actualHearingDate.getCourtRooms().size(), is(1));
        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertCourtRoom(actualCourtRoom);

        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(2));
        Hearing actualHearing = actualTimeslot.getHearings().get(0);
        assertHearing(actualHearing);

        Defendant actualDefendant = actualHearing.getDefendants().get(0);
        assertDefendant(actualDefendant);
        assertOffence(actualDefendant.getOffences().get(0));

        assertThat(actualTimeslot.getHearings().get(0).getSequence(), lessThan(actualTimeslot.getHearings().get(1).getSequence()));

        Timeslot actualTimeslot2 = actualCourtRoom.getTimeslots().get(1);
        assertThat(actualTimeslot2.getHearings().size(), is(1));

        assertThat(actualTimeslot2.getHearings().get(0).getStartTime(), is(START_LOCAL_TIME2.toString()));

        CourtRoom actualCourtRoom1 = actualHearingDate.getCourtRooms().get(0);
        assertThat(actualCourtRoom1.getCourtRoomName(), is(COURT_ROOM_NAME_1));
    }

    @Test
    public void shouldAssembleDataForStandardCourtListTemplateWhenThereIsNoJudiciary() throws Exception {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());

        Optional<JsonObject> standardListData = assembler.assemble(buildRequestEnvelope(buildHearingDataForNoJudiciary()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.STANDARD);
        final StandardCourtList actualCourtList = jsonObjectToObjectConverter.convert(standardListData.get(), StandardCourtList.class);

        System.out.println("courtList:" + actualCourtList);

        assertThat(actualCourtList.getHearingDates().size(), is(1));

        HearingDate actualHearingDate = actualCourtList.getHearingDates().get(0);

        assertThat(actualHearingDate.getCourtRooms().size(), is(1));

        CourtRoom actualCourtRoom = actualHearingDate.getCourtRooms().get(0);
        assertThat(actualCourtRoom.getTimeslots().size(), is(1));


        Timeslot actualTimeslot = actualCourtRoom.getTimeslots().get(0);
        assertThat(actualTimeslot.getHearings().size(), is(1));
    }


    private void assertCourtRoom(CourtRoom actualCourtRoom) {
        assertThat(actualCourtRoom.getCourtRoomName(), is(COURT_ROOM_NAME_1));
        assertThat(actualCourtRoom.getJudiciaryNames(), CoreMatchers.containsString("Ainsworth"));

        assertThat(actualCourtRoom.getTimeslots().size(), is(2));

    }

    private void assertHearing(Hearing actualHearing) {
        assertThat(actualHearing.getCaseNumber(), is(notNullValue()));
        assertThat(actualHearing.getHearingType(), is(notNullValue()));
        assertThat(actualHearing.getProsecutorType(), is(notNullValue()));
        assertThat(actualHearing.getSequence(), is(SEQUENCE_1));
        assertThat(actualHearing.getStartTime(), is(START_LOCAL_TIME.toString()));
        assertThat(actualHearing.getDefendants().size(), is(2));
    }

    private void assertDefendant(Defendant actualDefendant) {
        assertThat(actualDefendant.getDateOfBirth(), is(LocalDate.parse(DATE_OF_BIRTH).format(DOB_FORMATTER)));
        assertThat(actualDefendant.getFirstName(), is(notNullValue()));
        assertThat(actualDefendant.getSurname(), is(notNullValue()));
        assertThat(actualDefendant.getAge(), is(notNullValue()));
        assertThat(actualDefendant.getOffences().size(), is(2));
    }

    private void assertOffence(Offence actualOffence) {
        assertThat(actualOffence.getOffenceTitle(), is(notNullValue()));
        assertThat(actualOffence.getOffenceWording(), is(notNullValue()));
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

    private JsonArray buildHearingDataForNoJudiciary() {
        String jsonString = FileUtil.getPayload("stubbed.hearingRepository.findHearingsForPublicList-StandardList-NoJudiciaryScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString());
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


}