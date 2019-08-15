package uk.gov.moj.cpp.listing.query.api.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.WelshMonth;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.document.generator.alphabetical.courtlist.AlphabeticalCourtList;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AlphabeticalCourtListServiceTest {
    private static final String FIRST_NAME = "James";
    private static final String LAST_NAME = "Thomas";
    private static final String COURT_ROOM_NAME = "Room 3";
    private static final String COURT_ROOM_NAME_WELSH = "Room 3 Welsh";
    private static final String COURT_CENTRE_NAME = "Liverpool Crown Court";
    private static final String COURT_CENTRE_NAME_WELSH = "Liverpool Crown Court Welsh Name";
    private static final String ADDRESS_1 = "22 Liverpool Street";
    private static final String POST_CODE = "LV12 9XA";
    private static final String ADDRESS_1_WELSH = "22 Welsh Street";
    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private AlphabeticalCourtListService service;


    private final UUID COURT_CENTRE_ID = randomUUID();
    private final UUID COURT_ROOM_ID = randomUUID();
    private static final LocalDate HEARING_DATE = LocalDates.from("2018-11-21");
    private final ZonedDateTime START_DATE_TIME = ZonedDateTimes.fromString("2018-11-21T13:46:00.000Z");
    private static final LocalDate SUMMER_HEARING_DATE = LocalDates.from("2018-07-21");
    private final ZonedDateTime SUMMER_START_DATE_TIME = ZonedDateTimes.fromString("2018-07-21T13:46:00.000Z");
    private static final String CASE_URN = "CURN";
    private static final String QUERY_NAME = "listing.search.court.list";

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldBuildDataForAlphabeticalListNoRestrictions() {
        final JsonEnvelope envelope = buildRequestEnvelope(false);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));
        Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());
        final AlphabeticalCourtList courtList = jsonObjectToObjectConverter.convert(listJson.get(), AlphabeticalCourtList.class);
        assertCourtListValues(courtList, 2, false);
    }

    @Test
    public void shouldBuildDataForAlphabeticalListWithRestrictedCase() {
        final JsonEnvelope envelope = buildRequestEnvelopeWithRestrictedCase();
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));
        Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());
        final AlphabeticalCourtList courtList = jsonObjectToObjectConverter.convert(listJson.get(), AlphabeticalCourtList.class);
        assertCourtListValues(courtList, 1, false);
    }

    @Test
    public void shouldBuildDataForAlphabeticalListWithOneRestrictedDefendant() {
        final JsonEnvelope envelope = buildRequestEnvelopeWithOneDefendantRestricted();
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));
        Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());
        final AlphabeticalCourtList courtList = jsonObjectToObjectConverter.convert(listJson.get(), AlphabeticalCourtList.class);
        assertCourtListValues(courtList, 1, false);
    }
    @Test
    public void shouldBuildDataForAlphabeticalEngWelshList() {
        final JsonEnvelope envelope = buildRequestEnvelope(false);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(true));
        Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());
        final AlphabeticalCourtList courtList = jsonObjectToObjectConverter.convert(listJson.get(), AlphabeticalCourtList.class);
        assertCourtListValues(courtList, 2, false);
        assertWelshValues(courtList);
    }
    public void shouldBuildDataForAlphabeticalListBST() {
        final JsonEnvelope envelope = buildRequestEnvelope(true);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));
        Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());
        final AlphabeticalCourtList courtList = jsonObjectToObjectConverter.convert(listJson.get(), AlphabeticalCourtList.class);
        assertCourtListValues(courtList, 1, true);
    }
    private void assertWelshValues(final AlphabeticalCourtList courtList) {
        assertEquals(COURT_CENTRE_NAME_WELSH, courtList.getWelshCourtCentreName());
        assertEquals(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(WelshMonth.valueFor(HEARING_DATE.getMonth()).get().name()))
                + SPACE + HEARING_DATE.getYear(), courtList.getWelshHearingDate());
        assertEquals(ADDRESS_1_WELSH + ",", courtList.getWelshCourtCentreAddress1());
        assertEquals(POST_CODE, courtList.getWelshCourtCentreAddress2());
        assertEquals(StringUtils.upperCase(LAST_NAME) + "," + SPACE + FIRST_NAME, courtList.getDefendants().get(0).getDefendantFullName());
        assertEquals(CASE_URN, courtList.getDefendants().get(0).getCaseReference());
        assertEquals(COURT_ROOM_NAME_WELSH, courtList.getDefendants().get(0).getCourtRoomNameWelsh());

    }

    private void assertCourtListValues(final AlphabeticalCourtList courtList, final int expectedDefendantCount, boolean isBST) {
        assertEquals(COURT_CENTRE_NAME, courtList.getCourtCentreName());
        if(isBST) {
            assertEquals(SUMMER_HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(SUMMER_HEARING_DATE.getMonth().name()))
                    + SPACE + SUMMER_HEARING_DATE.getYear(), courtList.getHearingDate());
        }
        else {
            assertEquals(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(HEARING_DATE.getMonth().name()))
                    + SPACE + HEARING_DATE.getYear(), courtList.getHearingDate());
        }
        assertEquals(ADDRESS_1 + ",", courtList.getCourtCentreAddress1());
        assertEquals(POST_CODE, courtList.getCourtCentreAddress2());
        assertEquals(expectedDefendantCount, courtList.getDefendants().size());
        courtList.getDefendants().stream().forEach(defendant -> {
            assertEquals(StringUtils.upperCase(LAST_NAME) + "," + SPACE + FIRST_NAME, defendant.getDefendantFullName());
            assertEquals(CASE_URN, defendant.getCaseReference());
            assertEquals(COURT_ROOM_NAME, defendant.getCourtRoomName());
            if(isBST) {
                assertEquals("14:46", defendant.getHearingStartTime());
            }
            else{
                assertEquals("13:46", defendant.getHearingStartTime());
            }

        });

    }


    private JsonEnvelope buildRequestEnvelope(boolean isBST) {
        final JsonObject queryPayload = createObjectBuilder().add("hearings",
                createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", isBST? LocalDates.to(SUMMER_HEARING_DATE) :LocalDates.to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("startTime", isBST? ZonedDateTimes.toString(SUMMER_START_DATE_TIME):ZonedDateTimes.toString(START_DATE_TIME))
                                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                                        .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN))
                                        .add("courtRoomId", COURT_ROOM_ID.toString())
                                        .add("restrictFromCourtList", FALSE)
                                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                                        .add("firstName", FIRST_NAME)
                                                        .add("lastName", LAST_NAME)
                                                        .add("restrictFromCourtList", FALSE)
                                                )
                                                        .add(createObjectBuilder()
                                                                .add("firstName", FIRST_NAME)
                                                                .add("lastName", LAST_NAME)
                                                                .add("restrictFromCourtList", FALSE)
                                                        )
                                        )
                                ))
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonEnvelope buildRequestEnvelopeWithOneDefendantRestricted() {
        final JsonObject queryPayload = createObjectBuilder().add("hearings",
                createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", LocalDates.to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("startTime", ZonedDateTimes.toString(START_DATE_TIME))
                                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                                        .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN))
                                        .add("courtRoomId", COURT_ROOM_ID.toString())
                                        .add("restrictFromCourtList", FALSE)
                                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                                        .add("firstName", FIRST_NAME)
                                                        .add("lastName", LAST_NAME)
                                                        .add("restrictFromCourtList", FALSE)
                                                )
                                                        .add(createObjectBuilder()
                                                                .add("firstName", FIRST_NAME)
                                                                .add("lastName", LAST_NAME)
                                                                .add("restrictFromCourtList", TRUE)
                                                        )
                                        )
                                ))
                        .build()).build()).build();
        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonEnvelope buildRequestEnvelopeWithRestrictedCase() {
        final JsonObject queryPayload = createObjectBuilder().add("hearings",
                createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", LocalDates.to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("startTime", ZonedDateTimes.toString(START_DATE_TIME))
                                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                                        .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN))
                                        .add("courtRoomId", COURT_ROOM_ID.toString())
                                        .add("restrictFromCourtList", FALSE)
                                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                                .add("firstName", FIRST_NAME)
                                                .add("lastName", LAST_NAME)
                                                .add("restrictFromCourtList", FALSE)
                                        ))
                                )
                                .add(createObjectBuilder()
                                        .add("startTime", ZonedDateTimes.toString(START_DATE_TIME))
                                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                                        .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN))
                                        .add("courtRoomId", COURT_ROOM_ID.toString())
                                        .add("restrictFromCourtList", TRUE)
                                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                                .add("firstName", FIRST_NAME)
                                                .add("lastName", LAST_NAME)
                                                .add("restrictFromCourtList", FALSE)
                                        ))
                                )
                        )
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }
    private CourtCentreDetails getCourtCentreDetails(final Boolean welsh) {
        final CourtRoomDetails courtRoomDetails = CourtRoomDetails.courtRoomDetails()
                .withCourtRoomName(COURT_ROOM_NAME).withWelshCourtRoomName(COURT_ROOM_NAME_WELSH)
                .withId(COURT_ROOM_ID).build();
        Map<UUID, CourtRoomDetails> courtRooms = new HashMap<>();
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        return CourtCentreDetails.courtCentreDetails().withCourtCentreName(COURT_CENTRE_NAME)
                .withId(COURT_CENTRE_ID)
                .withWelshCourtCentreName(COURT_CENTRE_NAME_WELSH)
                .withAddress1(ADDRESS_1)
                .withPostcode(POST_CODE)
                .withWelshAddress1(ADDRESS_1_WELSH)
                .withCourtRooms(courtRooms)
                .withWelsh(welsh)
                .build();
    }
}
