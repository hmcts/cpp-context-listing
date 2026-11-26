package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.parse;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final String TRIAL_HEARING_TYPE_ID = "bf8155e1-90b9-4080-b133-bfbad895d6e4";
    private static final Set<String> hearingTypeIds = new HashSet<>(Arrays.asList(TRIAL_HEARING_TYPE_ID));

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
        // hearingTypeId is supplied on the query below and is now enforced listing-side against
        // the viewstore typeId (matchesHearingType), so the mocked hearings must carry a matching
        // typeId or they would be filtered out of the enriched result.
        hearings.forEach(hearing -> hearing.setTypeId(HEARING_TYPE_ID));
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
                        Optional.of(BUSINESS_TYPE),
                        Optional.of(MAGISTRATES_TYPE.toString()),
                        "FINAL",
                        "ADULT,YOUTH", 50, 1)).thenReturn(response);

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

    @Test
    void rangeSearchCourtCalendarWithCrownAllocatedAndOuCodeAndBusinessTypeShouldUseCourtScheduler() {
        final List<Hearing> mockHearings = hearingsJson(ALLOCATEDSTR);
        final List<IdResponse> hearingIds = new ArrayList<>();
        mockHearings.forEach(h -> hearingIds.add(new IdResponse(h.getId(), UUID.randomUUID(), LocalDate.now(), 1, 1)));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, mockHearings.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                "C01CY00",
                Optional.empty(),
                COURT_ROOM_ID.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString(),
                Optional.empty(),
                Optional.of("GENC"),
                Optional.of(JURISDICTION_TYPE.toString()),
                "FINAL",
                "ADULT,YOUTH",
                40,
                1
        )).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(mockHearings);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, "GENC")
                        .add(COURT_SESSION_QUERY_PARAMETER, "Any")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        assertThat(result, is(notNullValue()));
        verify(courtSchedulerServiceAdapter).getCourtSchedulerHearings(
                "C01CY00",
                Optional.empty(),
                COURT_ROOM_ID.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString(),
                Optional.empty(),
                Optional.of("GENC"),
                Optional.of(JURISDICTION_TYPE.toString()),
                "FINAL",
                "ADULT,YOUTH",
                40,
                1);
    }

    @Test
    void rangeSearchCourtCalendarWithCrownAllocatedAndOuCodeAndHearingTypeIdShouldPassHearingTypeIdToCourtScheduler() {
        final List<Hearing> mockHearings = hearingsJson(ALLOCATEDSTR);
        final List<IdResponse> hearingIds = new ArrayList<>();
        mockHearings.forEach(h -> hearingIds.add(new IdResponse(h.getId(), UUID.randomUUID(), LocalDate.now(), 1, 1)));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, mockHearings.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                "C01CY00",
                Optional.empty(),
                COURT_ROOM_ID.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString(),
                Optional.empty(),
                Optional.of("GENC"),
                Optional.of(JURISDICTION_TYPE.toString()),
                "FINAL",
                "ADULT,YOUTH",
                40,
                1
        )).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(mockHearings);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, "GENC")
                        .add(COURT_SESSION_QUERY_PARAMETER, "Any")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        assertThat(result, is(notNullValue()));
        verify(courtSchedulerServiceAdapter).getCourtSchedulerHearings(
                "C01CY00",
                Optional.empty(),
                COURT_ROOM_ID.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString(),
                Optional.empty(),
                Optional.of("GENC"),
                Optional.of(JURISDICTION_TYPE.toString()),
                "FINAL",
                "ADULT,YOUTH",
                40,
                1);
    }

    @Test
    void rangeSearchCourtCalendarShouldSplitMultiDayCrownHearingIntoDistinctPerDayRows() {
        final UUID multiDayId = randomUUID();
        final String props = "{ \"allocated\":\"true\", \"startDate\": \"2026-07-14\", \"courtRoomId\": \"" + COURT_ROOM_ID
                + "\", \"courtApplications\": [{}], \"listedCases\": [{}], \"hearingDays\": ["
                + "{\"hearingDate\": \"2026-07-14\"}, {\"hearingDate\": \"2026-07-15\"}, {\"hearingDate\": \"2026-07-16\"}] }";
        final Hearing multiDay = new Hearing(multiDayId, JacksonUtil.toJsonNode(props));
        multiDay.setAllocated(true);

        final List<IdResponse> hearingIds = new ArrayList<>();
        hearingIds.add(new IdResponse(multiDayId, randomUUID(), LocalDate.parse("2026-07-14"), 3, 1));
        hearingIds.add(new IdResponse(multiDayId, randomUUID(), LocalDate.parse("2026-07-15"), 3, 2));
        hearingIds.add(new IdResponse(multiDayId, randomUUID(), LocalDate.parse("2026-07-16"), 3, 3));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, hearingIds.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(newArrayList(multiDay));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_SESSION_QUERY_PARAMETER, "AD")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, "2026-07-14")
                        .add(END_DATE_QUERY_PARAMETER, "2026-07-17")
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject payload = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();
        final JsonArray hearings = payload.getJsonArray("hearings");

        final java.util.Set<String> dates = new java.util.HashSet<>();
        for (int i = 0; i < hearings.size(); i++) {
            dates.add(hearings.getJsonObject(i).getJsonArray("hearingDays").getJsonObject(0).getString("hearingDate"));
        }
        assertThat(hearings.size(), is(3));
        assertThat(dates.size(), is(3)); // aliasing bug collapses all rows onto the last day -> size 1
        assertThat(dates.contains("2026-07-14") && dates.contains("2026-07-15") && dates.contains("2026-07-16"), is(true));
        assertThat(payload.getInt("results"), is(3));
    }

    @Test
    void rangeSearchCourtCalendarShouldExcludeDraftUnallocatedHearingsFromAllocatedView() {
        // Draft (unallocated) exclusion moved from listing-side enrich to the courtscheduler call
        // itself: the allocated court-calendar view now requests status=FINAL so drafts never
        // come back in the hearingId list to begin with. This test pins that contract at the
        // adapter-invocation boundary rather than asserting listing-side enrich drops anything
        // (enrich no longer filters on allocated at all - see matchesHearingType).
        final UUID allocatedId = randomUUID();
        final String allocatedProps = "{ \"allocated\":\"true\", \"startDate\": \"2026-07-14\", \"courtRoomId\": \"" + COURT_ROOM_ID
                + "\", \"courtApplications\": [{}], \"listedCases\": [{}], \"hearingDays\": [{\"hearingDate\": \"2026-07-14\"}] }";
        final Hearing allocated = new Hearing(allocatedId, JacksonUtil.toJsonNode(allocatedProps));
        allocated.setAllocated(true);

        final List<IdResponse> hearingIds = new ArrayList<>();
        hearingIds.add(new IdResponse(allocatedId, randomUUID(), LocalDate.parse("2026-07-14"), 1, 1));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, hearingIds.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(newArrayList(allocated));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_SESSION_QUERY_PARAMETER, "AD")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, "2026-07-14")
                        .add(END_DATE_QUERY_PARAMETER, "2026-07-17")
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        rangeSearchQuery.rangeSearchCourtCalendar(query);

        verify(courtSchedulerServiceAdapter).getCourtSchedulerHearings(
                eq("C01CY00"), any(), any(), any(), any(), any(), any(), any(), eq("FINAL"), any(), any(), any());
    }

    @Test
    void rangeSearchCourtCalendarAllocatedShouldRequestIsDraftFalseFromCourtScheduler() {
        final List<Hearing> mockHearings = hearingsJson(ALLOCATEDSTR);
        final List<IdResponse> hearingIds = new ArrayList<>();
        mockHearings.forEach(h -> hearingIds.add(new IdResponse(h.getId(), UUID.randomUUID(), LocalDate.now(), 1, 1)));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, mockHearings.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(mockHearings);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_SESSION_QUERY_PARAMETER, "AD")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        rangeSearchQuery.rangeSearchCourtCalendar(query);

        // allocated=true on the court-calendar view must always ask courtscheduler to exclude drafts.
        verify(courtSchedulerServiceAdapter).getCourtSchedulerHearings(
                eq("C01CY00"), any(), any(), any(), any(), any(), any(), any(), eq("FINAL"), any(), any(), any());
    }

    @Test
    void rangeSearchCourtCalendarWithMatchingHearingTypeIdShouldReturnHearing() {
        final UUID hearingId = randomUUID();
        final String props = "{ \"allocated\":\"true\", \"startDate\": \"2026-07-14\", \"courtRoomId\": \"" + COURT_ROOM_ID
                + "\", \"courtApplications\": [{}], \"listedCases\": [{}], \"hearingDays\": [{\"hearingDate\": \"2026-07-14\"}] }";
        final Hearing hearing = new Hearing(hearingId, JacksonUtil.toJsonNode(props));
        hearing.setAllocated(true);
        hearing.setTypeId(HEARING_TYPE_ID);

        final List<IdResponse> hearingIds = List.of(new IdResponse(hearingId, randomUUID(), LocalDate.parse("2026-07-14"), 1, 1));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, hearingIds.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(newArrayList(hearing));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_SESSION_QUERY_PARAMETER, "AD")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(START_DATE_QUERY_PARAMETER, "2026-07-14")
                        .add(END_DATE_QUERY_PARAMETER, "2026-07-17")
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject payload = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        assertThat(payload.getJsonArray("hearings").size(), is(1));
        assertThat(payload.getInt("results"), is(1));
    }

    @Test
    void rangeSearchCourtCalendarWithNonMatchingHearingTypeIdShouldFilterOutHearing() {
        final UUID hearingId = randomUUID();
        final String props = "{ \"allocated\":\"true\", \"startDate\": \"2026-07-14\", \"courtRoomId\": \"" + COURT_ROOM_ID
                + "\", \"courtApplications\": [{}], \"listedCases\": [{}], \"hearingDays\": [{\"hearingDate\": \"2026-07-14\"}] }";
        final Hearing hearing = new Hearing(hearingId, JacksonUtil.toJsonNode(props));
        hearing.setAllocated(true);
        hearing.setTypeId(randomUUID()); // deliberately different from the queried hearingTypeId

        final List<IdResponse> hearingIds = List.of(new IdResponse(hearingId, randomUUID(), LocalDate.parse("2026-07-14"), 1, 1));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, hearingIds.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(newArrayList(hearing));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_SESSION_QUERY_PARAMETER, "AD")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(START_DATE_QUERY_PARAMETER, "2026-07-14")
                        .add(END_DATE_QUERY_PARAMETER, "2026-07-17")
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject payload = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        assertThat(payload.getJsonArray("hearings").size(), is(0));
        assertThat(payload.getInt("results"), is(1)); // courtscheduler's raw count is unfiltered; only the enriched rows drop the mismatch
    }

    @Test
    void rangeSearchCourtCalendarWithNoHearingTypeIdShouldNotFilterByType() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final String props = "{ \"allocated\":\"true\", \"startDate\": \"2026-07-14\", \"courtRoomId\": \"" + COURT_ROOM_ID
                + "\", \"courtApplications\": [{}], \"listedCases\": [{}], \"hearingDays\": [{\"hearingDate\": \"2026-07-14\"}] }";
        final Hearing hearing1 = new Hearing(hearingId1, JacksonUtil.toJsonNode(props));
        hearing1.setAllocated(true);
        hearing1.setTypeId(HEARING_TYPE_ID);
        final Hearing hearing2 = new Hearing(hearingId2, JacksonUtil.toJsonNode(props));
        hearing2.setAllocated(true);
        hearing2.setTypeId(randomUUID());

        final List<IdResponse> hearingIds = List.of(
                new IdResponse(hearingId1, randomUUID(), LocalDate.parse("2026-07-14"), 1, 1),
                new IdResponse(hearingId2, randomUUID(), LocalDate.parse("2026-07-14"), 1, 1));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, hearingIds.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(newArrayList(hearing1, hearing2));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_SESSION_QUERY_PARAMETER, "AD")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, "2026-07-14")
                        .add(END_DATE_QUERY_PARAMETER, "2026-07-17")
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject payload = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        assertThat(payload.getJsonArray("hearings").size(), is(2));
    }

    @Test
    void rangeSearchCourtCalendarShouldDedupeMultiDayHearingIdsBeforeFetchingFromViewstore() {
        final UUID multiDayId = randomUUID();
        final String props = "{ \"allocated\":\"true\", \"startDate\": \"2026-07-14\", \"courtRoomId\": \"" + COURT_ROOM_ID
                + "\", \"courtApplications\": [{}], \"listedCases\": [{}], \"hearingDays\": ["
                + "{\"hearingDate\": \"2026-07-14\"}, {\"hearingDate\": \"2026-07-15\"}] }";
        final Hearing multiDay = new Hearing(multiDayId, JacksonUtil.toJsonNode(props));
        multiDay.setAllocated(true);

        final List<IdResponse> hearingIds = new ArrayList<>();
        hearingIds.add(new IdResponse(multiDayId, randomUUID(), LocalDate.parse("2026-07-14"), 2, 1));
        hearingIds.add(new IdResponse(multiDayId, randomUUID(), LocalDate.parse("2026-07-15"), 2, 2));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, hearingIds.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(newArrayList(multiDay));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_SESSION_QUERY_PARAMETER, "AD")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, "2026-07-14")
                        .add(END_DATE_QUERY_PARAMETER, "2026-07-17")
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject payload = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        // 2 IdResponses share the same hearingId (a 2-day multiday hearing) -> the repository must
        // be asked for only 1 distinct id, while the response still carries one row per day.
        verify(hearingRepository).findAllCourtSchedulerHearingByIds(argThat(ids -> ids.size() == 1));
        assertThat(payload.getJsonArray("hearings").size(), is(2));
    }

    // -----------------------------------------------------------------------
    // rangeSearchCourtCalendar – missing branch coverage
    // -----------------------------------------------------------------------

    @Test
    void rangeSearchCourtCalendarWithCrownAndBusinessTypeShouldUseCourtScheduler() {
        final List<Hearing> mockHearings = hearingsJson(ALLOCATEDSTR);
        final List<IdResponse> hearingIds = new ArrayList<>();
        mockHearings.forEach(h -> hearingIds.add(new IdResponse(h.getId(), UUID.randomUUID(), LocalDate.now(), 1, 1)));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, mockHearings.size(), 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                "C01CY00",
                Optional.empty(),
                COURT_ROOM_ID.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString(),
                Optional.empty(),
                Optional.of("GENC"),
                Optional.of(JURISDICTION_TYPE.toString()),
                "FINAL",
                "ADULT,YOUTH",
                40,
                1
        )).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(mockHearings);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, "GENC")
                        .add(COURT_SESSION_QUERY_PARAMETER, "Any")
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(OU_CODE_QUERY_PARAMETER, "C01CY00")
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        assertThat(result, is(notNullValue()));
        assertThat(result.metadata().name(), is("listing.search.hearings"));
        verify(courtSchedulerServiceAdapter).getCourtSchedulerHearings(
                "C01CY00",
                Optional.empty(),
                COURT_ROOM_ID.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString(),
                Optional.empty(),
                Optional.of("GENC"),
                Optional.of(JURISDICTION_TYPE.toString()),
                "FINAL",
                "ADULT,YOUTH",
                40,
                1);
    }

    @Test
    void rangeSearchCourtCalendarUnallocatedShouldUseFindHearings() {
        final List<Hearing> mockHearings = hearingsJson(ALLOCATEDSTR);
        when(hearingRepository.findHearings(
                false,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                UUID.fromString(AUTHORITY_ID),
                HEARING_TYPE_ID,
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE, 0, 50)
        ).thenReturn(mockHearings);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
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

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        assertThat(result, is(notNullValue()));
        verify(hearingRepository).findHearings(
                false,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                UUID.fromString(AUTHORITY_ID),
                HEARING_TYPE_ID,
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE, 0, 50);
    }

    @Test
    void rangeSearchCourtCalendarWithWeekCommencingShouldUseFindHearingsByWeekCommencingRange() {
        final List<Hearing> mockHearings = hearingJsonForWeekCommencing();
        doReturn(mockHearings)
                .when(hearingRepository)
                .findHearingsByWeekCommencingRange(
                        null,
                        null,
                        AUTHORITY_ID,
                        null,
                        null,
                        WEEK_COMMENCING_START_DATE,
                        WEEK_COMMENCING_END_DATE, 0, 50);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        assertThat(result, is(notNullValue()));
        verify(hearingRepository).findHearingsByWeekCommencingRange(
                null,
                null,
                AUTHORITY_ID,
                null,
                null,
                WEEK_COMMENCING_START_DATE,
                WEEK_COMMENCING_END_DATE, 0, 50);
    }

    @Test
    void rangeSearchCourtCalendarWithEmptyHearingsReturnsZeroResults() {
        when(hearingRepository.findAllocatedHearingsForCourtCalendar(
                eq(COURT_CENTRE_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(SEARCH_DATE),
                eq(SEARCH_DATE),
                eq(null),
                eq(0),
                eq(50)
        )).thenReturn(List.of());

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        assertThat(result.payloadAsJsonObject().getInt("results"), is(0));
        assertThat(result.payloadAsJsonObject().getJsonArray("hearings").size(), is(0));
    }

    @Test
    void rangeSearchCourtCalendarWithInvalidExactHearingStartDateTimeThrowsBadRequest() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(EXACT_HEARING_START_DATETIME, "not-a-valid-datetime")
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () -> rangeSearchQuery.rangeSearchCourtCalendar(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.containsString("Invalid startDateTime format"));
    }

    // -----------------------------------------------------------------------
    // rangeSearchCourtCalendar – businessType / courtSession routing
    //   allocated=true            -> courtscheduler status=FINAL (any jurisdiction)
    //   allocated=false + CROWN   -> courtscheduler status=DRAFT
    //   allocated=false + other   -> 400
    //   no ouCode                 -> 400
    // -----------------------------------------------------------------------

    private static final String CROWN_OU_CODE = "C01CY00";
    private static final String PTPH = "PTPH";
    private static final String GENC = "GENC";

    static Stream<Arguments> courtCalendarSessionFilterRejections() {
        final String unallocatedNotCrown = RangeSearchQuery.COURT_SESSION_OR_BUSINESS_TYPE_UNALLOCATED_NOT_CROWN;
        final String withoutOuCode = RangeSearchQuery.COURT_SESSION_OR_BUSINESS_TYPE_WITHOUT_OU_CODE;
        return Stream.of(
                Arguments.of("MAGISTRATES", false, OU_CODE, BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE, unallocatedNotCrown),
                Arguments.of("MAGISTRATES", false, OU_CODE, COURT_SESSION_QUERY_PARAMETER, AM, unallocatedNotCrown),
                Arguments.of("MAGISTRATES", false, null, BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE, unallocatedNotCrown),
                Arguments.of(null, false, OU_CODE, BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE, unallocatedNotCrown),
                Arguments.of(null, false, OU_CODE, COURT_SESSION_QUERY_PARAMETER, AM, unallocatedNotCrown),
                Arguments.of("MAGISTRATES", true, null, BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE, withoutOuCode),
                Arguments.of("MAGISTRATES", true, null, COURT_SESSION_QUERY_PARAMETER, AM, withoutOuCode),
                Arguments.of("CROWN", true, null, BUSINESS_TYPE_QUERY_PARAMETER, PTPH, withoutOuCode),
                Arguments.of("CROWN", true, null, COURT_SESSION_QUERY_PARAMETER, AM, withoutOuCode),
                Arguments.of("CROWN", false, null, BUSINESS_TYPE_QUERY_PARAMETER, PTPH, withoutOuCode),
                Arguments.of("CROWN", false, null, COURT_SESSION_QUERY_PARAMETER, AM, withoutOuCode)
        );
    }

    @ParameterizedTest(name = "[{index}] jurisdiction={0} allocated={1} ouCode={2} {3}={4} -> 400")
    @MethodSource("courtCalendarSessionFilterRejections")
    void rangeSearchCourtCalendarWithSessionFilterThatCannotBeHonouredShouldThrowBadRequest(final String jurisdictionType,
                                                                                            final boolean allocated,
                                                                                            final String ouCode,
                                                                                            final String filterParameter,
                                                                                            final String filterValue,
                                                                                            final String expectedMessage) {
        final var payload = createObjectBuilder()
                .add(ALLOCATED_QUERY_PARAMETER, allocated)
                .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                .add(filterParameter, filterValue)
                .add(PAGE_SIZE, 40)
                .add(PAGE_NUMBER, 1);
        if (jurisdictionType != null) {
            payload.add(JURISDICTION_TYPE_QUERY_PARAMETER, jurisdictionType);
        }
        if (ouCode != null) {
            payload.add(OU_CODE_QUERY_PARAMETER, ouCode);
        }
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("event.name"), payload.build());

        final BadRequestException thrown = assertThrows(BadRequestException.class, () -> rangeSearchQuery.rangeSearchCourtCalendar(query));

        assertThat(thrown.getMessage(), is(expectedMessage));
        // rejected before any lookup: never silently falls back to an unfiltered viewstore search
        verifyNoInteractions(courtSchedulerServiceAdapter, hearingRepository);
    }

    static Stream<Arguments> crownUnallocatedCourtSchedulerWindows() {
        final String wcStart = WEEK_COMMENCING_START_DATE.toString();
        final String wcEnd = WEEK_COMMENCING_END_DATE.toString();
        final String searchDate = SEARCH_DATE.toString();
        return Stream.of(
                Arguments.of(wcStart, wcEnd, null, null, wcStart, wcEnd),
                Arguments.of(wcStart, null, null, null, wcStart, LATEST_SEARCH_DATE),
                Arguments.of(null, null, searchDate, searchDate, searchDate, searchDate),
                Arguments.of(null, null, null, null, EARLIEST_SEARCH_DATE, LATEST_SEARCH_DATE)
        );
    }

    @ParameterizedTest(name = "[{index}] wc={0}..{1} range={2}..{3} -> courtscheduler {4}..{5}")
    @MethodSource("crownUnallocatedCourtSchedulerWindows")
    void rangeSearchCourtCalendarCrownUnallocatedWithBusinessTypeShouldSearchDraftSessionsInCourtScheduler(final String weekCommencingStartDate,
                                                                                                           final String weekCommencingEndDate,
                                                                                                           final String startDate,
                                                                                                           final String endDate,
                                                                                                           final String expectedSessionStartDate,
                                                                                                           final String expectedSessionEndDate) {
        final Hearing unallocated = unallocatedCrownHearing(WEEK_COMMENCING_START_DATE, null);
        final HearingIdsResponse response = new HearingIdsResponse(
                List.of(new IdResponse(unallocated.getId(), randomUUID(), WEEK_COMMENCING_START_DATE, 1, 1)), 1, 1);
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                CROWN_OU_CODE, Optional.empty(), null, expectedSessionStartDate, expectedSessionEndDate, Optional.empty(),
                Optional.of(PTPH), Optional.of(JURISDICTION_TYPE.toString()), "DRAFT", "ADULT,YOUTH", 40, 1))
                .thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(List.of(unallocated.getId()))).thenReturn(newArrayList(unallocated));

        final var payload = createObjectBuilder()
                .add(ALLOCATED_QUERY_PARAMETER, false)
                .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                .add(OU_CODE_QUERY_PARAMETER, CROWN_OU_CODE)
                .add(BUSINESS_TYPE_QUERY_PARAMETER, PTPH)
                .add(PAGE_SIZE, 40)
                .add(PAGE_NUMBER, 1);
        addIfPresent(payload, WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, weekCommencingStartDate);
        addIfPresent(payload, WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, weekCommencingEndDate);
        addIfPresent(payload, START_DATE_QUERY_PARAMETER, startDate);
        addIfPresent(payload, END_DATE_QUERY_PARAMETER, endDate);
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("event.name"), payload.build());

        final JsonObject result = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        assertThat(result.getInt("results"), is(1));
        assertThat(result.getJsonArray("hearings").size(), is(1));
        assertThat(result.getJsonArray("hearings").getJsonObject(0).getString("id"), is(unallocated.getId().toString()));
        // only the courtscheduler-backed id lookup: no unfiltered viewstore range query
        verify(hearingRepository).findAllCourtSchedulerHearingByIds(List.of(unallocated.getId()));
        verifyNoMoreInteractions(hearingRepository);
    }

    @Test
    void rangeSearchCourtCalendarCrownUnallocatedWithCourtSessionShouldSearchDraftSessionsInCourtScheduler() {
        final Hearing unallocated = unallocatedCrownHearing(SEARCH_DATE, null);
        final HearingIdsResponse response = new HearingIdsResponse(
                List.of(new IdResponse(unallocated.getId(), randomUUID(), SEARCH_DATE, 1, 1)), 1, 1);
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                CROWN_OU_CODE, Optional.of(AM), COURT_ROOM_ID.toString(), SEARCH_DATE.toString(), SEARCH_DATE.toString(), Optional.empty(),
                Optional.empty(), Optional.of(JURISDICTION_TYPE.toString()), "DRAFT", "ADULT,YOUTH", 40, 1))
                .thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(List.of(unallocated.getId()))).thenReturn(newArrayList(unallocated));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(OU_CODE_QUERY_PARAMETER, CROWN_OU_CODE)
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(COURT_SESSION_QUERY_PARAMETER, AM)
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject result = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        assertThat(result.getJsonArray("hearings").size(), is(1));
        assertThat(result.getJsonArray("hearings").getJsonObject(0).getString("id"), is(unallocated.getId().toString()));
    }

    @Test
    void rangeSearchCourtCalendarCrownUnallocatedWithBusinessTypeShouldStillFilterByHearingTypeListingSide() {
        final UUID otherHearingTypeId = randomUUID();
        final Hearing matching = unallocatedCrownHearing(WEEK_COMMENCING_START_DATE, HEARING_TYPE_ID);
        final Hearing otherType = unallocatedCrownHearing(WEEK_COMMENCING_START_DATE, otherHearingTypeId);
        final HearingIdsResponse response = new HearingIdsResponse(List.of(
                new IdResponse(matching.getId(), randomUUID(), WEEK_COMMENCING_START_DATE, 1, 1),
                new IdResponse(otherType.getId(), randomUUID(), WEEK_COMMENCING_START_DATE, 1, 1)), 2, 1);
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                eq(CROWN_OU_CODE), any(), any(), any(), any(), any(), eq(Optional.of(PTPH)), any(), eq("DRAFT"), any(), any(), any()))
                .thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(newArrayList(matching, otherType));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(OU_CODE_QUERY_PARAMETER, CROWN_OU_CODE)
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, PTPH)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonArray hearings = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject().getJsonArray("hearings");

        assertThat(hearings.size(), is(1));
        assertThat(hearings.getJsonObject(0).getString("id"), is(matching.getId().toString()));
    }

    @Test
    void rangeSearchCourtCalendarCrownUnallocatedWithBusinessTypeShouldReturnEachHearingOnceWithAllItsHearingDays() {
        final LocalDate monday = LocalDate.parse("2026-10-12");
        final Hearing singleDay = multiDayUnallocatedCrownHearing(monday);
        final Hearing fourDay = multiDayUnallocatedCrownHearing(monday, monday.plusDays(1), monday.plusDays(2), monday.plusDays(3));
        // courtscheduler returns one row per DRAFT hearing day in the window: the four-day hearing's
        // Thursday falls outside it
        final HearingIdsResponse draftHearingDays = new HearingIdsResponse(List.of(
                new IdResponse(singleDay.getId(), randomUUID(), monday, 1, 1),
                new IdResponse(fourDay.getId(), randomUUID(), monday, 4, 1),
                new IdResponse(fourDay.getId(), randomUUID(), monday.plusDays(1), 4, 2),
                new IdResponse(fourDay.getId(), randomUUID(), monday.plusDays(2), 4, 3)), 4, 1);
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                CROWN_OU_CODE, Optional.empty(), null, monday.toString(), monday.plusDays(2).toString(), Optional.empty(),
                Optional.of(GENC), Optional.of(JURISDICTION_TYPE.toString()), "DRAFT", "ADULT,YOUTH", 40, 1))
                .thenReturn(draftHearingDays);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(List.of(singleDay.getId(), fourDay.getId())))
                .thenReturn(newArrayList(fourDay, singleDay));

        final JsonObject result = rangeSearchQuery.rangeSearchCourtCalendar(
                unallocatedCrownBusinessTypeQuery(monday, monday.plusDays(2), 40, 1)).payloadAsJsonObject();

        assertThat(result.getInt("results"), is(2));
        assertThat(result.getInt("pageCount"), is(1));
        final JsonArray hearings = result.getJsonArray("hearings");
        assertThat(hearings.size(), is(2));
        assertThat(hearings.getJsonObject(0).getString("id"), is(singleDay.getId().toString()));
        final JsonObject fourDayHearing = hearings.getJsonObject(1);
        assertThat(fourDayHearing.getString("id"), is(fourDay.getId().toString()));
        assertThat(fourDayHearing.getJsonArray("hearingDays").size(), is(4));
        assertThat(fourDayHearing.getJsonArray("hearingDays").getJsonObject(3).getString("hearingDate"), is(monday.plusDays(3).toString()));
        // hearingDayCount/hearingDayPosition mark the allocated calendar's one-row-per-hearing-day shape
        assertThat(fourDayHearing.containsKey("hearingDayCount"), is(false));
        assertThat(fourDayHearing.containsKey("hearingDayPosition"), is(false));
    }

    @Test
    void rangeSearchCourtCalendarCrownUnallocatedWithBusinessTypeShouldPageByHearingNotByCourtSchedulerHearingDay() {
        final LocalDate monday = LocalDate.parse("2026-10-12");
        final Hearing threeDay = multiDayUnallocatedCrownHearing(monday, monday.plusDays(1), monday.plusDays(2));
        final Hearing singleDay = multiDayUnallocatedCrownHearing(monday.plusDays(2));
        final List<IdResponse> allDraftHearingDays = List.of(
                new IdResponse(threeDay.getId(), randomUUID(), monday, 3, 1),
                new IdResponse(threeDay.getId(), randomUUID(), monday.plusDays(1), 3, 2),
                new IdResponse(threeDay.getId(), randomUUID(), monday.plusDays(2), 3, 3),
                new IdResponse(singleDay.getId(), randomUUID(), monday.plusDays(2), 1, 1));
        // the caller's page of hearing days holds only part of the window, so every hearing day is fetched
        doReturn(4L).when(paginationParameterFactory).getMaxPageSize();
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                CROWN_OU_CODE, Optional.empty(), null, monday.toString(), monday.plusDays(4).toString(), Optional.empty(),
                Optional.of(GENC), Optional.of(JURISDICTION_TYPE.toString()), "DRAFT", "ADULT,YOUTH", 1, 1))
                .thenReturn(new HearingIdsResponse(allDraftHearingDays.subList(0, 1), 4, 4));
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                CROWN_OU_CODE, Optional.empty(), null, monday.toString(), monday.plusDays(4).toString(), Optional.empty(),
                Optional.of(GENC), Optional.of(JURISDICTION_TYPE.toString()), "DRAFT", "ADULT,YOUTH", 4, 1))
                .thenReturn(new HearingIdsResponse(allDraftHearingDays, 4, 1));
        when(hearingRepository.findAllCourtSchedulerHearingByIds(List.of(threeDay.getId(), singleDay.getId())))
                .thenReturn(newArrayList(threeDay, singleDay));

        final JsonObject result = rangeSearchQuery.rangeSearchCourtCalendar(
                unallocatedCrownBusinessTypeQuery(monday, monday.plusDays(4), 1, 2)).payloadAsJsonObject();

        assertThat(result.getInt("results"), is(2));
        assertThat(result.getInt("pageCount"), is(2));
        assertThat(result.getJsonArray("hearings").size(), is(1));
        assertThat(result.getJsonArray("hearings").getJsonObject(0).getString("id"), is(singleDay.getId().toString()));
    }

    @Test
    void rangeSearchCourtCalendarCrownUnallocatedWithBusinessTypeShouldRejectWindowWithMoreHearingDaysThanTheMaxPageSize() {
        final LocalDate monday = LocalDate.parse("2026-10-12");
        doReturn(4L).when(paginationParameterFactory).getMaxPageSize();
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                CROWN_OU_CODE, Optional.empty(), null, monday.toString(), monday.plusDays(4).toString(), Optional.empty(),
                Optional.of(GENC), Optional.of(JURISDICTION_TYPE.toString()), "DRAFT", "ADULT,YOUTH", 1, 1))
                .thenReturn(new HearingIdsResponse(List.of(new IdResponse(randomUUID(), randomUUID(), monday, 1, 1)), 5, 5));

        final JsonEnvelope query = unallocatedCrownBusinessTypeQuery(monday, monday.plusDays(4), 1, 1);
        final BadRequestException thrown = assertThrows(BadRequestException.class, () -> rangeSearchQuery.rangeSearchCourtCalendar(query));

        assertThat(thrown.getMessage(), is(String.format(RangeSearchQuery.COURT_SESSION_OR_BUSINESS_TYPE_UNALLOCATED_WINDOW_TOO_LARGE, 5, 4)));
        // never asks courtscheduler for more hearing days than the cap
        verify(courtSchedulerServiceAdapter).getCourtSchedulerHearings(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(hearingRepository);
    }

    @Test
    void rangeSearchCourtCalendarAllocatedWithBusinessTypeAndWeekCommencingShouldSearchFinalSessionsInThatWindow() {
        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                OU_CODE, Optional.empty(), null, WEEK_COMMENCING_START_DATE.toString(), WEEK_COMMENCING_END_DATE.toString(), Optional.empty(),
                Optional.of(BUSINESS_TYPE), Optional.of(MAGISTRATES_TYPE.toString()), "FINAL", "ADULT,YOUTH", 40, 1))
                .thenReturn(new HearingIdsResponse(List.of(), 0, 0));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, MAGISTRATES_TYPE.toString())
                        .add(OU_CODE_QUERY_PARAMETER, OU_CODE)
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject result = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        assertThat(result.getInt("results"), is(0));
        assertThat(result.getJsonArray("hearings").size(), is(0));
        verifyNoInteractions(hearingRepository);
    }

    @ParameterizedTest(name = "[{index}] jurisdiction={0}")
    @ValueSource(strings = {"CROWN", "MAGISTRATES"})
    void rangeSearchCourtCalendarUnallocatedWithCourtSessionAnyIsNotAFilterAndShouldSearchViewstore(final String jurisdictionType) {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, jurisdictionType)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_SESSION_QUERY_PARAMETER, "Any")
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .add(PAGE_SIZE, 40)
                        .add(PAGE_NUMBER, 1)
                        .build());

        final JsonObject result = rangeSearchQuery.rangeSearchCourtCalendar(query).payloadAsJsonObject();

        assertThat(result.getInt("results"), is(0));
        verify(hearingRepository).findUnallocatedHearingsByWeekCommencingRange(
                COURT_CENTRE_ID, null, null, null, jurisdictionType,
                WEEK_COMMENCING_START_DATE, WEEK_COMMENCING_END_DATE, false, 0, 40);
        verifyNoInteractions(courtSchedulerServiceAdapter);
    }

    private static void addIfPresent(final JsonObjectBuilder builder, final String name, final String value) {
        if (value != null) {
            builder.add(name, value);
        }
    }

    private static Hearing unallocatedCrownHearing(final LocalDate hearingDate, final UUID hearingTypeId) {
        final UUID hearingId = randomUUID();
        final String json = "{ \"id\": \"" + hearingId + "\", \"allocated\": false, \"jurisdictionType\": \"CROWN\", \"startDate\": \"" + hearingDate + "\", "
                + "\"courtApplications\": [{}], \"listedCases\": [{}], \"hearingDays\": [{\"hearingDate\": \"" + hearingDate + "\"}] }";
        final Hearing hearing = new Hearing(hearingId, JacksonUtil.toJsonNode(json));
        hearing.setAllocated(false);
        hearing.setTypeId(hearingTypeId);
        return hearing;
    }

    private static Hearing multiDayUnallocatedCrownHearing(final LocalDate... hearingDates) {
        final UUID hearingId = randomUUID();
        final String hearingDays = Arrays.stream(hearingDates)
                .map(hearingDate -> "{\"hearingDate\": \"" + hearingDate + "\", \"isDraft\": true}")
                .collect(joining(", "));
        final String json = "{ \"id\": \"" + hearingId + "\", \"allocated\": false, \"jurisdictionType\": \"CROWN\", \"startDate\": \"" + hearingDates[0] + "\", "
                + "\"endDate\": \"" + hearingDates[hearingDates.length - 1] + "\", \"listedCases\": [{}], \"hearingDays\": [" + hearingDays + "] }";
        final Hearing hearing = new Hearing(hearingId, JacksonUtil.toJsonNode(json));
        hearing.setAllocated(false);
        return hearing;
    }

    private static JsonEnvelope unallocatedCrownBusinessTypeQuery(final LocalDate weekCommencingStartDate, final LocalDate weekCommencingEndDate,
                                                                  final int pageSize, final int pageNumber) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(OU_CODE_QUERY_PARAMETER, CROWN_OU_CODE)
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, GENC)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, weekCommencingStartDate.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, weekCommencingEndDate.toString())
                        .add(PAGE_SIZE, pageSize)
                        .add(PAGE_NUMBER, pageNumber)
                        .build());
    }

    // -----------------------------------------------------------------------
    // rangeSearchHearings – missing branch coverage
    // -----------------------------------------------------------------------

    @Test
    void rangeSearchHearingsWithMagsAllocatedOuCodeCourtSessionAndBusinessTypeShouldDelegateToCourtScheduler() {
        final List<Hearing> hearings = hearingsJson(ALLOCATEDSTR);
        final List<IdResponse> hearingIds = new ArrayList<>();
        hearings.forEach(h -> hearingIds.add(new IdResponse(h.getId(), UUID.randomUUID(), LocalDate.now(), 1, 1)));
        final HearingIdsResponse response = new HearingIdsResponse(hearingIds, 2, 1);

        when(courtSchedulerServiceAdapter.getCourtSchedulerHearings(
                OU_CODE,
                Optional.of(COURT_SESSION),
                COURT_ROOM_ID.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString(),
                Optional.empty(),
                Optional.of(BUSINESS_TYPE),
                Optional.of(MAGISTRATES_TYPE.toString()),
                // The plain (non-calendar) range-search path always passes status=null - draft
                // exclusion is a court-calendar-only concern - so the adapter stub must match null
                // here, not "FINAL".
                null,
                "ADULT,YOUTH", 50, 1)
        ).thenReturn(response);
        when(hearingRepository.findAllCourtSchedulerHearingByIds(anyList())).thenReturn(hearings);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(OU_CODE_QUERY_PARAMETER, OU_CODE)
                        .add(COURT_SESSION_QUERY_PARAMETER, COURT_SESSION)
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, MAGISTRATES_TYPE.toString())
                        .add(BUSINESS_TYPE_QUERY_PARAMETER, BUSINESS_TYPE)
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final JsonEnvelope result = rangeSearchQuery.rangeSearchHearings(query);

        assertThat(result.payloadAsJsonObject().getInt("results"), is(2));
        assertThat(result.metadata().name(), is("listing.search.hearings"));
    }

    @Test
    void rangeSearchHearingsWithInvalidExactHearingStartDateTimeThrowsBadRequest() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, true)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(EXACT_HEARING_START_DATETIME, "not-a-valid-datetime")
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () -> rangeSearchQuery.rangeSearchHearings(query)
        );

        assertThat(thrown.getMessage(), CoreMatchers.containsString("Invalid startDateTime format"));
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

    /**
     * LPT-2406 — the court calendar must show the tier and list type a trial inherited from
     * its seeding hearing (LPT-2405). No field mapping does this: the values live in the
     * hearing's {@code properties} jsonb and reach the response only because the converter
     * emits that node wholesale. This test pins that behaviour, so a converter change that
     * starts field-picking cannot silently drop them from the calendar.
     */
    @Test
    void courtCalendarSearchShouldReturnInheritedTierAndListType() {
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
                        .add(PAGE_SIZE, 10)
                        .add(PAGE_NUMBER, 1)
                        .build());

        when(hearingRepository.findAllocatedHearingsForCourtCalendar(
                eq(COURT_CENTRE_ID), eq(COURT_ROOM_ID), eq(UUID.fromString(AUTHORITY_ID)), eq(HEARING_TYPE_ID),
                eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE), eq(SEARCH_DATE), eq(null), eq(0), eq(10)))
                .thenReturn(hearingsWithPtphDetail());

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        final JsonArray hearings = result.payloadAsJsonObject().getJsonArray("hearings");
        assertThat(hearings.isEmpty(), is(false));
        final JsonObject hearing = hearings.getJsonObject(0);
        assertThat(hearing.getString("tier"), is("TIER_3"));
        assertThat(hearing.getString("listType"), is("TYPE_1_FIXED"));
        assertThat(hearing.getString("keyReason"), is("Vulnerable witness"));
    }

    /**
     * A hearing that inherited nothing (non-trial, or a seeding record that was never
     * finalised) must simply carry no values — the calendar shows a blank, not an error.
     */
    @Test
    void courtCalendarSearchShouldReturnNoTierWhenNothingWasInherited() {
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
                        .add(PAGE_SIZE, 10)
                        .add(PAGE_NUMBER, 1)
                        .build());

        when(hearingRepository.findAllocatedHearingsForCourtCalendar(
                eq(COURT_CENTRE_ID), eq(COURT_ROOM_ID), eq(UUID.fromString(AUTHORITY_ID)), eq(HEARING_TYPE_ID),
                eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE), eq(SEARCH_DATE), eq(null), eq(0), eq(10)))
                .thenReturn(hearingsJson(ALLOCATEDSTR));

        final JsonEnvelope result = rangeSearchQuery.rangeSearchCourtCalendar(query);

        final JsonArray hearings = result.payloadAsJsonObject().getJsonArray("hearings");
        assertThat(hearings.isEmpty(), is(false));
        assertThat(hearings.getJsonObject(0).containsKey("tier"), is(false));
    }

    private List<Hearing> hearingsWithPtphDetail() {
        final LocalDate today = LocalDate.now();
        final String json = "{ \"allocated\":\"" + ALLOCATEDSTR + "\", \"startDate\": \"2020-09-03\", "
                + "\"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", "
                + "\"tier\": \"TIER_3\", \"listType\": \"TYPE_1_FIXED\", \"keyReason\": \"Vulnerable witness\", "
                + "\"courtApplications\" : [{}] , \"listedCases\" : [{}], "
                + "\"hearingDays\" : [{\"hearingDate\": \"" + today + "\"}] }";

        final Hearing hearing = new Hearing(randomUUID(), JacksonUtil.toJsonNode(json));
        hearing.setAllocated(true);
        hearing.setTotalCount(1L);
        final HearingDays hearingDay = new HearingDays();
        hearingDay.setHearingDate(today);
        hearing.getHearingDays().add(hearingDay);
        return newArrayList(hearing);
    }
}
