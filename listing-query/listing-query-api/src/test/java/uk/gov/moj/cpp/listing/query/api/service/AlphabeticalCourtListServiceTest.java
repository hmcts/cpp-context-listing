package uk.gov.moj.cpp.listing.query.api.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyMap;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.range;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_ZONED_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails.courtCentreDetails;
import static uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails.courtRoomDetails;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.MapUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AlphabeticalCourtListServiceTest {

    private static final String FIRST_NAME_1 = "James";
    private static final String LAST_NAME_1 = "Thomas";
    private static final String FIRST_NAME_2 = "Erica";
    private static final String LAST_NAME_2 = "Larsen";
    private static final String FIRST_NAME_3 = "Lindsey";
    private static final String LAST_NAME_3 = "Craft";
    private static final String FIRST_NAME_4 = "Monika";
    private static final String LAST_NAME_4 = "Ross";
    private static final String FIRST_NAME_5 = "Kendra";
    private static final String LAST_NAME_5 = "Love";
    private static final String FIRST_NAME_6 = "Emir";
    private static final String LAST_NAME_6 = "Sharples";
    private static final String ORGANISATION_NAME_1 = "Barker and Sons";
    private static final String COURT_ROOM_NAME = "Room 3";
    private static final String COURT_ROOM_NAME_WELSH = "Room 3 Welsh";
    private static final String COURT_CENTRE_NAME = "Liverpool Crown Court";
    private static final String COURT_CENTRE_NAME_WELSH = "Liverpool Crown Court Welsh Name";
    private static final String ADDRESS_1 = "22 Liverpool Street";
    private static final String POST_CODE = "LV12 9XA";
    private static final String ADDRESS_1_WELSH = "22 Welsh Street";
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final String HH_MM = "HH:mm";
    private static final String EMPTY = "";

    private final UUID COURT_CENTRE_ID = randomUUID();
    private final UUID COURT_ROOM_ID = randomUUID();
    private static final LocalDate HEARING_DATE = LocalDates.from("2018-11-21");
    private static final String HEARING_DATE_WELSH = "21 Tachwedd 2018";
    private final ZonedDateTime START_DATE_TIME_1 = ZonedDateTime.of(2018, 11, 21, integer(23).next(), integer(60).next(), integer(60).next(), integer(999999999).next(), UTC);
    private final ZonedDateTime START_DATE_TIME_2 = ZonedDateTime.of(2018, 11, 21, integer(23).next(), integer(60).next(), integer(60).next(), integer(999999999).next(), UTC);
    private static final LocalDate SUMMER_HEARING_DATE = LocalDates.from("2018-07-21");
    private final ZonedDateTime SUMMER_START_DATE_TIME = ZonedDateTimes.fromString("2018-07-21T13:46:00.000Z");
    private static final String CASE_URN_1 = STRING.next();
    private static final String CASE_URN_2 = STRING.next();
    private static final String APPLICATION_REFERENCE_1 = STRING.next();
    private static final String APPLICATION_REFERENCE_2 = STRING.next();
    private static final String QUERY_NAME = "listing.search.court.list";
    private static final String TOP_LEVEL_COURT_CENTRE_ID = randomUUID().toString();
    private static final String TOP_LEVEL_COURT_ROOM_ID = randomUUID().toString();
    private static final String OTHER_COURT_CENTRE_ID = randomUUID().toString();
    private static final String OTHER_COURT_ROOM_ID = randomUUID().toString();

    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private AlphabeticalCourtListService service;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldBuildDataForLegalEntityAlphabeticalList() {
        final JsonEnvelope envelope = buildRequestEnvelope(false, true);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(HEARING_DATE.getMonth().name())) + SPACE + HEARING_DATE.getYear())),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(3)),

                withJsonPath("$.defendants[0].defendantFullName", equalTo(upperCase(ORGANISATION_NAME_1))),
                withJsonPath("$.defendants[0].caseReference", equalTo(CASE_URN_2)),
                withJsonPath("$.defendants[0].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[0].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute()))),

                withJsonPath("$.defendants[1].defendantFullName", equalTo(upperCase(LAST_NAME_2) + "," + SPACE + FIRST_NAME_2)),
                withJsonPath("$.defendants[1].caseReference", equalTo(CASE_URN_1)),
                withJsonPath("$.defendants[1].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute()))),

                withJsonPath("$.defendants[2].defendantFullName", equalTo(upperCase(LAST_NAME_1) + "," + SPACE + FIRST_NAME_1)),
                withJsonPath("$.defendants[2].caseReference", equalTo(CASE_URN_1)),
                withJsonPath("$.defendants[2].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[2].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute())))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildDataForBulkCaseWithOnlyGroupMasterCaseInAlphabeticalList() {
        final JsonEnvelope envelope = buildOneBulkCaseRequestEnvelope(false, true, true);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(HEARING_DATE.getMonth().name())) + SPACE + HEARING_DATE.getYear())),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(1)),

                withJsonPath("$.defendants[0].defendantFullName", equalTo("1000 DEFENDANTS")),
                withJsonPath("$.defendants[0].caseReference", equalTo(EMPTY)),
                withJsonPath("$.defendants[0].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[0].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute())))

        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildDataForBulkCaseWithGroupMasterCaseAndAnotherRemovedGroupCaseInAlphabeticalList() {
        final JsonEnvelope envelope = buildTwoBulkCasesRequestEnvelope(false, true, true);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(HEARING_DATE.getMonth().name())) + SPACE + HEARING_DATE.getYear())),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(2)),

                withJsonPath("$.defendants[0].defendantFullName", equalTo("1000 DEFENDANTS")),
                withJsonPath("$.defendants[0].caseReference", equalTo(EMPTY)),
                withJsonPath("$.defendants[0].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[0].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute()))),

                withJsonPath("$.defendants[1].defendantFullName", equalTo(upperCase(LAST_NAME_2) + "," + SPACE + FIRST_NAME_2)),
                withJsonPath("$.defendants[1].caseReference", equalTo(CASE_URN_2)),
                withJsonPath("$.defendants[1].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute())))

        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildDataForAlphabeticalListNoRestrictions() {
        final JsonEnvelope envelope = buildRequestEnvelope(false);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(HEARING_DATE.getMonth().name())) + SPACE + HEARING_DATE.getYear())),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(5)),

                withJsonPath("$.defendants[0].defendantFullName", equalTo(upperCase(LAST_NAME_3) + "," + SPACE + FIRST_NAME_3)),
                withJsonPath("$.defendants[0].caseReference", equalTo(CASE_URN_2)),
                withJsonPath("$.defendants[0].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[0].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute()))),

                withJsonPath("$.defendants[1].defendantFullName", equalTo(upperCase(LAST_NAME_2) + "," + SPACE + FIRST_NAME_2)),
                withJsonPath("$.defendants[1].caseReference", equalTo(CASE_URN_1)),
                withJsonPath("$.defendants[1].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute()))),

                withJsonPath("$.defendants[2].defendantFullName", equalTo(upperCase(LAST_NAME_5) + "," + SPACE + FIRST_NAME_5)),
                withJsonPath("$.defendants[2].caseReference", equalTo(APPLICATION_REFERENCE_2)),
                withJsonPath("$.defendants[2].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[2].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_2.getHour(), START_DATE_TIME_2.getMinute()))),

                withJsonPath("$.defendants[3].defendantFullName", equalTo(upperCase(LAST_NAME_4) + "," + SPACE + FIRST_NAME_4)),
                withJsonPath("$.defendants[3].caseReference", equalTo(APPLICATION_REFERENCE_1)),
                withJsonPath("$.defendants[3].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[3].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_2.getHour(), START_DATE_TIME_2.getMinute()))),

                withJsonPath("$.defendants[4].defendantFullName", equalTo(upperCase(LAST_NAME_1) + "," + SPACE + FIRST_NAME_1)),
                withJsonPath("$.defendants[4].caseReference", equalTo(CASE_URN_1)),
                withJsonPath("$.defendants[4].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[4].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute())))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildDataForAlphabeticalListWithRestrictedCase() {
        final JsonEnvelope envelope = buildRequestEnvelopeWithRestrictedCase();
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(HEARING_DATE.getMonth().name())) + SPACE + HEARING_DATE.getYear())),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(3)),

                withJsonPath("$.defendants[0].defendantFullName", equalTo(upperCase(LAST_NAME_2) + "," + SPACE + FIRST_NAME_2)),
                withJsonPath("$.defendants[0].caseReference", equalTo(CASE_URN_1)),
                withJsonPath("$.defendants[0].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[0].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute()))),

                withJsonPath("$.defendants[1].defendantFullName", equalTo(upperCase(LAST_NAME_4) + "," + SPACE + FIRST_NAME_4)),
                withJsonPath("$.defendants[1].caseReference", equalTo(APPLICATION_REFERENCE_1)),
                withJsonPath("$.defendants[1].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_2.getHour(), START_DATE_TIME_2.getMinute()))),

                withJsonPath("$.defendants[2].defendantFullName", equalTo(upperCase(LAST_NAME_1) + "," + SPACE + FIRST_NAME_1)),
                withJsonPath("$.defendants[2].caseReference", equalTo(CASE_URN_1)),
                withJsonPath("$.defendants[2].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[2].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute())))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildDataForAlphabeticalListWithOneRestrictedDefendant() {
        final JsonEnvelope envelope = buildRequestEnvelopeWithRestrictedDefendant();
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(HEARING_DATE.getMonth().name())) + SPACE + HEARING_DATE.getYear())),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(2)),

                withJsonPath("$.defendants[0].defendantFullName", equalTo(upperCase(LAST_NAME_2) + "," + SPACE + FIRST_NAME_2)),
                withJsonPath("$.defendants[0].caseReference", equalTo(CASE_URN_1)),
                withJsonPath("$.defendants[0].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[0].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_1.getHour(), START_DATE_TIME_1.getMinute()))),

                withJsonPath("$.defendants[1].defendantFullName", equalTo(upperCase(LAST_NAME_5) + "," + SPACE + FIRST_NAME_5)),
                withJsonPath("$.defendants[1].caseReference", equalTo(APPLICATION_REFERENCE_2)),
                withJsonPath("$.defendants[1].courtRoomName", equalTo(COURT_ROOM_NAME)),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo(format("%02d:%02d", START_DATE_TIME_2.getHour(), START_DATE_TIME_2.getMinute())))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildDataForAlphabeticalEngWelshList() {
        final JsonEnvelope envelope = buildRequestEnvelope(false);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(true));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.welshCourtCentreName", equalTo(COURT_CENTRE_NAME_WELSH)),
                withJsonPath("$.welshHearingDate", equalTo(HEARING_DATE_WELSH)),
                withJsonPath("$.welshCourtCentreAddress1", equalTo(ADDRESS_1_WELSH + ",")),
                withJsonPath("$.welshCourtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(5))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildDataForAlphabeticalListBST() {
        final JsonEnvelope envelope = buildRequestEnvelope(true);
        when(courtCentreFactory.getCourtCentre(COURT_CENTRE_ID, envelope)).thenReturn(getCourtCentreDetails(false));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, COURT_CENTRE_ID.toString());

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(SUMMER_HEARING_DATE.getDayOfMonth() + SPACE + capitalize(lowerCase(SUMMER_HEARING_DATE.getMonth().name())) + SPACE + SUMMER_HEARING_DATE.getYear())),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(5)),

                withJsonPath("$.defendants[0].hearingStartTime", equalTo("14:46")),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo("14:46")),
                withJsonPath("$.defendants[2].hearingStartTime", equalTo("14:46")),
                withJsonPath("$.defendants[3].hearingStartTime", equalTo("14:46")),
                withJsonPath("$.defendants[4].hearingStartTime", equalTo("14:46"))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(COURT_CENTRE_ID), eq(envelope));
    }

    @Test
    public void shouldBuildAlphabeticalListDataForHearingsWithMultipleCourtCentres_1() {
        final LocalDate date = LocalDate.of(2018, 11, 15);
        final LocalTime time = LocalTime.of(11, 12, 12);
        final JsonEnvelope envelope = buildRequestEnvelopeForHearingWithMultipleCourtCentres(date, time);
        when(courtCentreFactory.getCourtCentre(fromString(TOP_LEVEL_COURT_CENTRE_ID), envelope)).thenReturn(getCourtCentreDetails(false, fromString(TOP_LEVEL_COURT_CENTRE_ID), fromString(TOP_LEVEL_COURT_ROOM_ID)));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, TOP_LEVEL_COURT_CENTRE_ID);

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(date.format(ofPattern("dd MMMM yyyy")))),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(6)),

                withJsonPath("$.defendants[0].hearingStartTime", equalTo(time.format(ofPattern(HH_MM)))),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo(time.plusMinutes(60).format(ofPattern(HH_MM)))),
                withJsonPath("$.defendants[2].hearingStartTime", equalTo(time.format(ofPattern(HH_MM)))),
                withJsonPath("$.defendants[3].hearingStartTime", equalTo(time.plusMinutes(60).format(ofPattern(HH_MM)))),
                withJsonPath("$.defendants[4].hearingStartTime", equalTo(time.format(ofPattern(HH_MM)))),
                withJsonPath("$.defendants[5].hearingStartTime", equalTo(time.plusMinutes(60).format(ofPattern(HH_MM))))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(fromString(TOP_LEVEL_COURT_CENTRE_ID)), eq(envelope));
    }

    @Test
    public void shouldBuildAlphabeticalListDataForHearingsWithMultipleCourtCentres_2() {
        final LocalDate date = LocalDate.of(2018, 11, 15);
        final LocalTime time = LocalTime.of(11, 12, 12);
        final JsonEnvelope envelope = buildRequestEnvelopeForHearingWithMultipleCourtCentres(date, time);
        when(courtCentreFactory.getCourtCentre(fromString(OTHER_COURT_CENTRE_ID), envelope)).thenReturn(getCourtCentreDetails(false, fromString(OTHER_COURT_CENTRE_ID), fromString(OTHER_COURT_ROOM_ID)));

        final Optional<JsonObject> listJson = service.buildAlphabeticalCourtListData(envelope, OTHER_COURT_CENTRE_ID);

        assertThat(listJson.orElse(createObjectBuilder().build()).toString(), isJson(allOf(
                withJsonPath("$.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                withJsonPath("$.hearingDate", equalTo(date.format(ofPattern("dd MMMM yyyy")))),
                withJsonPath("$.courtCentreAddress1", equalTo(ADDRESS_1 + ",")),
                withJsonPath("$.courtCentreAddress2", equalTo(POST_CODE)),
                withJsonPath("$.defendants", hasSize(3)),

                withJsonPath("$.defendants[0].hearingStartTime", equalTo(time.plusMinutes(30).format(ofPattern(HH_MM)))),
                withJsonPath("$.defendants[1].hearingStartTime", equalTo(time.plusMinutes(30).format(ofPattern(HH_MM)))),
                withJsonPath("$.defendants[2].hearingStartTime", equalTo(time.plusMinutes(30).format(ofPattern(HH_MM))))
        )));
        verify(courtCentreFactory).getCourtCentre(eq(fromString(OTHER_COURT_CENTRE_ID)), eq(envelope));
    }

    private JsonEnvelope buildRequestEnvelope(boolean isBST, boolean legalEntity) {
        final boolean caseRestricted = false;
        final boolean defendantRestricted = false;
        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", isBST ? to(SUMMER_HEARING_DATE) : to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder().add(createObjectBuilder()
                                .add("hearing", createObjectBuilder()
                                        .add("id", randomUUID().toString())
                                        .add("startDate", to(HEARING_DATE))
                                        .add("endDate", to(HEARING_DATE))
                                        .add("allocated", true)
                                        .add("courtRoomId", COURT_ROOM_ID.toString())
                                        .add("hearingDays", generateHearingDays(isBST ? SUMMER_START_DATE_TIME : START_DATE_TIME_1))
                                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                                        .add("listedCases", generateListedCases(caseRestricted, defendantRestricted, legalEntity))
                                )
                        ))
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonEnvelope buildOneBulkCaseRequestEnvelope(final boolean isBST, final boolean isGroupProceedings, final boolean isGroupMaster) {
        final boolean caseRestricted = false;
        final boolean defendantRestricted = false;
        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", isBST ? to(SUMMER_HEARING_DATE) : to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder().add(createObjectBuilder()
                                .add("hearing", createObjectBuilder()
                                        .add("id", randomUUID().toString())
                                        .add("startDate", to(HEARING_DATE))
                                        .add("endDate", to(HEARING_DATE))
                                        .add("allocated", true)
                                        .add("courtRoomId", COURT_ROOM_ID.toString())
                                        .add("hearingDays", generateHearingDays(isBST ? SUMMER_START_DATE_TIME : START_DATE_TIME_1))
                                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                                        .add("listedCases", generateListedCaseForBulkCase(caseRestricted, defendantRestricted, isGroupMaster))
                                        .add("isGroupProceedings", isGroupProceedings)
                                        .add("numberOfGroupCases", 1000)
                                )
                        ))
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonEnvelope buildTwoBulkCasesRequestEnvelope(final boolean isBST, final boolean isGroupProceedings, final boolean isGroupMaster) {
        final boolean caseRestricted = false;
        final boolean defendantRestricted = false;
        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", isBST ? to(SUMMER_HEARING_DATE) : to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder().add(createObjectBuilder()
                                .add("hearing", createObjectBuilder()
                                        .add("id", randomUUID().toString())
                                        .add("startDate", to(HEARING_DATE))
                                        .add("endDate", to(HEARING_DATE))
                                        .add("allocated", true)
                                        .add("courtRoomId", COURT_ROOM_ID.toString())
                                        .add("hearingDays", generateHearingDays(isBST ? SUMMER_START_DATE_TIME : START_DATE_TIME_1))
                                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                                        .add("listedCases", generateListedCasesForBulkCase(caseRestricted, defendantRestricted, isGroupMaster))
                                        .add("isGroupProceedings", isGroupProceedings)
                                        .add("numberOfGroupCases", 1000)
                                )
                        ))
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonObjectBuilder getHearingBuilder(final Map<String, String> hearingDetails) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("id", randomUUID().toString());

        if (MapUtils.isEmpty(hearingDetails)) {
            objectBuilder.add("startDate", to(HEARING_DATE))
                    .add("endDate", to(HEARING_DATE))
                    .add("courtRoomId", COURT_ROOM_ID.toString())
                    .add("courtCentreId", COURT_CENTRE_ID.toString());
        } else {
            hearingDetails.entrySet().forEach(e -> objectBuilder.add(e.getKey(), e.getValue()));
        }

        return objectBuilder;
    }

    private JsonEnvelope buildRequestEnvelope(boolean isBST) {
        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", isBST ? to(SUMMER_HEARING_DATE) : to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder().add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(emptyMap())
                                                .add("allocated", true)
                                                .add("hearingDays", generateHearingDays(isBST ? SUMMER_START_DATE_TIME : START_DATE_TIME_1))
                                                .add("listedCases", generateListedCases(false, false, false))
                                        )
                                ).add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(emptyMap())
                                                .add("allocated", true)
                                                .add("hearingDays", generateHearingDays(isBST ? SUMMER_START_DATE_TIME : START_DATE_TIME_2))
                                                .add("courtApplications", generateCourtApplications(false, false))
                                        )
                                )
                        )
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonEnvelope buildRequestEnvelopeForHearingWithMultipleCourtCentres(final LocalDate hearingDate, final LocalTime hearingTime) {
        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
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
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }


    private JsonEnvelope buildRequestEnvelopeWithRestrictedCase() {
        final boolean caseRestricted = true;
        final boolean legalEntity = false;
        final boolean defendantRestricted = false;
        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder().add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(emptyMap())
                                                .add("allocated", true)
                                                .add("hearingDays", generateHearingDays(START_DATE_TIME_1))
                                                .add("listedCases", generateListedCases(caseRestricted, defendantRestricted, legalEntity))
                                        )
                                ).add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(emptyMap())
                                                .add("allocated", true)
                                                .add("hearingDays", generateHearingDays(START_DATE_TIME_2))
                                                .add("courtApplications", generateCourtApplications(caseRestricted, defendantRestricted))
                                        )
                                )
                        )
                        .build()).build()).build();

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                queryPayload);
    }

    private JsonArrayBuilder generateHearingDays(final ZonedDateTime startDateTime) {
        final JsonArrayBuilder builder = createArrayBuilder();
        final boolean additionalHearingDays = BOOLEAN.next();
        if (additionalHearingDays) {
            range(1, integer(5).next())
                    .forEach(index -> {
                        final ZonedDateTime startTime = FUTURE_ZONED_DATE_TIME.next();
                        createObjectBuilder()
                                .add("startTime", ZonedDateTimes.toString(startTime))
                                .add("endTime", ZonedDateTimes.toString(startTime.plusMinutes(20)))
                                .add("hearingDate", to(startTime.toLocalDate()))
                                .add("durationMinutes", 20)
                                .add("sequence", integer(1, 10).next());
                    });
        }
        return builder.add(createObjectBuilder()
                .add("startTime", ZonedDateTimes.toString(startDateTime))
                .add("endTime", ZonedDateTimes.toString(startDateTime.plusMinutes(20)))
                .add("hearingDate", to(startDateTime.toLocalDate()))
                .add("durationMinutes", 20)
                .add("sequence", integer(1, 10).next())
        );
    }

    private JsonArrayBuilder generateHearingDaysForMultipleCourtCentres(final Map<LocalDate, Map<String, String>> hearingDayDetails) {
        final JsonArrayBuilder builder = createArrayBuilder();
        hearingDayDetails.entrySet().stream().forEach(day -> {
            builder.add(createObjectBuilder()
                    .add("courtCentreId", day.getValue().get("courtCentreId"))
                    .add("courtRoomId", day.getValue().get("courtRoomId"))
                    .add("startTime", ZonedDateTime.of(day.getKey(), LocalTime.parse(day.getValue().get("hearingTime")), BST).toString())
                    .add("endTime", ZonedDateTime.of(day.getKey(), LocalTime.parse(day.getValue().get("hearingTime")).plusMinutes(30), BST).toString())
                    .add("hearingDate", day.getKey().toString())
                    .add("durationMinutes", 30)
                    .add("sequence", integer(1, 10).next()));
        });

        return builder;
    }

    private JsonArrayBuilder generateListedCases(final boolean caseRestricted, final boolean defendantRestricted, final boolean legalEntity) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN_1))
                .add("prosecutor", createObjectBuilder().add("prosecutorId", randomUUID().toString()).add("prosecutorCode", "CPS"))
                .add("restrictFromCourtList", FALSE)
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("firstName", FIRST_NAME_1)
                                .add("lastName", LAST_NAME_1)
                                .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE))
                        .add(createObjectBuilder()
                                .add("firstName", FIRST_NAME_2)
                                .add("lastName", LAST_NAME_2)
                                .add("restrictFromCourtList", FALSE))
                ))
                .add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN_2))
                        .add("prosecutor", createObjectBuilder().add("prosecutorId", randomUUID().toString()).add("prosecutorCode", "CPS"))
                        .add("restrictFromCourtList", caseRestricted ? TRUE : FALSE)
                        .add("defendants", legalEntity ? createArrayBuilder().add(createObjectBuilder()
                                .add("organisationName", ORGANISATION_NAME_1)
                                .add("restrictFromCourtList", FALSE)) :
                                createArrayBuilder().add(createObjectBuilder()
                                        .add("firstName", FIRST_NAME_3)
                                        .add("lastName", LAST_NAME_3)
                                        .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE)
                                )
                        )
                );
    }

    private JsonArrayBuilder generateListedCaseForBulkCase(final boolean caseRestricted, final boolean defendantRestricted, final boolean isGroupMaster) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN_1))
                .add("prosecutor", createObjectBuilder().add("prosecutorId", randomUUID().toString()).add("prosecutorCode", "CPS"))
                .add("restrictFromCourtList", caseRestricted ? TRUE : FALSE)
                .add("isGroupMaster", isGroupMaster)
                .add("isGroupMember", true)
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("firstName", FIRST_NAME_1)
                                .add("lastName", LAST_NAME_1)
                                .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE))
                ));
    }

    private JsonArrayBuilder generateListedCasesForBulkCase(final boolean caseRestricted, final boolean defendantRestricted, final boolean isGroupMaster) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN_1))
                .add("prosecutor", createObjectBuilder().add("prosecutorId", randomUUID().toString()).add("prosecutorCode", "CPS"))
                .add("restrictFromCourtList", FALSE)
                .add("isGroupMaster", isGroupMaster)
                .add("isGroupMember", true)
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("firstName", FIRST_NAME_1)
                                .add("lastName", LAST_NAME_1)
                                .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE))
                ))
                .add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("caseIdentifier", createObjectBuilder().add("caseReference", CASE_URN_2))
                        .add("prosecutor", createObjectBuilder().add("prosecutorId", randomUUID().toString()).add("prosecutorCode", "CPS"))
                        .add("restrictFromCourtList", caseRestricted ? TRUE : FALSE)
                        .add("isGroupMaster", false)
                        .add("isGroupMember", true)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                        .add("firstName", FIRST_NAME_2)
                                        .add("lastName", LAST_NAME_2)
                                        .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE)
                                )
                        )
                );
    }

    private JsonArrayBuilder generateCourtApplications(final boolean caseRestricted, final boolean defendantRestricted) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("applicationReference", APPLICATION_REFERENCE_1)
                .add("restrictFromCourtList", FALSE)
                .add("respondents", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("lastName", ORGANISATION_NAME_1)
                                .add("restrictFromCourtList", FALSE)
                                .add("courtApplicationPartyType", "ORGANISATION"))
                )
                .add("applicant", createObjectBuilder()
                        .add("firstName", FIRST_NAME_4)
                        .add("lastName", LAST_NAME_4)
                        .add("restrictFromCourtList", defendantRestricted ? TRUE : FALSE)
                        .add("courtApplicationPartyType", "PERSON"))
        ).add(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("applicationReference", APPLICATION_REFERENCE_2)
                .add("restrictFromCourtList", caseRestricted ? TRUE : FALSE)
                .add("respondents", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("firstName", FIRST_NAME_5)
                                .add("lastName", LAST_NAME_5)
                                .add("restrictFromCourtList", FALSE)
                                .add("courtApplicationPartyType", "PERSON"))
                )
                .add("applicant", createObjectBuilder()
                        .add("firstName", FIRST_NAME_6)
                        .add("lastName", LAST_NAME_6)
                        .add("restrictFromCourtList", FALSE)
                        .add("courtApplicationPartyType", "PERSON"))
        );
    }

    private JsonEnvelope buildRequestEnvelopeWithRestrictedDefendant() {
        final boolean caseRestricted = false;
        final boolean defendantRestricted = true;

        final JsonObject queryPayload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", to(HEARING_DATE))
                        .add("hearingsByHearingDate", createArrayBuilder().add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(emptyMap())
                                                .add("allocated", true)
                                                .add("hearingDays", generateHearingDays(START_DATE_TIME_1))
                                                .add("listedCases", generateListedCases(caseRestricted, defendantRestricted, false))
                                        )
                                ).add(createObjectBuilder()
                                        .add("hearing", getHearingBuilder(emptyMap())
                                                .add("allocated", true)
                                                .add("hearingDays", generateHearingDays(START_DATE_TIME_2))
                                                .add("courtApplications", generateCourtApplications(caseRestricted, defendantRestricted))
                                        )
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
        final CourtRoomDetails courtRoomDetails = courtRoomDetails()
                .withCourtRoomName(COURT_ROOM_NAME).withWelshCourtRoomName(COURT_ROOM_NAME_WELSH)
                .withId(COURT_ROOM_ID).build();
        Map<UUID, CourtRoomDetails> courtRooms = new HashMap<>();
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        return courtCentreDetails()
                .withCourtCentreName(COURT_CENTRE_NAME)
                .withId(COURT_CENTRE_ID)
                .withWelshCourtCentreName(COURT_CENTRE_NAME_WELSH)
                .withAddress1(ADDRESS_1)
                .withPostcode(POST_CODE)
                .withWelshAddress1(ADDRESS_1_WELSH)
                .withCourtRooms(courtRooms)
                .withWelsh(welsh)
                .build();
    }

    private CourtCentreDetails getCourtCentreDetails(final Boolean welsh, final UUID courtCentreId, final UUID courtRoomId) {
        final CourtRoomDetails courtRoomDetails = courtRoomDetails()
                .withCourtRoomName(COURT_ROOM_NAME).withWelshCourtRoomName(COURT_ROOM_NAME_WELSH)
                .withId(courtRoomId).build();
        Map<UUID, CourtRoomDetails> courtRooms = new HashMap<>();
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        courtRooms.put(courtRoomDetails.getId(), courtRoomDetails);
        return courtCentreDetails()
                .withCourtCentreName(COURT_CENTRE_NAME)
                .withId(courtCentreId)
                .withWelshCourtCentreName(COURT_CENTRE_NAME_WELSH)
                .withAddress1(ADDRESS_1)
                .withPostcode(POST_CODE)
                .withWelshAddress1(ADDRESS_1_WELSH)
                .withCourtRooms(courtRooms)
                .withWelsh(welsh)
                .build();
    }
}
