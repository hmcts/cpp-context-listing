package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.parse;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.EXACT_HEARING_START_DATETIME;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingIdsResponse;
import uk.gov.moj.cpp.listing.common.service.IdResponse;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameter;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;
import uk.gov.moj.cpp.listing.query.view.service.SessionJudiciaryEnrichmentService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class RangeSearchQueryTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String OU_CODE = "B01LY00";
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final boolean ALLOCATED = true;
    private static final String ALLOCATEDSTR = "true";
    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final boolean POSSIBLE_DISQUALIFICATION_STR = true;
    private static final String POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER = "possibleDisqualification";
    private static final String SEARCH_DATE_QUERY_PARAMETER = "searchDate";
    private static final String START_DATE_QUERY_PARAMETER = "startDate";
    private static final String END_DATE_QUERY_PARAMETER = "endDate";
    private static final String WEEK_COMMENCING_START_DATE_QUERY_PARAMETER = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE_QUERY_PARAMETER = "weekCommencingEndDate";
    private static final String COURT_CENTRE_QUERY_PARAMETER = "courtCentreId";
    private static final String OU_CODE_QUERY_PARAMETER = "ouCode";
    private static final String BUSINESS_TYPE_QUERY_PARAMETER = "businessType";
    private static final String COURT_SESSION_QUERY_PARAMETER = "courtSession";

    private static final String COURT_ROOM_QUERY_PARAMETER = "courtRoomId";
    private static final String AUTHORITY_ID_QUERY_PARAMETER = "authorityId";
    private static final String HEARING_TYPE_QUERY_PARAMETER = "hearingTypeId";
    private static final String JURISDICTION_TYPE_QUERY_PARAMETER = "jurisdictionType";
    private static final String AUTHORITY_ID = "efa4e01b-1dc5-48c5-80b5-c3858a7622d6";
    private static final String AUTHORITY_ID_SEARCH = String.format("[ { \"caseIdentifier\": { \"authorityId\": \"%s\" } } ]", AUTHORITY_ID);
    private static final String PROSECUTOR_ID_SEARCH = String.format("[ { \"prosecutor\": { \"prosecutorId\": \"%s\" } } ]", AUTHORITY_ID);
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final JurisdictionType MAGISTRATES_TYPE = JurisdictionType.MAGISTRATES;
    private static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.parse("2023-08-29");
    private static final LocalDate SEARCH_DATE = WEEK_COMMENCING_START_DATE;
    private static final LocalDate WEEK_COMMENCING_END_DATE = WEEK_COMMENCING_START_DATE.plusDays(7);
    private static final String BUSINESS_TYPE = "G1T";
    private static final String COURT_SESSION = "AD";

    private static final String EARLIEST_SEARCH_DATE = "1900-01-01";
    private static final String LATEST_SEARCH_DATE = "9999-01-01";

    private static final String PAGE_SIZE = "pageSize";
    private static final String PAGE_NUMBER = "pageNumber";
    private static final boolean IS_POSSIBLE_DISQUALIFICATION = true;

    private static final String TRIAL_HEARING_TYPE_ID = "bf8155e1-90b9-4080-b133-bfbad895d6e4";
    private static final Set<String> hearingTypeIds = new HashSet<>(Arrays.asList(TRIAL_HEARING_TYPE_ID));

    private static final String IS_CIVIL = "isCivil";
    private static final String IS_GROUP_MEMBER = "isGroupMember";
    private static final String IS_GROUP_MASTER = "isGroupMaster";
    private static final String COURT_SESSION_OR_BUSINESS_ERR = "courtSession or businessType are only relevant to allocated MAGs with ouCode";
    private static final String AM = "AM";

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Mock
    private Logger logger;

    @Spy
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    @Spy
    private PaginationParameterFactory paginationParameterFactory;

    @Mock
    private PaginationParameter paginationParameter;

    @Mock
    private NotesService notesService;

    @Mock
    private SessionJudiciaryEnrichmentService sessionJudiciaryEnrichmentService;

    @InjectMocks
    private RangeSearchQuery rangeSearchQuery;

    private ListToJsonArrayConverter listToJsonArrayConverter = new ListToJsonArrayConverter();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        final ObjectMapper objectMapper = new ObjectMapper();
        FieldUtils.writeField(this.listToJsonArrayConverter, "mapper", objectMapper, true);
        FieldUtils.writeField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter, true);
        FieldUtils.writeField(this.rangeSearchQuery, "listToJsonArrayConverter", listToJsonArrayConverter, true);
        final JsonObject paginationParametersAsJson = createObjectBuilder().add("pageSize", 50).add("pageNumber", 1).add("offset", 0).build();
        paginationParameter = paginationParameterFactory.newPaginationParameter(paginationParametersAsJson);
        hearingJsonListConverterFilterEjectCases = new HearingJsonListConverterFilterEjectCases();
    }

    @Test
    public void rangeSearchHearingsForJudgeList() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        when(hearingRepository.findHearings(
                ALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                null,
                null,
                SEARCH_DATE,
                SEARCH_DATE))
                .thenReturn(hearingsJson);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearingsForJudgeList(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertEquals("listing.range.search.hearings.for.judge", results.metadata().name());
    }

    @Test
    public void searchHearingsForCotr() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        when(hearingRepository.findHearingsForCotr(
                hearingTypeIds,
                COURT_CENTRE_ID.toString(),
                SEARCH_DATE,
                SEARCH_DATE))
                .thenReturn(hearingsJson);


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .build());

        final JsonEnvelope results = rangeSearchQuery.searchHearingsForCotr(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }


    @Test
    public void searchHearingsWithDateRangeWithAllParametersProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                UUID.fromString(AUTHORITY_ID),
                HEARING_TYPE_ID,
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);


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
        assertEquals(2, results.payloadAsJsonObject().getInt("results"));
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }

    @Test
    public void searchMagistratesHearings() {
        final List<Hearing> hearings = hearingsJson(ALLOCATEDSTR);
        final List<IdResponse> hearingIds = new ArrayList<>();
        hearings.forEach(hearing -> hearingIds.add(new IdResponse(hearing.getId(), UUID.randomUUID(), LocalDate.now(), 1,1)));

        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, 2, 1);

        when(courtSchedulerServiceAdapter
                .getCourtSchedulerHearings(
                        OU_CODE,
                        Optional.of(COURT_SESSION),
                        COURT_ROOM_ID.toString(),
                        SEARCH_DATE.toString(),
                        SEARCH_DATE.toString(),
                        Optional.empty(),
                        Optional.of(BUSINESS_TYPE), "ADULT,YOUTH", 50, 1)).thenReturn(response);

        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(hearings);


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(OU_CODE_QUERY_PARAMETER, OU_CODE)
                        .add(COURT_SESSION_QUERY_PARAMETER, COURT_SESSION.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, MAGISTRATES_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchCourtCalendar(query);

        final JsonObject payloadJsonObj = results.payloadAsJsonObject();
        final JsonArray hearingsJsonArr = payloadJsonObj.getJsonArray("hearings");

        assertThat(payloadJsonObj.getInt("results"), is(2));
        assertThat(hearingsJsonArr.size(), is(2));
        assertThat(hearingsJsonArr.getJsonObject(0).getString("startDate"), is("2020-09-03"));
        assertThat(hearingsJsonArr.getJsonObject(0).getJsonArray("hearingDays").getJsonObject(0).getString("courtScheduleId"), is(notNullValue()));
        assertThat(hearingsJsonArr.getJsonObject(1).getJsonArray("hearingDays").getJsonObject(0).getString("courtScheduleId"), is(notNullValue()));
        assertThat(results.metadata().name(), is("listing.search.hearings"));
    }


    @Test
    public void searchHearingsWithDateRangeWithAllParametersProvidedWithoutPagination() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        when(hearingRepository.findHearings(
                ALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE))
                .thenReturn(hearingsJson);

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
                        .add("noPagination", true)
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals(2, results.payloadAsJsonObject().getInt("results"));
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
        verify(hearingRepository).findHearings(
                ALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE);

        verify(notesService).findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), anyList());
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
                        WEEK_COMMENCING_START_DATE.minusDays(1),
                        WEEK_COMMENCING_END_DATE, 0, paginationParameter.getPageSize());


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
        assertEquals(2, results.payloadAsJsonObject().getInt("results"));
        assertEquals("2019-10-13", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("weekCommencingStartDate"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }

    @Test
    public void searchHearingsWithWeekCommencingDateRangeWithNoPagination() {

        final List<Hearing> hearingsJson = hearingJsonForWeekCommencing();

        doReturn(hearingsJson)
                .when(hearingRepository)
                .findHearingsByWeekCommencingRangeWithNoPagination(
                        null,
                        null,
                        AUTHORITY_ID,
                        null,
                        null,
                        WEEK_COMMENCING_START_DATE.minusDays(1),
                        WEEK_COMMENCING_END_DATE);


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .add("noPagination", true)
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
                fromString(AUTHORITY_ID),
                null,
                null,
                parse(WEEK_COMMENCING_START_DATE.toString()).minusDays(1),
                parse(WEEK_COMMENCING_END_DATE.toString()),
                false, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
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
    public void searchUnallocatedHearingsWithWeekCommencingDateRangeAndPossibleDisqualification() {

        final List<Hearing> hearingsJson = hearingJsonForWeekCommencingAndPossibleDisqualification();

        when(hearingRepository.findUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification(
                null,
                null,
                AUTHORITY_ID,
                null,
                null,
                parse(WEEK_COMMENCING_START_DATE.toString()).minusDays(1),
                parse(WEEK_COMMENCING_END_DATE.toString()),
                false, true, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .add(POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER, true)
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(2, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2019-10-13", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("weekCommencingStartDate"));
        assertEquals("true", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("isPossibleDisqualification"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }

    @Test
    public void searchHearingsWithDateRangeWithAllOptionalParametersNotProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        UUID uuid = fromString(AUTHORITY_ID);

        when(hearingRepository.findHearings(
                ALLOCATED,
                null,
                null,
                null,
                null,
                null,
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE), 0, paginationParameter.getPageSize())
        )
                .thenReturn(hearingsJson);

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

    @Test
    public void searchHearingsWithDateRangeWithPossibleDisqualificationNotProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR, POSSIBLE_DISQUALIFICATION_STR);
        when(hearingRepository.findHearings(
                ALLOCATED, null, null,
                null, null, null,
                parse(EARLIEST_SEARCH_DATE), parse(LATEST_SEARCH_DATE),
                0, paginationParameter.getPageSize())
        ).thenReturn(hearingsJson);


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER, true)
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals(1, results.payloadAsJsonObject().getJsonArray("hearings").size());
        assertEquals("2020-09-03", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("startDate"));
        assertTrue(results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getBoolean("isPossibleDisqualification"));
        assertEquals("listing.search.hearings", results.metadata().name());
    }

    @Test
    void searchHearingsWithBusinessTypeUnallocated() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE)
                        .build());

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () ->  rangeSearchQuery.rangeSearchHearings(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.is(COURT_SESSION_OR_BUSINESS_ERR));
    }

    @Test
    void searchHearingsWithSessionTypeUnallocated() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add("courtSession", AM)
                        .build());

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () ->  rangeSearchQuery.rangeSearchHearings(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.is(COURT_SESSION_OR_BUSINESS_ERR));
    }

    @Test
    void searchHearingsWithBusinessTypeNoOuCode() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add("businessType", BUSINESS_TYPE)
                        .build());

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () ->  rangeSearchQuery.rangeSearchHearings(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.is(COURT_SESSION_OR_BUSINESS_ERR));
    }

    @Test
    void searchHearingsWithSessionTypeNoOuCode() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add("courtSession", AM)
                        .build());

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () ->  rangeSearchQuery.rangeSearchHearings(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.is(COURT_SESSION_OR_BUSINESS_ERR));
    }

    @Test
    void searchHearingsWithBusinessTypeCrown() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JurisdictionType.CROWN.name())
                        .build());

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () ->  rangeSearchQuery.rangeSearchHearings(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.is(COURT_SESSION_OR_BUSINESS_ERR));
    }

    @Test
    void searchHearingsWithSessionTypeCrown() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(COURT_SESSION_QUERY_PARAMETER, AM)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JurisdictionType.CROWN.name())
                        .build());

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () ->  rangeSearchQuery.rangeSearchHearings(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.is(COURT_SESSION_OR_BUSINESS_ERR));
    }

    @Test
    void searchHearingsWithSessionTypeAnyUnallocated() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(COURT_SESSION_QUERY_PARAMETER, "ANY")
                        .build());

        final JsonEnvelope response = rangeSearchQuery.rangeSearchHearings(query);

        assertNotNull(response);
        verify(hearingRepository).findHearings(
                false,
                null,
                null,
                null,
                null,
                null,
                LocalDate.parse("1900-01-01"),
                LocalDate.parse("9999-01-01"),
                0,
                50);

    }

    @Test
    void searchHearingsWithExactHearingStartDateTime() {
        final String exactHearingStartDateTime = "2023-08-29T10:30:00Z";
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(EXACT_HEARING_START_DATETIME, exactHearingStartDateTime)
                        .add(PAGE_SIZE, 10)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final List<Hearing> mockHearings = hearingsJson(ALLOCATEDSTR);
        when(hearingRepository.findAllocatedHearingsForCourtCalendar(
                eq(COURT_CENTRE_ID),
                eq(COURT_ROOM_ID),
                eq(UUID.fromString(AUTHORITY_ID)),
                eq(HEARING_TYPE_ID),
                eq(JURISDICTION_TYPE.toString()),
                eq(SEARCH_DATE),
                eq(SEARCH_DATE),
                eq(Instant.parse(exactHearingStartDateTime)),
                eq(0),
                eq(10)
        )).thenReturn(mockHearings);

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        assertThat(result, is(notNullValue()));
        verify(hearingRepository).findAllocatedHearingsForCourtCalendar(
                eq(COURT_CENTRE_ID),
                eq(COURT_ROOM_ID),
                eq(UUID.fromString(AUTHORITY_ID)),
                eq(HEARING_TYPE_ID),
                eq(JURISDICTION_TYPE.toString()),
                eq(SEARCH_DATE),
                eq(SEARCH_DATE),
                any(Instant.class),
                eq(0),
                eq(10)
        );
    }

    private List<Hearing> hearingsJson(String allocated) {
        String testJsonString = "{ \"allocated\":\"" + allocated + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}], \"hearingDays\" : [{\"hearingDate\": \"HEARING_DATE1\"}, {\"hearingDate\": \"HEARING_DATE2\"}] }";
        LocalDate today = LocalDate.now();
        LocalDate tmrw = today.plusDays(1);
        testJsonString = testJsonString.replace("HEARING_DATE1", today.toString()).replace("HEARING_DATE2", tmrw.toString());
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        hearing1.setAllocated(true);
        hearing1.setTotalCount(Long.valueOf(2));
        final HearingDays hd1 = new HearingDays();
        hd1.setHearingDate(today);
        final HearingDays hd2 = new HearingDays();
        hd2.setHearingDate(tmrw);
        hearing1.getHearingDays().add(hd1);
        hearing1.getHearingDays().add(hd2);

        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        hearing2.setAllocated(true);
        hearing2.getHearingDays().add(hd1);
        hearing2.getHearingDays().add(hd2);

        return newArrayList(hearing1, hearing2);
    }

    private List<Hearing> hearingsJson(String allocated, boolean possibleDisqualification) {
        final String testJsonString = "{ \"allocated\":\"" + allocated + "\",\"isPossibleDisqualification\":" + possibleDisqualification + ", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        hearing1.setTotalCount(Long.valueOf(1));
        hearing1.setAllocated(true);
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
        hearing1.setTotalCount(2L);
        hearing1.setAllocated(true);
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonStringForUnallocated));
        hearing2.setAllocated(false);
        hearing2.setTotalCount(2L);
        return newArrayList(hearing1, hearing2);

    }

    private List<Hearing> hearingJsonForWeekCommencingAndPossibleDisqualification() {
        final String testJsonStringForAllocated = "{\n" +
                "\t\t\"id\": \"54482cb7-31aa-4c64-8656-3be6e3a4d158\",\n" +
                "\t\t\"allocated\": \"true\",\n" +
                "\t\t\"isPossibleDisqualification\": \"true\",\n" +
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
                "\t\t\"isPossibleDisqualification\": \"true\",\n" +
                "\t\t\"weekCommencingStartDate\": \"2019-10-13\",\n" +
                "\t\t\"weekCommencingEndDate\": \"2019-10-25\",\n" +
                "\t\t\"listedCases\": [{\n" +
                "\t\t}],\n" +
                "\t\t\"courtApplications\": [{\n" +
                "\t\t}]\n" +
                "\t}";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonStringForAllocated));
        hearing1.setTotalCount(1L);
        hearing1.setAllocated(false);
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonStringForUnallocated));
        return newArrayList(hearing1, hearing2);

    }

    private List<Notes> createNotesList() {
        return newArrayList(new Notes(UUID.randomUUID(), fromString("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18"), LocalDates.from("2020-09-03"), "Note 1"));
    }
}
