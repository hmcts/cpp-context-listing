package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.parse;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.deltaspike.core.util.ArraysUtils.asSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.entity.query.CaseByDefendant;
import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;
import uk.gov.moj.cpp.listing.persistence.repository.CaseByDefendantRepository;
import uk.gov.moj.cpp.listing.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusResult;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;
import uk.gov.moj.cpp.listing.query.view.courtlist.CourtListService;
import uk.gov.moj.cpp.listing.query.view.dto.LinkedApplicationsSummary;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameter;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory;
import uk.gov.moj.cpp.listing.query.view.dto.csv.HearingCsvData;
import uk.gov.moj.cpp.listing.query.view.hearing.ApplicationTypeFilter;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;
import uk.gov.moj.cpp.listing.query.view.service.csv.HearingCsvReportService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.ReadContext;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class HearingQueryViewTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String PUBLISH_COURT_LIST_TYPES = "FIRM,FINAL";
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID ID = fromString("7c5e9d0c-9e28-46a9-b139-68fc0813842c");
    private static final boolean ALLOCATED = true;
    private static final String ALLOCATEDSTR = "true";
    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final String SEARCH_DATE_QUERY_PARAMETER = "searchDate";
    private static final String START_DATE_QUERY_PARAMETER = "startDate";
    private static final String END_DATE_QUERY_PARAMETER = "endDate";
    private static final String START_TIME_QUERY_PARAMETER = "startTime";
    private static final String END_TIME_QUERY_PARAMETER = "endTime";
    private static final String COURT_CENTRE_QUERY_PARAMETER = "courtCentreId";
    private static final String PUBLISH_COURT_LIST_TYPES_QUERY_PARAMETER = "publishCourtListTypes";
    private static final String PUBLISH_DATE_QUERY_PARAMETER = "publishDate";
    private static final String WEEK_COMMENCING_QUERY_PARAMETER = "weekCommencing";
    private static final String CASE_URN = "caseUrn";
    private static final String TYPE_OF_LIST = "typeOfList";
    private static final String COURT_CENTRE_IDS = "courtCentreIds";
    private static final String PAGE_SIZE = "pageSize";
    private static final String PAGE_NUMBER = "pageNumber";
    private static final String RETURN_ALL_HEARINGS = "returnAllHearings";

    private static final String COURT_ROOM_QUERY_PARAMETER = "courtRoomId";
    private static final String AUTHORITY_ID_QUERY_PARAMETER = "authorityId";
    private static final String HEARING_TYPE_QUERY_PARAMETER = "hearingTypeId";
    private static final String JURISDICTION_TYPE_QUERY_PARAMETER = "jurisdictionType";
    private static final String LIST_ID_QUERY_PARAMETER = "listId";
    private static final String CASE_URN_QUERY_PARAMETER = "caseUrn";
    private static final String HEARING_ID_QUERY_PARAMETER = "hearingId";
    private static final String SEARCH_CRITERIA_QUERY_PARAMETER = "searchCriteria";
    private static final String MATCHING_DEFENDANT_IDS_QUERY_PARAMETER = "matchedDefendantIds";
    private static final String CASE_URN_FOR_LINKED_CASES_QUERY_PARAMETER = "caseUrnForLinkedCases";
    private static final String ID_PARAMETER = "id";
    private static final String AUTHORITY_ID = "efa4e01b-1dc5-48c5-80b5-c3858a7622d6";
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final LocalDate SEARCH_DATE = LocalDate.parse("2023-08-29");
    private static final LocalTime START_TIME = LocalTime.now();
    private static final LocalTime END_TIME = LocalTime.now();
    private static final String PUBLISH_DATE = "2012-12-11";
    private static final String PUBLISH_DATE_WEEK_COMMENCING = "2012-12-13";
    private static final String EMPTY_STRING = "";
    private static final String CASE_ID_QUERY_PARAMETER  = "caseId";
    private static final String APPLICATION_ID_QUERY_PARAMETER  = "applicationId";
    private static final String WOFD_HEARING_TYPE_ID = "638ced9d-3f95-4e99-b27b-47fa5a2c6add";
    private static final String PCB_HEARING_TYPE_ID = "3a2d160f-363b-4360-96e1-0007a400a64c";


    @Spy
    private Enveloper enveloper = createEnveloper();
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private CourtApplicationRepository courtApplicationRepository;
    @Mock
    private CourtListPublishStatusJdbcRepository courtListRepository;
    @Mock
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;
    @Mock
    private RangeSearchQuery rangeSearchQuery;
    @Mock
    private CourtListService courtListService;
    @Mock
    private PublishedCourtListRepository publishedCourtListRepository;
    @Mock
    private PublishedCourtListToJsonConverter publishedCourtListToJsonConverter;
    @Mock
    private NotesService notesService;
    @Mock
    private CaseByDefendantRepository caseByDefendantRepository;
    @Mock
    private ApplicationTypeFilter applicationTypeFilter;
    @Mock
    private HearingCsvReportService hearingCsvReportService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();;

    @Spy
    private PaginationParameterFactory paginationParameterFactory;

    private PaginationParameter paginationParameter;

    @InjectMocks
    private HearingQueryView hearingsQueryView;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    private ListToJsonArrayConverter listToJsonArrayConverter = new ListToJsonArrayConverter();

    @BeforeEach
    public void setup() throws IllegalAccessException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        FieldUtils.writeField(this.listToJsonArrayConverter, "mapper", objectMapper, true);
        FieldUtils.writeField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter, true);
        FieldUtils.writeField(this.hearingsQueryView, "listToJsonArrayConverter", listToJsonArrayConverter, true);
        final JsonObject paginationParametersAsJson = createObjectBuilder().add("pageSize", 50).add("pageNumber", 1).add("offset", 0).build();
        paginationParameter = paginationParameterFactory.newPaginationParameter(paginationParametersAsJson);
    }

    @Test
    public void shouldReturnCorrectPublishCourtListStatusForWeekCommencing() {

        final Set<PublishCourtListType> publishCourtListTypeSet = new HashSet<>();
        publishCourtListTypeSet.add(PublishCourtListType.FIRM);
        publishCourtListTypeSet.add(PublishCourtListType.FINAL);

        when(courtListRepository.courtListPublishStatuses(COURT_CENTRE_ID, publishCourtListTypeSet, parse(PUBLISH_DATE_WEEK_COMMENCING), true))
                .thenReturn(publishCourtListStatuses());

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.court.list.publish.status"),
                createObjectBuilder()
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(PUBLISH_COURT_LIST_TYPES_QUERY_PARAMETER, PUBLISH_COURT_LIST_TYPES.toString())
                        .add(PUBLISH_DATE_QUERY_PARAMETER, PUBLISH_DATE_WEEK_COMMENCING)
                        .add(WEEK_COMMENCING_QUERY_PARAMETER, true)
                        .build());

        final JsonEnvelope results = hearingsQueryView.getCourtListPublishStatus(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.court.list.publish.status"), payloadIsJson(
                allOf(
                        withJsonPath("$.publishCourtListStatuses", hasSize(1)),
                        withJsonPath("$.publishCourtListStatuses[0].publishStatus", equalTo(EXPORT_SUCCESSFUL.name())),
                        withJsonPath("$.publishCourtListStatuses[0].publishCourtListType", equalTo(FINAL.toString()))
                )))));
        verify(courtListRepository).courtListPublishStatuses(eq(COURT_CENTRE_ID), eq(publishCourtListTypeSet), eq(parse(PUBLISH_DATE_WEEK_COMMENCING)), eq(true));
    }

    @Test
    public void shouldReturnCorrectPublishCourtListStatusForFixedDate() {

        final Set<PublishCourtListType> publishCourtListTypeSet = new HashSet<>();
        publishCourtListTypeSet.add(PublishCourtListType.FIRM);
        publishCourtListTypeSet.add(PublishCourtListType.FINAL);

        when(courtListRepository.courtListPublishStatuses(COURT_CENTRE_ID, publishCourtListTypeSet, parse(PUBLISH_DATE), false))
                .thenReturn(publishCourtListStatuses());

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.court.list.publish.status"),
                createObjectBuilder()
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(PUBLISH_COURT_LIST_TYPES_QUERY_PARAMETER, PUBLISH_COURT_LIST_TYPES.toString())
                        .add(PUBLISH_DATE_QUERY_PARAMETER, PUBLISH_DATE)
                        .add(WEEK_COMMENCING_QUERY_PARAMETER, false)
                        .build());

        final JsonEnvelope results = hearingsQueryView.getCourtListPublishStatus(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.court.list.publish.status"), payloadIsJson(
                allOf(
                        withJsonPath("$.publishCourtListStatuses", hasSize(1)),
                        withJsonPath("$.publishCourtListStatuses[0].publishStatus", equalTo(EXPORT_SUCCESSFUL.name())),
                        withJsonPath("$.publishCourtListStatuses[0].publishCourtListType", equalTo(FINAL.toString()))
                )))));

        verify(courtListRepository).courtListPublishStatuses(eq(COURT_CENTRE_ID), eq(publishCourtListTypeSet), eq(parse(PUBLISH_DATE)), eq(false));
    }


    private List<CourtListPublishStatusResult> publishCourtListStatuses() {
        final UUID courtCentreId1 = randomUUID();
        final CourtListPublishStatusResult publishCourtListStatus3 = new CourtListPublishStatusResult(courtCentreId1, FINAL, now(), EXPORT_SUCCESSFUL, "");
        return newArrayList(publishCourtListStatus3);
    }

    @Test
    public void searchHearingsWithSearchDateWithAllParametersProvidedApartFromStartTime() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder()
                        .add("hello", "world"))
                .build();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE.atStartOfDay(UTC),
                SEARCH_DATE.atTime(END_TIME).atZone(UTC)))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convertForSearchHearing(anyList(), anyMap())).thenReturn(hearingsJsonArray);
        when(applicationTypeFilter.filter(any(), anyList())).thenReturn(hearingsJson);


        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(SEARCH_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_TIME_QUERY_PARAMETER, END_TIME.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(
                        withJsonPath("$.hearings[0].hello", equalTo("world"))
                ))
        ));
        verify(hearingRepository).findHearings(eq(ALLOCATED), eq(COURT_CENTRE_ID.toString()), eq(COURT_ROOM_ID.toString()),
                eq(fromString(AUTHORITY_ID).toString()), eq(HEARING_TYPE_ID.toString()), eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE),
                eq(SEARCH_DATE.atTime(LocalTime.MIN).atZone(UTC)), eq(SEARCH_DATE.atTime(END_TIME).atZone(UTC)));
        verify(hearingJsonListConverterFilterEjectCases).convertForSearchHearing(eq(hearingsJson), anyMap());
    }

    @Test
    public void searchHearingsWithSearchDateWithAllParametersProvidedApartFromStartTimeAndReturnFilteredData() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final List<Hearing> hearingsFilteredJson = singletonList(hearingsJson.get(0));
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder()
                        .add("hello", "world"))
                .build();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE.atStartOfDay(UTC),
                SEARCH_DATE.atTime(END_TIME).atZone(UTC)))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convertForSearchHearing(anyList(), anyMap())).thenReturn(hearingsJsonArray);
        when(applicationTypeFilter.filter(any(), anyList())).thenReturn(hearingsFilteredJson);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(SEARCH_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_TIME_QUERY_PARAMETER, END_TIME.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchHearings(query);
        assertThat(hearingsJson.size(), is(2));
        assertThat(results.payloadAsJsonObject().getJsonArray("hearings").size(), is(1));
    }

    @Test
    public void searchHearingsWithSearchDateWithAllParametersProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder()
                        .add("hello", "world"))
                .build();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE.atTime(START_TIME).atZone(UTC),
                SEARCH_DATE.atTime(END_TIME).atZone(UTC)))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convertForSearchHearing(anyList(), anyMap())).thenReturn(hearingsJsonArray);
        when(notesService.findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), any(List.class))).thenReturn(createNotesList());
        when(applicationTypeFilter.filter(any(), anyList())).thenReturn(hearingsJson);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(SEARCH_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(START_TIME_QUERY_PARAMETER, START_TIME.toString())
                        .add(END_TIME_QUERY_PARAMETER, END_TIME.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(
                        withJsonPath("$.hearings[0].hello", equalTo("world"))
                ))
        ));
        verify(hearingRepository).findHearings(eq(ALLOCATED), eq(COURT_CENTRE_ID.toString()), eq(COURT_ROOM_ID.toString()),
                eq(AUTHORITY_ID), eq(HEARING_TYPE_ID.toString()),eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE),
                eq(SEARCH_DATE.atTime(START_TIME).atZone(UTC)), eq(SEARCH_DATE.atTime(END_TIME).atZone(UTC)));
        verify(hearingJsonListConverterFilterEjectCases).convertForSearchHearing(eq(hearingsJson), anyMap());
        verify(notesService, times(1)).findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), any(List.class));
    }

    @Test
    public void shouldFindHearingsWithMatchingDefendantIds() throws Exception {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        final Set<String> MASTER_DEFENDANT_IDS = new HashSet<>();
        MASTER_DEFENDANT_IDS.add("d676f354-ba50-462e-bd55-4e8842d29ebd");

        final Set<String> CASE_URN_SET = new HashSet<>();
        CASE_URN_SET.add(EMPTY_STRING);

        final Set<String> LINKED_CASE_SET = new HashSet<>();
        LINKED_CASE_SET.add(EMPTY_STRING);

        final String SEARCH_CRITERIA = "CASE_IN_HEARING,MATCHED_DEFENDANTS";

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());

        when(hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                null,
                CASE_URN_SET,
                MASTER_DEFENDANT_IDS,
                LINKED_CASE_SET,
                null, LocalDate.now()))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(SEARCH_CRITERIA_QUERY_PARAMETER, SEARCH_CRITERIA)
                        .add(MATCHING_DEFENDANT_IDS_QUERY_PARAMETER, MASTER_DEFENDANT_IDS.stream().collect(Collectors.joining(",")))
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchAvailableHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearings[0].startDate", equalTo("2020-09-03")),
                        withJsonPath("$.hearings[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")),
                        withJsonPath("$.notes.size()", equalTo(0)))
                ))
        ));
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearingsByCaseId() throws Exception {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        final UUID caseId = UUID.randomUUID();

        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(caseId.toString())).thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_ID_QUERY_PARAMETER, caseId.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearings[0].startDate", equalTo("2020-09-03")),
                        withJsonPath("$.hearings[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")))
                ))
        ));
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearingsByCaseAndApplicationId() throws Exception {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        final UUID caseId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();

        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(caseId.toString(), applicationId.toString())).thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        when(courtApplicationRepository.findByParentApplicationId(applicationId)).thenReturn(List.of());
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_ID_QUERY_PARAMETER, caseId.toString())
                        .add(APPLICATION_ID_QUERY_PARAMETER, applicationId.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearings[0].startDate", equalTo("2020-09-03")),
                        withJsonPath("$.hearings[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")))
                ))
        ));
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearingsByParentAndChildApplicationId() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        final UUID applicationId = UUID.randomUUID();
        final UUID childApplicationId = UUID.randomUUID();
        final UUID childApplicationId1 = UUID.randomUUID();

        final List<Hearing> childHearingsJson = childHearingsJson(ALLOCATEDSTR);
        final List<Hearing> allHearings = hearingsJson;
        allHearings.addAll(childHearingsJson);


        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(null, applicationId.toString())).thenReturn(hearingsJson);

        List<CourtApplications> list = new ArrayList<CourtApplications>();
        list.add(new CourtApplications(randomUUID(), childApplicationId, null,
                "appType", applicationId, "appRef", "appParticulars", false));
        list.add(new CourtApplications(randomUUID(), childApplicationId1, null,
                "appType", applicationId, "appRef", "appParticulars", false));

        when(courtApplicationRepository.findByParentApplicationId(applicationId)).thenReturn(list);

        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(null, childApplicationId.toString())).thenReturn(childHearingsJson);
        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(null, childApplicationId1.toString())).thenReturn(childHearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(allHearings))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(allHearings));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(APPLICATION_ID_QUERY_PARAMETER, applicationId.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearings[0].startDate", equalTo("2020-09-03")),
                        withJsonPath("$.hearings[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")),
                        withJsonPath("$.hearings[2].startDate", equalTo("2020-10-10")),
                        withJsonPath("$.hearings[2].courtRoomId", equalTo("42481915-4d98-437b-a5dd-ace41e2ab0ea"))
                        )
                ))
        ));
    }

    @Test
    public void shouldFindHearingsWithCaseUrnForLinkedCase() throws Exception {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        final Set<String> MASTER_DEFENDANT_IDS = new HashSet<>();
        MASTER_DEFENDANT_IDS.add(EMPTY_STRING);

        final Set<String> CASE_URN_SET = new HashSet<>();
        CASE_URN_SET.add(EMPTY_STRING);

        final Set<String> LINKED_CASE_SET = new HashSet<>();
        LINKED_CASE_SET.add(EMPTY_STRING);

        final String SEARCH_CRITERIA = "MATCHED_DEFENDANTS";

        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());

        final String caseUrnForLinkedCase = "45DI277164";

        when(hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                null,
                CASE_URN_SET,
                MASTER_DEFENDANT_IDS,
                LINKED_CASE_SET,
                caseUrnForLinkedCase, LocalDate.now()))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(SEARCH_CRITERIA_QUERY_PARAMETER, SEARCH_CRITERIA)
                        .add(CASE_URN_FOR_LINKED_CASES_QUERY_PARAMETER, caseUrnForLinkedCase)
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchAvailableHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearings[0].startDate", equalTo("2020-09-03")),
                        withJsonPath("$.hearings[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")),
                        withJsonPath("$.notes.size()", equalTo(0)))
                ))
        ));
    }

    @Test
    public void shouldSearchAvailableHearingsWithWithAllParametersProvided() throws Exception {
        searchAvailableHearingsWithWithAllParametersProvided(getHearingById(), false);
    }

    @Test
    public void shouldSearchAvailableHearingsWithWithAllParametersProvidedForUnscheduledHearing() throws Exception {
        searchAvailableHearingsWithWithAllParametersProvided(getUnscheduledHearingById(), false);
    }

    @Test
    public void shouldReturnNotesAndSearchAvailableHearingsWithWithAllParametersProvided() throws Exception {
        searchAvailableHearingsWithWithAllParametersProvided(getHearingById(), true);
    }

    @Test
    public void shouldReturnNotesAndSearchAvailableHearingsWithWithAllParametersProvidedAndReturnAllHearings() throws Exception {
        searchAvailableHearingsWithWithAllParametersProvided(getHearingById(), true, true);
    }

    public void searchAvailableHearingsWithWithAllParametersProvided(final Hearing returnedHearing, final boolean notesExist) throws Exception {
        searchAvailableHearingsWithWithAllParametersProvided(returnedHearing, notesExist, false);
    }

    public void searchAvailableHearingsWithWithAllParametersProvided(final Hearing returnedHearing, final boolean notesExist, final boolean returnAllHearings) throws Exception {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);

        final Set<String> MASTER_DEFENDANT_IDS = new HashSet<>();
        MASTER_DEFENDANT_IDS.add("d676f354-ba50-462e-bd55-4e8842d29ebd");

        final Set<String> CASE_URN_SET = new HashSet<>();
        CASE_URN_SET.add("CCC333");
        CASE_URN_SET.add("DDD444");
        CASE_URN_SET.add("EEE555");

        final Set<String> LINKED_CASE_SET = new HashSet<>();
        LINKED_CASE_SET.add("URN1");
        LINKED_CASE_SET.add("URN2");

        final String SEARCH_CRITERIA = "CASE_IN_HEARING,MATCHED_DEFENDANTS";
        final Set<String> jurisdictionTypeSet = new HashSet<>();
        jurisdictionTypeSet.add(JurisdictionType.CROWN.name());

        when(hearingRepository.findBy(ID)).thenReturn(returnedHearing);
        final JsonObject queryPayload;
        if(returnAllHearings){
            when(hearingRepository.findHearings(
                    jurisdictionTypeSet,
                    ID.toString(),
                    CASE_URN_SET,
                    MASTER_DEFENDANT_IDS,
                    LINKED_CASE_SET,
                    null, LocalDate.now()))
                    .thenReturn(hearingsJson);

            queryPayload = createObjectBuilder()
                    .add(RETURN_ALL_HEARINGS, ALLOCATED)
                    .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                    .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                    .add(HEARING_ID_QUERY_PARAMETER, ID.toString())
                    .add(CASE_URN_QUERY_PARAMETER, CASE_URN_SET.stream().collect(Collectors.joining(",")))
                    .add(SEARCH_CRITERIA_QUERY_PARAMETER, SEARCH_CRITERIA)

                    .build();
        }else {
            when(hearingRepository.findHearings(
                    ALLOCATED,
                    jurisdictionTypeSet,
                    ID.toString(),
                    CASE_URN_SET,
                    MASTER_DEFENDANT_IDS,
                    LINKED_CASE_SET,
                    null, LocalDate.now()))
                    .thenReturn(hearingsJson);

            queryPayload = createObjectBuilder()
                    .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                    .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                    .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                    .add(HEARING_ID_QUERY_PARAMETER, ID.toString())
                    .add(CASE_URN_QUERY_PARAMETER, CASE_URN_SET.stream().collect(Collectors.joining(",")))
                    .add(SEARCH_CRITERIA_QUERY_PARAMETER, SEARCH_CRITERIA)
                    .build();
        }
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));
        if (notesExist) {
            when(notesService.findNotes(eq(ALLOCATED), eq(null), eq(null), any(List.class))).thenReturn(createNotesList());
        }
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                queryPayload);

        final JsonEnvelope results = hearingsQueryView.searchAvailableHearings(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.hearings"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearings[0].startDate", equalTo("2020-09-03")),
                        withJsonPath("$.hearings[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")),
                        withJsonPath("$.hearings[0].judiciary", hasSize(0)),
                        getMatcherForNotes(notesExist))
                ))
        ));
        verify(hearingRepository).findBy(eq(ID));
        if(returnAllHearings){
            verify(hearingRepository).findHearings(eq(jurisdictionTypeSet),
                    eq(ID.toString()), eq(CASE_URN_SET), eq(MASTER_DEFENDANT_IDS), eq(LINKED_CASE_SET), eq(null), eq(LocalDate.now()));
        }else {
            verify(hearingRepository).findHearings(eq(ALLOCATED), eq(jurisdictionTypeSet),
                    eq(ID.toString()), eq(CASE_URN_SET), eq(MASTER_DEFENDANT_IDS), eq(LINKED_CASE_SET), eq(null), eq(LocalDate.now()));
        }
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
    }

    @Test
    public void shouldSearchUnscheduledHearingsWithoutCourtCentreIds() throws IOException {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        hearingsJson.get(0).setCourtCentreId(randomUUID());
        hearingsJson.get(1).setCourtCentreId(randomUUID());
        final JsonArray hearingsJsonArray = hearingsJsonArray();
        final String typeOfListId = "82c4c09c-12f1-4014-ad9f-f1a67aba1761";

        when(hearingRepository.findHearings(
                "caseUrnValue", typeOfListId, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_URN, "caseUrnValue")
                        .add(TYPE_OF_LIST, typeOfListId)
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        final Envelope<JsonObject> results = hearingsQueryView.searchUnscheduledHearings(query);

        assertThat(envelopeFrom(results.metadata(), results.payload()),
                is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.unscheduled.search.hearings"),
                        payloadIsJson(
                                withJsonPath("$.hearings[0].hello", equalTo("world"))
                        ))
                ));
        verify(hearingRepository).findHearings(eq("caseUrnValue"), eq(typeOfListId), eq(0), eq(50));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
    }

    @Test
    public void shouldSearchHearingsWithAnyAllocationWithCaseUrn() throws IOException {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();
        final LocalDate startDate = LocalDate.now();

        when(hearingRepository.findHearingsByCaseUrnAndAnyAllocationState("CASEURNVALUE", startDate))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_URN, "caseUrnValue")
                        .add(START_DATE_QUERY_PARAMETER, startDate.toString())
                        .build());

        final Envelope<JsonObject> results = hearingsQueryView.searchHearingsWithAnyAllocationState(query);

        assertThat(JsonEnvelope.envelopeFrom(results.metadata(), results.payload()),
                is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.any-allocation.search.hearings"),
                        payloadIsJson(
                                withJsonPath("$.hearings[0].hello", equalTo("world"))
                        ))
                ));
        verify(hearingRepository).findHearingsByCaseUrnAndAnyAllocationState(eq("CASEURNVALUE"), eq(startDate));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
    }

    @Test
    public void shouldSearchUnscheduledHearingsWithCourtCentreIds() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();
        final String typeOfListId = "82c4c09c-12f1-4014-ad9f-f1a67aba1761";
        final UUID courtCentreId1 = UUID.fromString("6bf56746-cfe8-40bc-a789-3fae393c33ab");
        final UUID courtCentreId2 = UUID.fromString("110aa28-abf7-4ff4-8848-942814e55787");
        final String courtCentreId = courtCentreId1.toString() +"," +courtCentreId2.toString();

        final HashSet<String> courtCentreIdsSet = new HashSet<>(Arrays.asList(courtCentreId1.toString(), courtCentreId2.toString()));
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_URN, "caseUrnValue")
                        .add(TYPE_OF_LIST, typeOfListId)
                        .add(COURT_CENTRE_IDS, courtCentreId)
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());
        when(hearingRepository.findHearings(
                "caseUrnValue", typeOfListId, courtCentreIdsSet, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);

        Set courtCentreIdSet = new HashSet();
        courtCentreIdSet.add("6bf56746-cfe8-40bc-a789-3fae393c33ab");
        courtCentreIdSet.add("0110aa28-abf7-4ff4-8848-942814e55787");

        final Envelope<JsonObject> results = hearingsQueryView.searchUnscheduledHearings(query);

        assertThat(envelopeFrom(results.metadata(), results.payload()),
                is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.unscheduled.search.hearings"),
                        payloadIsJson(
                                withJsonPath("$.hearings[0].hello", equalTo("world"))
                        ))
                ));
        verify(hearingRepository).findHearings(eq("caseUrnValue"), eq(typeOfListId), eq(courtCentreIdsSet), eq(0), eq(50));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
        assertTrue(((List)results.payload().get("hearings")).size() == 1);
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearingsWithCaseIdAndApplicationId() throws IOException {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        final ListedCases listedCases1 = new ListedCases();
        final ListedCases listedCases2 = new ListedCases();

        hearingsJson.get(0).getListedCases().add(listedCases1);
        hearingsJson.get(1).getListedCases().add(listedCases2);

        final ObjectMapper objectMapper = new ObjectMapper();
        hearingsJson.get(0).setProperties(objectMapper.readTree("{\"type\":{\"description\":\"Review\"}}"));

        final UUID applicationId = UUID.randomUUID();
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add("caseId", "be7866d6-cecb-407c-83dc-34864f7b4ff6")
                        .add("applicationId", applicationId.toString())
                        .build());

        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(
                "be7866d6-cecb-407c-83dc-34864f7b4ff6", applicationId.toString()))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);

        final List<LinkedApplicationsSummary> linkedApplicationsSummaryList = new ArrayList<>();
        linkedApplicationsSummaryList.add(LinkedApplicationsSummary.linkedApplicationsSummary().build());


        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);


        verify(hearingRepository).findAllocatedAndUnallocatedHearingsByCaseId(eq("be7866d6-cecb-407c-83dc-34864f7b4ff6"), eq(applicationId.toString()));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
        assertEquals(1, ((JsonObject) results.payload()).getJsonArray("hearings").size());
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearingsWithCaseId() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add("caseId", "caseId")
                        .build());
        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(
                "caseId"))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);

        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);


        verify(hearingRepository).findAllocatedAndUnallocatedHearingsByCaseId(eq("caseId"));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
        assertEquals(1, ((JsonObject) results.payload()).getJsonArray("hearings").size());
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearings() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);


        verify(hearingRepository, times(0)).findAllocatedAndUnallocatedHearingsByCaseId(any());
        verify(hearingRepository, times(0)).findAllocatedAndUnallocatedHearingsByCaseId(any());
        verify(hearingJsonListConverterFilterEjectCases, times(0)).convert(eq(hearingsJson));
        assertEquals(0, ((JsonObject) results.payload()).getJsonArray("hearings").size());
    }

    @Test
    public void shouldSearchUnscheduledHearingsWithCourtCentreIdsNotReturnHmiEnabled() throws Exception{

        HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases = new HearingJsonListConverterFilterEjectCases();
        Field converter = HearingQueryView.class.getDeclaredField("hearingJsonListConverterFilterEjectCases");
        converter.setAccessible(true);
        converter.set(hearingsQueryView, hearingJsonListConverterFilterEjectCases);


        final List<Hearing> hearingsJson = singleHearingsJson(ALLOCATEDSTR);
        final String typeOfListId = "82c4c09c-12f1-4014-ad9f-f1a67aba1761";
        final UUID courtCentreId1 = UUID.fromString("6bf56746-cfe8-40bc-a789-3fae393c33ab");
        final String courtCentreId = courtCentreId1.toString();

        final HashSet<String> courtCentreIdsSet = new HashSet<>(Arrays.asList(courtCentreId1.toString()));
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_URN, "caseUrnValue")
                        .add(TYPE_OF_LIST, typeOfListId)
                        .add(COURT_CENTRE_IDS, courtCentreId)
                        .add(PAGE_SIZE, "50")
                        .add(PAGE_NUMBER, "1")
                        .build());

        when(hearingRepository.findHearings(
                "caseUrnValue", typeOfListId, courtCentreIdsSet, 0, paginationParameter.getPageSize()))
                .thenReturn(hearingsJson);

        Set courtCentreIdSet = new HashSet();
        courtCentreIdSet.add("6bf56746-cfe8-40bc-a789-3fae393c33ab");
        courtCentreIdSet.add("0110aa28-abf7-4ff4-8848-942814e55787");

        final Envelope<JsonObject> results = hearingsQueryView.searchUnscheduledHearings(query);

        assertEquals(1, ((List) results.payload().get("hearings")).size());
    }

    @Test
    public void getAlphabeticalCourtListContentWithAllParamsProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        final Set<String> excludedHearingTypeIds = Set.of(WOFD_HEARING_TYPE_ID, PCB_HEARING_TYPE_ID);
        when(hearingRepository.findHearingsForAlphabeticalList(ALLOCATED, COURT_CENTRE_ID.toString(), SEARCH_DATE, excludedHearingTypeIds))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convertHearingResultForAlphabeticalList(hearingsJson)).thenReturn(hearingsJsonArray);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(LIST_ID_QUERY_PARAMETER, CourtListType.ALPHABETICAL.name())
                        .build());

        final JsonEnvelope results = hearingsQueryView.getCourtListContent(query);

        assertThat(results, is(jsonEnvelope(metadata().withName("listing.search.court.list"),
                payloadIsJson(
                        withJsonPath("$.hearings[0].hello", equalTo("world"))
                ))
        ));
        verify(hearingRepository).findHearingsForAlphabeticalList(eq(ALLOCATED), eq(COURT_CENTRE_ID.toString()), eq(SEARCH_DATE), eq(excludedHearingTypeIds));
        verify(hearingJsonListConverterFilterEjectCases).convertHearingResultForAlphabeticalList(eq(hearingsJson));
    }

    @Test
    public void searchHearingsAllCaseApplicationsEjected() {
        final List<Hearing> hearingsJson = hearingJsonForEjected();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE,
                SEARCH_DATE.atTime(START_TIME).atZone(UTC),
                SEARCH_DATE.atTime(END_TIME).atZone(UTC)))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convertForSearchHearing(anyList(), anyMap())).thenCallRealMethod();
        when(notesService.findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), any(List.class))).thenReturn(createNotesList());
        when(applicationTypeFilter.filter(any(), anyList())).thenReturn(hearingsJson);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .add(COURT_ROOM_QUERY_PARAMETER, COURT_ROOM_ID.toString())
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(HEARING_TYPE_QUERY_PARAMETER, HEARING_TYPE_ID.toString())
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(SEARCH_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(START_TIME_QUERY_PARAMETER, START_TIME.toString())
                        .add(END_TIME_QUERY_PARAMETER, END_TIME.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchHearings(query);
        assertThat(results.payloadAsJsonObject().getJsonArray("hearings").size(), is(0));

        verify(hearingRepository).findHearings(eq(ALLOCATED), eq(COURT_CENTRE_ID.toString()), eq(COURT_ROOM_ID.toString()),
                eq(AUTHORITY_ID), eq(HEARING_TYPE_ID.toString()), eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE),
                eq(SEARCH_DATE.atTime(START_TIME).atZone(UTC)), eq(SEARCH_DATE.atTime(END_TIME).atZone(UTC)));

        verify(hearingJsonListConverterFilterEjectCases).convertForSearchHearing(eq(hearingsJson), anyMap());
        verify(notesService, times(1)).findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), any(List.class));
    }

    @Test
    public void findHearingByIdWhenIdIsAbsent() {

        final JsonEnvelope query = generateQuery(
                createObjectBuilder()
                        .build());

        assertThrows(NullPointerException.class, () -> hearingsQueryView.getHearingById(query));
    }

    @Test
    public void findHearingByIdWhenIdIsInvalid() {

        final JsonEnvelope query = generateQuery(
                createObjectBuilder()
                        .add("id", "b4ae7de5-6bce")
                        .build());

        try {
            hearingsQueryView.getHearingById(query);
            fail("Expected a IllegalArgumentException.");
        } catch (IllegalArgumentException ex) {
            assertEquals("Invalid UUID string: b4ae7de5-6bce", ex.getMessage());
        }

    }

    @Test
    public void findHearingByIdWhenNotFound() {

        when(hearingRepository.findBy(ID)).thenReturn(null);

        final JsonEnvelope query = generateQuery(
                createObjectBuilder().add(ID_PARAMETER, ID.toString())
                        .build());

        try {
            hearingsQueryView.getHearingById(query);
            fail("Expected a NotFoundException.");
        } catch (NotFoundException ex) {
            assertEquals("There is no Hearing for this ID: "+ID, ex.getMessage());
        }
        verify(hearingRepository).findBy(eq(ID));
    }

    @Test
    public void findHearingByIdWhenFound() {

        final Hearing returnedHearing = generateMinimalHearing();
        when(hearingRepository.findBy(ID)).thenReturn(returnedHearing);

        final JsonEnvelope query = generateQuery(
                createObjectBuilder().add(ID_PARAMETER, ID.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.getHearingById(query);

        assertThat(results,
                is(jsonEnvelope(metadata()
                                .withName("listing.search.hearing"),
                        payloadIsJson(
                                withJsonPath("$.id", equalTo(ID.toString()))
                        ))
                ));
        verify(hearingRepository).findBy(eq(ID));
    }

    @Test
    public void shouldGetAllPublishedCourtLists() {

        final List<PublishedCourtList> publishedCourtLists = new ArrayList<>();
        final JsonObject expectedPayload = JsonObjects.createObjectBuilder().build();

        when(publishedCourtListRepository.findAll()).thenReturn(publishedCourtLists);
        when(publishedCourtListToJsonConverter.convert(publishedCourtLists)).thenReturn(expectedPayload);

        final JsonEnvelope queryEnvelope = generateQuery(
                createObjectBuilder()
                        .build());

        final JsonEnvelope results = hearingsQueryView.getPublishedCourtLists(queryEnvelope);

        assertThat(results,
                is(jsonEnvelope(metadata()
                                .withName("listing.publishedcourtlist"),
                        payloadIsJson(
                                withJsonPath("$", equalTo(expectedPayload))
                        ))
                ));
        verify(publishedCourtListRepository).findAll();
        verify(publishedCourtListToJsonConverter).convert(eq(publishedCourtLists));
    }

    @Test
    public void shouldRetrieveUnpublishedCourtList() {

        final UUID courtCentreId = randomUUID();
        final uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType = uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        final String endDate = LocalDate.now().toString();

        final JsonEnvelope queryEnvelope = generateQuery(
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("publishCourtListType", publishCourtListType.name())
                        .add("startDate", startDate.toString())
                        .add("endDate", endDate)
                        .build());

        final JsonObject courtListResponsePayload = JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .build();

        when(courtListService.retrieveUnPublishedCourtList(courtCentreId, publishCourtListType,
                startDate,
                endDate,
                queryEnvelope)).thenReturn(courtListResponsePayload);

        final JsonEnvelope results = hearingsQueryView.retrieveCourtList(queryEnvelope);

        assertThat(results,
                is(jsonEnvelope(metadata()
                                .withName("listing.courtlist"),
                        payloadIsJson(
                                withJsonPath("$.courtCentreId", equalTo(courtCentreId.toString()))
                        ))
                ));
        verify(courtListService).retrieveUnPublishedCourtList(eq(courtCentreId), eq(publishCourtListType),
                eq(startDate), eq(endDate), eq(queryEnvelope));
    }

    @Test
    public void shouldRetrievePublishedCourtListIfExists() {

        final UUID courtCentreId = randomUUID();
        final uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType = uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonEnvelope queryEnvelope = generateQuery(
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("publishCourtListType", publishCourtListType.name())
                        .add("startDate", startDate.toString())
                        .add("published", true)
                        .build());

        final JsonObject courtListJson = createObjectBuilder().build();

        final JsonObject expectedPayload = JsonObjects.createObjectBuilder()
                .add("courtListJson", courtListJson)
                .build();

        final PublishedCourtList publishedCourtList = new PublishedCourtList(courtCentreId,
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(publishCourtListType.name()), startDate, null,
                ZonedDateTime.now(), ZonedDateTime.now(), null);

        final PublishedCourtListPrimaryKey primaryKey = new PublishedCourtListPrimaryKey(
                courtCentreId,
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(publishCourtListType.name()),
                startDate);

        when(publishedCourtListRepository.findBy(primaryKey)).thenReturn(publishedCourtList);
        when(publishedCourtListToJsonConverter.convert(publishedCourtList)).thenReturn(expectedPayload);

        final JsonEnvelope results = hearingsQueryView.retrieveCourtList(queryEnvelope);

        assertThat(results.payloadAsJsonObject(), is(courtListJson));
        verify(publishedCourtListRepository).findBy(eq(primaryKey));
        verify(publishedCourtListToJsonConverter).convert(eq(publishedCourtList));
    }

    @Test
    public void shouldRetrievePublishedCourtListIfNotExists() {

        final UUID courtCentreId = randomUUID();
        final uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType = uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonEnvelope queryEnvelope = generateQuery(
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("publishCourtListType", publishCourtListType.name())
                        .add("startDate", startDate.toString())
                        .add("published", true)
                        .build());

        final PublishedCourtListPrimaryKey primaryKey = new PublishedCourtListPrimaryKey(
                courtCentreId,
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(publishCourtListType.name()),
                startDate);

        final JsonObject emptyCourtListJson = JsonObjects.createObjectBuilder().build();

        when(publishedCourtListRepository.findBy(primaryKey)).thenReturn(null);
        when(courtListService.emptyCourtList(courtCentreId)).thenReturn(emptyCourtListJson);

        final JsonEnvelope response = hearingsQueryView.retrieveCourtList(queryEnvelope);

        assertThat(response.payloadAsJsonObject(), is(emptyCourtListJson));
        verify(publishedCourtListRepository).findBy(eq(primaryKey));
        verify(courtListService).emptyCourtList(eq(courtCentreId));

    }

    @Test
    public void shouldGetCasesByDefendantAndHearingDate(){
        final UUID caseId1 = randomUUID();
        final String caseUrn1 = randomAlphabetic(5);
        final UUID caseId2 = randomUUID();
        final String caseUrn2 = randomAlphabetic(5);
        final String hearingDate = "2023-05-26";

        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("listing.get.cases-by-person-defendant", createObjectBuilder().build());

        final CaseByDefendant caseByDefendant1 = CaseByDefendant.caseByDefendant().withCaseId(caseId1).withUrn(caseUrn1).build();
        final CaseByDefendant caseByDefendant2 = CaseByDefendant.caseByDefendant().withCaseId(caseId2).withUrn(caseUrn2).build();

        when(caseByDefendantRepository.getCasesByDefendantAndHearingDate( any(), any(), any())).thenReturn(Arrays.asList(caseByDefendant1, caseByDefendant2));

        final JsonEnvelope response = hearingsQueryView.getCasesByDefendantAndHearingDate(asList(randomUUID()), asList(randomUUID()), hearingDate, envelope);

        final JsonArray prosecutionCases = response.payloadAsJsonObject().getJsonArray("prosecutionCases");
        assertThat(prosecutionCases.getJsonObject(0).getString("caseId"), is(caseId1.toString()));
        assertThat(prosecutionCases.getJsonObject(0).getString("urn"), is(caseUrn1));
        assertThat(prosecutionCases.getJsonObject(1).getString("caseId"), is(caseId2.toString()));
        assertThat(prosecutionCases.getJsonObject(1).getString("urn"), is(caseUrn2));
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearingsWithCaseIdWhenOneOfApplicationHearingTypeIsReview() {

        final UUID applicationId = fromString("72876919-396e-4f9f-8c39-c678d7548120");
        final String testJsonString1 = "{ \"allocated\":\"" + true + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final String testJsonString2 = "{ \"allocated\":\"" + true + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{\"id\":\"72876919-396e-4f9f-8c39-c678d7548120\"}] , \"listedCases\" : [{}] , \"type\" : {\"id\":\"bd4dab38-ea91-434b-8e73-e0d50ef0cbdf\", \"description\":\"Review\"}}";
        final Hearing hearing1 = Hearing.builder().withId(randomUUID())
                .withProperties(JacksonUtil.toJsonNode(testJsonString1))
                .withListedCases(asSet(new ListedCases(randomUUID(), randomUUID(), null, null, null, null, null, false)))
                .build();
        final Hearing hearing2 = Hearing.builder().withId(randomUUID())
                .withProperties(JacksonUtil.toJsonNode(testJsonString2))
                .withListedCases(asSet(new ListedCases(randomUUID(), randomUUID(), null, null, null, null, null, false)))
                .withCourtApplications(asSet(new CourtApplications(randomUUID(), applicationId, null, null, null, null,null, false)))
                .build();


        final List<Hearing> hearingsJson =  newArrayList(hearing1, hearing2);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add("caseId", "caseId")
                        .build());
        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(
                "caseId"))
                .thenReturn(hearingsJson);
        hearingsJson.remove(hearing2);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);

        verify(hearingRepository).findAllocatedAndUnallocatedHearingsByCaseId(eq("caseId"));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
        assertEquals(1, ((JsonObject) results.payload()).getJsonArray("hearings").size());
    }

    @Test
    public void shouldSearchAllocatedAndUnallocatedHearingsWithCaseIdWhenOneOfCaseHearingTypeIsReview() {

        final String testJsonString1 = "{ \"allocated\":\"" + true + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final String testJsonString2 = "{ \"allocated\":\"" + true + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] , \"type\" : {\"id\":\"bd4dab38-ea91-434b-8e73-e0d50ef0cbdf\", \"description\":\"Review\"}}";

        final Hearing hearing1 = Hearing.builder().withId(randomUUID())
                .withProperties(JacksonUtil.toJsonNode(testJsonString1))
                .withListedCases(asSet(new ListedCases(randomUUID(), randomUUID(), null, null, null, null, null, false)))
                .build();
        final Hearing hearing2 = Hearing.builder().withId(randomUUID())
                .withProperties(JacksonUtil.toJsonNode(testJsonString2))
                .withListedCases(asSet(new ListedCases(randomUUID(), randomUUID(), null, null, null, null, null, false)))
                .build();


        final List<Hearing> hearingsJson =  newArrayList(hearing1, hearing2);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add("caseId", "caseId")
                        .build());
        when(hearingRepository.findAllocatedAndUnallocatedHearingsByCaseId(
                "caseId"))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));

        final JsonEnvelope results = hearingsQueryView.searchAllocatedAndUnallocatedHearings(query);

        verify(hearingRepository).findAllocatedAndUnallocatedHearingsByCaseId(eq("caseId"));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
        assertEquals(2, ((JsonObject) results.payload()).getJsonArray("hearings").size());
    }

    @Test
    public void testRangeSearch() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQuery.rangeSearchHearings(query)).thenReturn(rangeSearchQueryResponse);
        when(rangeSearchQueryResponse.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearings", createArrayBuilder().build()).build());

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearings(query);

        assertThat(results, is(rangeSearchQueryResponse));
        verify(rangeSearchQuery).rangeSearchHearings(eq(query));
    }

    @Test
    public void searchHearingsWithDateRangeWithAllOptionalParametersNotProvided() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQueryResponse.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearings", createArrayBuilder().build()).build());
        when(rangeSearchQuery.rangeSearchHearings(query)).thenReturn(rangeSearchQueryResponse);

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearings(query);

        assertThat(results, is(rangeSearchQueryResponse));
        verify(rangeSearchQuery).rangeSearchHearings(eq(query));
    }

    @Test
    public void shouldApplyApplicationTypeFilteringToRangeSearchHearings() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.search.hearings");

        final JsonEnvelope query = envelopeFrom(metadataBuilder,
                createObjectBuilder()
                        .add("allocated", true)
                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                        .build()
        );

        final JsonArray originalHearings = createArrayBuilder()
                .add(createObjectBuilder().add("id", hearingId1.toString()).build())
                .add(createObjectBuilder().add("id", hearingId2.toString()).build())
                .build();

        final JsonEnvelope originalResponse = envelopeFrom(metadataBuilder,
                createObjectBuilder()
                        .add("hearings", originalHearings)
                        .add("notes", createArrayBuilder().build())
                        .add("results", 2)
                        .add("pageCount", 1)
                        .build());

        final JsonArray filteredHearings = createArrayBuilder()
                .add(createObjectBuilder().add("id", hearingId1.toString()).build())
                .build();

        when(rangeSearchQuery.rangeSearchHearings(query)).thenReturn(originalResponse);
        when(applicationTypeFilter.filter(any(), any(JsonArray.class))).thenReturn(filteredHearings);

        final JsonEnvelope result = hearingsQueryView.rangeSearchHearings(query);

        verify(rangeSearchQuery).rangeSearchHearings(query);
        verify(applicationTypeFilter).filter(query.metadata(), originalHearings);

        assertThat(result.payloadAsJsonObject().getJsonArray("hearings").size(), is(1));
        assertThat(result.payloadAsJsonObject().getInt("results"), is(2));
        assertThat(result.payloadAsJsonObject().getInt("pageCount"), is(1));
    }

    @Test
    public void testRangeSearchHearingsForCourtCalendar() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQuery.rangeSearchCourtCalendar(query)).thenReturn(rangeSearchQueryResponse);
        when(rangeSearchQueryResponse.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearings", createArrayBuilder().build()).build());

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearingsForCourtCalendar(query);

        assertThat(results, is(rangeSearchQueryResponse));
        verify(rangeSearchQuery).rangeSearchCourtCalendar(eq(query));
    }

    @Test
    public void searchHearingsForCourtCalendarWithAllOptionalParametersNotProvided() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQueryResponse.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearings", createArrayBuilder().build()).build());
        when(rangeSearchQuery.rangeSearchCourtCalendar(query)).thenReturn(rangeSearchQueryResponse);

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearingsForCourtCalendar(query);

        assertThat(results, is(rangeSearchQueryResponse));
        verify(rangeSearchQuery).rangeSearchCourtCalendar(eq(query));
    }

    @Test
    public void shouldApplyApplicationTypeFilteringToRangeSearchHearingsForCourtCalendar() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.range.search.hearings.court.calendar");

        final JsonEnvelope query = envelopeFrom(metadataBuilder,
                createObjectBuilder()
                        .add("allocated", true)
                        .add("courtCentreId", COURT_CENTRE_ID.toString())
                        .build()
        );

        final JsonArray originalHearings = createArrayBuilder()
                .add(createObjectBuilder().add("id", hearingId1.toString()).build())
                .add(createObjectBuilder().add("id", hearingId2.toString()).build())
                .build();

        final JsonEnvelope originalResponse = envelopeFrom(metadataBuilder,
                createObjectBuilder()
                        .add("hearings", originalHearings)
                        .add("notes", createArrayBuilder().build())
                        .add("results", 2)
                        .add("pageCount", 1)
                        .build());

        final JsonArray filteredHearings = createArrayBuilder()
                .add(createObjectBuilder().add("id", hearingId1.toString()).build())
                .build();

        when(rangeSearchQuery.rangeSearchCourtCalendar(query)).thenReturn(originalResponse);
        when(applicationTypeFilter.filter(any(), any(JsonArray.class))).thenReturn(filteredHearings);

        final JsonEnvelope result = hearingsQueryView.rangeSearchHearingsForCourtCalendar(query);

        verify(rangeSearchQuery).rangeSearchCourtCalendar(query);
        verify(applicationTypeFilter).filter(query.metadata(), originalHearings);

        assertThat(result.payloadAsJsonObject().getJsonArray("hearings").size(), is(1));
        assertThat(result.payloadAsJsonObject().getInt("results"), is(2));
        assertThat(result.payloadAsJsonObject().getInt("pageCount"), is(1));
    }


    private JsonEnvelope generateQuery(final JsonValue payload) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(fromString("a595f500-08f4-44d1-99bb-5547a5bcc9a6"))
                        .withName("event.name"),

                payload
        );
    }

    private Hearing generateMinimalHearing() {
        return new Hearing(ID, JacksonUtil.toJsonNode(String.format("{ \"id\": \"%s\" }", ID.toString())));
    }


    private List<Hearing> hearingsJson(String allocated) {
        final String testJsonString = "{ \"allocated\":\"" + allocated + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        hearing1.setTotalCount(Long.valueOf(2));
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        return newArrayList(hearing1, hearing2);
    }

    private List<Hearing> singleHearingsJson(String allocated) {
        final String testJsonString = "{ \"allocated\":\"" + allocated + "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\"," +
                " \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        hearing1.setTotalCount(Long.valueOf(2));
        return newArrayList(hearing1);
    }

    private List<Hearing> childHearingsJson(String allocated) {
        final String testJsonString = "{ \"allocated\":\"" + allocated + "\", \"startDate\": \"2020-10-10\", \"courtRoomId\": \"42481915-4d98-437b-a5dd-ace41e2ab0ea\"," +
                " \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        hearing1.setTotalCount(Long.valueOf(2));
        return newArrayList(hearing1);
    }

    private JsonArray hearingsJsonArray() {
        return JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder()
                        .add("hello", "world"))
                .build();
    }

    private List<Hearing> hearingJsonForEjected() {
        final String testJsonString = "{\"hearings\":[{\"id\":\"54482cb7-31aa-4c64-8656-3be6e3a4d158\",\"listedCases\":[{\"isEjected\":\"true\"}],\"courtApplications\":[{\"isEjected\":\"true\"}]}]}";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        return newArrayList(hearing1, hearing2);

    }

    private Hearing getHearingById() {
        final String testJsonHearing = "{\"id\":\"b7b136da-7156-4391-ab0e-24e90c2bc599\",\"endDate\":\"END_DATE\",\"allocated\":false,\"startDate\":\"2020-03-17\",\"listedCases\":[{\"id\":\"523b6826-3e56-48d0-bb1b-a0209e7b9c70\",\"defendants\":[{\"id\":\"367f1f6d-f300-4e35-9a8b-92f0c81a28b5\",\"masterDefendantId\":\"d676f354-ba50-462e-bd55-4e8842d29ebd\"}],\"caseIdentifier\":{\"caseReference\":\"EEE555\"},\"restrictFromCourtList\":false,\"linkedCases\":[{\"caseId\":\"367f1f6d-f300-4e35-9a8b-92f0c81a298e\",\"caseUrn\":\"URN1\"},{\"caseId\":\"367f1f6d-f300-4e35-9a8b-92f0c81a246g\",\"caseUrn\":\"URN2\"}]}]}";
        String testJson = testJsonHearing.replace("END_DATE", LocalDate.now().toString());
        return new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJson));
    }

    private Hearing getUnscheduledHearingById() {
        final String testJsonHearing = "{\"id\":\"b7b136da-7156-4391-ab0e-24e90c2bc599\",\"allocated\":false,\"unscheduled:\":true,\"startDate\":\"2020-03-17\",\"listedCases\":[{\"id\":\"523b6826-3e56-48d0-bb1b-a0209e7b9c70\",\"defendants\":[{\"id\":\"367f1f6d-f300-4e35-9a8b-92f0c81a28b5\",\"masterDefendantId\":\"d676f354-ba50-462e-bd55-4e8842d29ebd\"}],\"caseIdentifier\":{\"caseReference\":\"EEE555\"},\"restrictFromCourtList\":false,\"linkedCases\":[{\"caseId\":\"367f1f6d-f300-4e35-9a8b-92f0c81a298e\",\"caseUrn\":\"URN1\"},{\"caseId\":\"367f1f6d-f300-4e35-9a8b-92f0c81a246g\",\"caseUrn\":\"URN2\"}]}]}";
        return new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonHearing));
    }

    private List<Notes> createNotesList() {
        return newArrayList(new Notes(randomUUID(), fromString("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18"), LocalDates.from("2020-09-03"), "Note 1"));
    }

    private Matcher<? super ReadContext> getMatcherForNotes(boolean exist) {
        if (exist) {
            return allOf(withJsonPath("$.notes.size()", equalTo(1)),
                    withJsonPath("$.notes[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")));
        } else {
            return allOf(withJsonPath("$.notes.size()", equalTo(0)));
        }

    }

    private Hearing createHearing(final UUID hearingId) {
        final Hearing hearing = new Hearing();
        hearing.setId(hearingId);
        return hearing;
    }


    @Test
    void shouldGenerateCsvReport() {
        // Given
        final String courtCentreId = "123e4567-e89b-12d3-a456-426614174000";
        final String startDate = "2024-01-01";
        final Integer numberOfWeeks = 2;
        final UUID judiciaryId = randomUUID();
        final String judiciaryJson = String.format("%s", judiciaryId);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.query.download-hearing-csv-report"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("numberOfWeeks", numberOfWeeks)
                        .build());

        final List<HearingCsvData> csvData = new ArrayList<>();
        csvData.add(new HearingCsvData(
                LocalDate.parse("2024-01-01"),
                "From 2024-01-01 to 2024-01-07",
                "Courtroom 1",
                judiciaryJson, // Use JSON format instead of simple string
                "09:00",
                "Trial",
                "120",
                "Day 1 of 1",
                "URN123",
                "", // caseIds
                "John Doe",
                "Flag",
                "Offence",
                "Note",
                "English",
                "Video",
                "CTL 2025",
                "Multi-day details",
                "Pinned",
                "Unpinned",
                "Markers",
                "Restriction",
                CsvRecordType.CASE
        ));

        when(hearingCsvReportService.findHearingsForCsvReport(eq(courtCentreId), eq(LocalDate.parse(startDate)), eq(numberOfWeeks), anyLong()))
                .thenReturn(csvData);
        when(hearingCsvReportService.generateCsvContent(csvData, query))
                .thenReturn("Date of hearing,Fixed/week commencing,Courtroom,Judiciary,Time,Hearing type,Duration,Day,URN/s,Defendant Names,Deft flag,Offences,Public list note,Language,Video Hearing,Custody status,Multi-day hearing details,Pinned notes,Unpinned notes,Markers,Reporting Restriction\n2024-01-01,From 2024-01-01 to 2024-01-07,Courtroom 1,His Honour Judge Smith,09:00,Trial,120,Day 1 of 1,URN123,John Doe,Flag,Offence,Note,English,Video,CTL 2025,Multi-day details,Pinned,Unpinned,Markers,Restriction");

        // When
        final String result = hearingsQueryView.generateHearingCsvReport(query);

        // Then
        assertThat(result, is(not(emptyString())));
        assertThat(result, containsString("Date of hearing"));
        assertThat(result, containsString("Courtroom"));
        assertThat(result, containsString("His Honour Judge Smith")); // Should contain resolved name
        verify(hearingCsvReportService).findHearingsForCsvReport(eq(courtCentreId), eq(LocalDate.parse(startDate)), eq(numberOfWeeks), anyLong());
        verify(hearingCsvReportService).generateCsvContent(csvData, query);
    }

    @Test
    void shouldReturnOnlyHeadersWhenNoDataForCsvReport() {
        // Given
        final String courtCentreId = "123e4567-e89b-12d3-a456-426614174000";
        final String startDate = "2024-01-01";
        final Integer numberOfWeeks = 2;

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.query.download-hearing-csv-report"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("numberOfWeeks", numberOfWeeks)
                        .build());

        when(hearingCsvReportService.findHearingsForCsvReport(eq(courtCentreId), eq(LocalDate.parse(startDate)), eq(numberOfWeeks), anyLong()))
                .thenReturn(new ArrayList<>());
        when(hearingCsvReportService.generateCsvContent(new ArrayList<>(), query))
                .thenReturn("Date of hearing,Fixed/week commencing,Courtroom,Judiciary,Time,Hearing type,Duration,Day,URN/s,Defendant Names,Deft flag,Offences,Public list note,Language,Video Hearing,Custody status,Multi-day hearing details,Pinned notes,Unpinned notes,Markers,Reporting Restriction");

        // When
        final String result = hearingsQueryView.generateHearingCsvReport(query);

        // Then
        assertThat(result, containsString("Courtroom"));
        long lineCount = result.split("\n").length;
        assertThat(lineCount, is(1L));

        verify(hearingCsvReportService).findHearingsForCsvReport(eq(courtCentreId), eq(LocalDate.parse(startDate)), eq(numberOfWeeks), anyLong());
        verify(hearingCsvReportService).generateCsvContent(new ArrayList<>(), query);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenInvalidDateFormat() {
        // Given
        final String courtCentreId = "123e4567-e89b-12d3-a456-426614174000";
        final String invalidStartDate = "invalid-date";
        final Integer numberOfWeeks = 2;

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.query.download-hearing-csv-report"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", invalidStartDate)
                        .add("numberOfWeeks", numberOfWeeks)
                        .build());

        // When & Then
        final BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> hearingsQueryView.generateHearingCsvReport(query));

        assertThat(exception.getMessage(), is("Invalid start date: " + invalidStartDate));
    }

}