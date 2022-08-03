package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.parse;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameter;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class RangeSearchQueryTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final boolean ALLOCATED = true;
    private static final String ALLOCATEDSTR = "true";
    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final String SEARCH_DATE_QUERY_PARAMETER = "searchDate";
    private static final String START_DATE_QUERY_PARAMETER = "startDate";
    private static final String END_DATE_QUERY_PARAMETER = "endDate";
    private static final String WEEK_COMMENCING_START_DATE_QUERY_PARAMETER = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE_QUERY_PARAMETER = "weekCommencingEndDate";
    private static final String COURT_CENTRE_QUERY_PARAMETER = "courtCentreId";

    private static final String COURT_ROOM_QUERY_PARAMETER = "courtRoomId";
    private static final String AUTHORITY_ID_QUERY_PARAMETER = "authorityId";
    private static final String HEARING_TYPE_QUERY_PARAMETER = "hearingTypeId";
    private static final String JURISDICTION_TYPE_QUERY_PARAMETER = "jurisdictionType";
    private static final String AUTHORITY_ID = "efa4e01b-1dc5-48c5-80b5-c3858a7622d6";
    private  static final String AUTHORITY_ID_SEARCH = String.format("[ { \"caseIdentifier\": { \"authorityId\": \"%s\" } } ]", AUTHORITY_ID);
    private static final String PROSECUTOR_ID_SEARCH = String.format("[ { \"prosecutor\": { \"prosecutorId\": \"%s\" } } ]", AUTHORITY_ID);
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final LocalDate SEARCH_DATE = LocalDate.now();

    private static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.now();
    private static final LocalDate WEEK_COMMENCING_END_DATE = LocalDate.now().plusDays(7);

    private static final String EARLIEST_SEARCH_DATE = "1900-01-01";
    private static final String LATEST_SEARCH_DATE = "9999-01-01";

    private static final String PAGE_SIZE = "pageSize";
    private static final String PAGE_NUMBER = "pageNumber";

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Logger logger;

    @Mock
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    @Mock
    private PaginationParameter paginationParameter;

    @InjectMocks
    private RangeSearchQuery rangeSearchQuery;

    private ListToJsonArrayConverter listToJsonArrayConverter = new ListToJsonArrayConverter();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Before
    public void setup() throws IllegalAccessException {
        final ObjectMapper objectMapper = new ObjectMapper();
        FieldUtils.writeField(this.listToJsonArrayConverter, "mapper", objectMapper, true);
        FieldUtils.writeField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter, true);
        FieldUtils.writeField(this.rangeSearchQuery, "listToJsonArrayConverter", listToJsonArrayConverter, true);
        paginationParameter = new PaginationParameter(50, 1, 0);
    }


    @Test
    public void searchHearingsWithDateRangeWithAllParametersProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        when(hearingRepository.findHearings(
                ALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }
    @Test
    public void searchHearingsforFirmListWhereNoPaginationIsRequired() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        when(hearingRepository.findHearingsWithNoPagination(
                ALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add("noPagination", true)
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }

    @Test
    public void searchHearingsWithWeekCommencingDateRange() {

        final List<Hearing> hearingsJson = hearingJsonForWeekCommencing();

        doReturn(hearingsJson)
                .when(hearingRepository)
                .findHearingsByWeekCommencingRange(
                        null,
                        null,
                        AUTHORITY_ID,
                        null,
                        null,
                        WEEK_COMMENCING_START_DATE,
                        WEEK_COMMENCING_END_DATE,0, paginationParameter.getPageSize());

        doReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson))
                .when(hearingJsonListConverterFilterEjectCases)
                .convert(hearingsJson);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2019-10-13", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("weekCommencingStartDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }

    @Test
    public void searchUnallocatedHearingsWithWeekCommencingDateRange() {

        final List<Hearing> hearingsJson = hearingJsonForWeekCommencing();

        when(hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                null,
                null,
                AUTHORITY_ID,
                null,
                null,
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE),
                false, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, "")
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2019-10-13", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("weekCommencingStartDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }


    @Test
    public void searchHearingsWithDateRangeWithAllOptionalParametersNotProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        UUID uuid = fromString(AUTHORITY_ID);

        when(hearingRepository.findHearings(
                ALLOCATEDSTR,
                null,
                null,
                null,
                null,
                null,
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE), 0, paginationParameter.getPageSize()
        ))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }

    private List<Hearing> hearingsJson(String allocated) {
        final String testJsonString = "{ \"allocated\":\"" + allocated + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        hearing1.setTotalCount(Long.valueOf(2));
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        return newArrayList(hearing1, hearing2);
    }

    private List<Hearing> hearingJsonForWeekCommencing() {
        final String testJsonStringForAllocated = "{\n" +
                "\t\t\"id\": \"54482cb7-31aa-4c64-8656-3be6e3a4d158\",\n" +
                "\t\t\"allocated\": \"true\",\n" +
                "\t\t\"weekCommencingStartDate\": \"2019-10-13\",\n" +
                "\t\t\"weekCommencingEndDate\": \"2019-10-25\",\n" +
                "\t\t\"startDate\": \"2020-09-03\",\n" +
                "\t\t\"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\",\n" +
                "\t\t\"listedCases\": [{\n" +
                "\t\t}],\n" +
                "\t\t\"courtApplications\": [{\n" +
                "\t\t}]\n" +
                "\t}";
        final String testJsonStringForUnallocated = "{\n" +
                "\t\t\"id\": \"54482cb7-31aa-4c64-8656-3be6e3a4d158\",\n" +
                "\t\t\"allocated\": \"false\",\n" +
                "\t\t\"weekCommencingStartDate\": \"2019-10-13\",\n" +
                "\t\t\"weekCommencingEndDate\": \"2019-10-25\",\n" +
                "\t\t\"listedCases\": [{\n" +
                "\t\t}],\n" +
                "\t\t\"courtApplications\": [{\n" +
                "\t\t}]\n" +
                "\t}";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonStringForAllocated));
        hearing1.setTotalCount(Long.valueOf(2));
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonStringForUnallocated));
        return newArrayList(hearing1, hearing2);

    }


    private List<Notes> createNotesList() {
        return newArrayList(new Notes(UUID.randomUUID(), UUID.fromString("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18"), LocalDates.from("2020-09-03"), "Note 1"));
    }
}
