package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.parse;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusResult;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;
import uk.gov.moj.cpp.listing.query.view.courtlist.CourtListService;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

import java.io.IOException;
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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.ReadContext;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
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
    private static final String AUTHORITY_ID_SEARCH = String.format(HearingRepository.AUTHORITY_ID_SEARCH, AUTHORITY_ID);
    private static final String PROSECUTOR_ID_SEARCH = String.format(HearingRepository.PROSECUTOR_ID_SEARCH, AUTHORITY_ID);
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final LocalDate SEARCH_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final LocalTime END_TIME = LocalTime.now();
    private static final String PUBLISH_DATE = "2012-12-11";
    private static final String PUBLISH_DATE_WEEK_COMMENCING = "2012-12-13";
    private static final String EMPTY_STRING = "";


    @Spy
    private Enveloper enveloper = createEnveloper();
    @Mock
    private HearingRepository hearingRepository;
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
    @InjectMocks
    private HearingQueryView hearingsQueryView;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    private ListToJsonArrayConverter listToJsonArrayConverter = new ListToJsonArrayConverter();

    @Before
    public void setup() throws IllegalAccessException {
        final ObjectMapper objectMapper = new ObjectMapper();
        FieldUtils.writeField(this.listToJsonArrayConverter, "mapper", objectMapper, true);
        FieldUtils.writeField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter, true);
        FieldUtils.writeField(this.hearingsQueryView, "listToJsonArrayConverter", listToJsonArrayConverter, true);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("hello", "world"))
                .build();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID_SEARCH,
                PROSECUTOR_ID_SEARCH,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.atTime(LocalTime.MIN).toString(),
                SEARCH_DATE.atTime(END_TIME).toString()))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convertForSearchHearing(anyListOf(Hearing.class), anyMapOf(String.class, String.class))).thenReturn(hearingsJsonArray);

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
                eq(AUTHORITY_ID_SEARCH), eq(PROSECUTOR_ID_SEARCH), eq(HEARING_TYPE_ID.toString()), eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE.toString()),
                eq(SEARCH_DATE.atTime(LocalTime.MIN).toString()), eq(SEARCH_DATE.atTime(END_TIME).toString()));
        verify(hearingJsonListConverterFilterEjectCases).convertForSearchHearing(eq(hearingsJson), anyMapOf(String.class, String.class));
    }

    @Test
    public void searchHearingsWithSearchDateWithAllParametersProvided() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("hello", "world"))
                .build();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID_SEARCH,
                PROSECUTOR_ID_SEARCH,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.atTime(START_TIME).toString(),
                SEARCH_DATE.atTime(END_TIME).toString()))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convertForSearchHearing(anyListOf(Hearing.class), anyMapOf(String.class, String.class))).thenReturn(hearingsJsonArray);
        when(notesService.findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), any(List.class))).thenReturn(createNotesList());

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
                eq(AUTHORITY_ID_SEARCH), eq(PROSECUTOR_ID_SEARCH), eq(HEARING_TYPE_ID.toString()), eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE.toString()),
                eq(SEARCH_DATE.atTime(START_TIME).toString()), eq(SEARCH_DATE.atTime(END_TIME).toString()));
        verify(hearingJsonListConverterFilterEjectCases).convertForSearchHearing(eq(hearingsJson), anyMapOf(String.class, String.class));
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
                null))
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
                caseUrnForLinkedCase))
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

    public void searchAvailableHearingsWithWithAllParametersProvided(final Hearing returnedHearing, final boolean notesExist) throws Exception {

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

        when(hearingRepository.findHearings(
                ALLOCATED,
                jurisdictionTypeSet,
                ID.toString(),
                CASE_URN_SET,
                MASTER_DEFENDANT_IDS,
                LINKED_CASE_SET,
                null))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(new HearingJsonListConverterFilterEjectCases().convert(hearingsJson));
        if (notesExist) {
            when(notesService.findNotes(eq(ALLOCATED), eq(null), eq(null), any(List.class))).thenReturn(createNotesList());
        }
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(JURISDICTION_TYPE_QUERY_PARAMETER, JURISDICTION_TYPE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(HEARING_ID_QUERY_PARAMETER, ID.toString())
                        .add(CASE_URN_QUERY_PARAMETER, CASE_URN_SET.stream().collect(Collectors.joining(",")))
                        .add(SEARCH_CRITERIA_QUERY_PARAMETER, SEARCH_CRITERIA)
                        .build());

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
        verify(hearingRepository).findHearings(eq(ALLOCATED), eq(jurisdictionTypeSet),
                eq(ID.toString()), eq(CASE_URN_SET), eq(MASTER_DEFENDANT_IDS), eq(LINKED_CASE_SET), eq(null));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
    }

    @Test
    public void shouldSearchUnscheduledHearingsWithoutCourtCentreIds() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        when(hearingRepository.findHearings(
                "caseUrnValue", "typeOfListValue"))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_URN, "caseUrnValue")
                        .add(TYPE_OF_LIST, "typeOfListValue")
                        .build());

        final Envelope<JsonObject> results = hearingsQueryView.searchUnscheduledHearings(query);

        assertThat(envelopeFrom(results.metadata(), results.payload()),
                is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.unscheduled.search.hearings"),
                        payloadIsJson(
                                withJsonPath("$.hearings[0].hello", equalTo("world"))
                        ))
                ));
        verify(hearingRepository).findHearings(eq("caseUrnValue"), eq("typeOfListValue"));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
    }

    @Test
    public void shouldSearchHearingsWithAnyAllocationWithCaseUrn() throws IOException {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        when(hearingRepository.findHearingsByCaseUrnAndAnyAllocationState("CASEURNVALUE"))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_URN, "caseUrnValue")
                        .build());

        final Envelope<JsonObject> results = hearingsQueryView.searchHearingsWithAnyAllocationState(query);

        assertThat(JsonEnvelope.envelopeFrom(results.metadata(), results.payload()),
                is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.any-allocation.search.hearings"),
                        payloadIsJson(
                                withJsonPath("$.hearings[0].hello", equalTo("world"))
                        ))
                ));
        verify(hearingRepository).findHearingsByCaseUrnAndAnyAllocationState(eq("CASEURNVALUE"));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
    }

    @Test
    public void shouldSearchUnscheduledHearingsWithCourtCentreIds() {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        final HashSet<String> courtCentreIds = new HashSet<>(Arrays.asList("courtCentreId1", "courtCentreId2"));
        when(hearingRepository.findHearings(
                "caseUrnValue", "typeOfListValue", courtCentreIds))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(CASE_URN, "caseUrnValue")
                        .add(TYPE_OF_LIST, "typeOfListValue")
                        .add(COURT_CENTRE_IDS, "courtCentreId1,courtCentreId2")
                        .build());

        final Envelope<JsonObject> results = hearingsQueryView.searchUnscheduledHearings(query);

        assertThat(envelopeFrom(results.metadata(), results.payload()),
                is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.unscheduled.search.hearings"),
                        payloadIsJson(
                                withJsonPath("$.hearings[0].hello", equalTo("world"))
                        ))
                ));
        verify(hearingRepository).findHearings(eq("caseUrnValue"), eq("typeOfListValue"), eq(courtCentreIds));
        verify(hearingJsonListConverterFilterEjectCases).convert(eq(hearingsJson));
    }

    @Test
    public void testRangeSearch() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQuery.rangeSearchHearings(query)).thenReturn(rangeSearchQueryResponse);

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearings(query);

        assertThat(results, is(rangeSearchQueryResponse));
        verify(rangeSearchQuery).rangeSearchHearings(eq(query));
    }

    @Test
    public void searchHearingsWithDateRangeWithAllOptionalParametersNotProvided() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQuery.rangeSearchHearings(query)).thenReturn(rangeSearchQueryResponse);

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearings(query);

        assertThat(results, is(rangeSearchQueryResponse));
        verify(rangeSearchQuery).rangeSearchHearings(eq(query));
    }

    @Test
    public void getAlphabeticalCourtListContentWithAllParamsProvided() throws Exception {

        final List<Hearing> hearingsJson = hearingsJson(ALLOCATEDSTR);
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        when(hearingRepository.findHearingsForAlphabeticalList(ALLOCATED, COURT_CENTRE_ID.toString(), LocalDates.to(SEARCH_DATE)))
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
        verify(hearingRepository).findHearingsForAlphabeticalList(eq(ALLOCATED), eq(COURT_CENTRE_ID.toString()), eq(LocalDates.to(SEARCH_DATE)));
        verify(hearingJsonListConverterFilterEjectCases).convertHearingResultForAlphabeticalList(eq(hearingsJson));
    }

    @Test
    public void searchHearingsAllCaseApplicationsEjected() {
        final List<Hearing> hearingsJson = hearingJsonForEjected();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID_SEARCH,
                PROSECUTOR_ID_SEARCH,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.atTime(START_TIME).toString(),
                SEARCH_DATE.atTime(END_TIME).toString()))
                .thenReturn(hearingsJson);

        when(hearingJsonListConverterFilterEjectCases.convertForSearchHearing(anyListOf(Hearing.class), anyMapOf(String.class, String.class))).thenCallRealMethod();
        when(notesService.findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), any(List.class))).thenReturn(createNotesList());

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
                eq(AUTHORITY_ID_SEARCH), eq(PROSECUTOR_ID_SEARCH), eq(HEARING_TYPE_ID.toString()), eq(JURISDICTION_TYPE.toString()), eq(SEARCH_DATE.toString()),
                eq(SEARCH_DATE.atTime(START_TIME).toString()), eq(SEARCH_DATE.atTime(END_TIME).toString()));

        verify(hearingJsonListConverterFilterEjectCases).convertForSearchHearing(eq(hearingsJson), anyMapOf(String.class, String.class));
        verify(notesService, times(1)).findNotes(eq(ALLOCATED), eq(COURT_ROOM_ID.toString()), eq(SEARCH_DATE.toString()), any(List.class));
    }

    @Test(expected = NullPointerException.class)
    public void findHearingByIdWhenIdIsAbsent() {

        final JsonEnvelope query = generateQuery(
                createObjectBuilder()
                        .build());

        hearingsQueryView.getHearingById(query);
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
            assertEquals("There is no Hearing for that ID.", ex.getMessage());
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
        final JsonObject expectedPayload = Json.createObjectBuilder().build();

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

        final UUID courtCentreId = UUID.randomUUID();
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

        final JsonObject courtListResponsePayload = Json.createObjectBuilder()
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

        final UUID courtCentreId = UUID.randomUUID();
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

        final JsonObject expectedPayload = Json.createObjectBuilder()
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

        final UUID courtCentreId = UUID.randomUUID();
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

        final JsonObject emptyCourtListJson = Json.createObjectBuilder().build();

        when(publishedCourtListRepository.findBy(primaryKey)).thenReturn(null);
        when(courtListService.emptyCourtList(courtCentreId)).thenReturn(emptyCourtListJson);

        final JsonEnvelope response = hearingsQueryView.retrieveCourtList(queryEnvelope);

        assertThat(response.payloadAsJsonObject(), is(emptyCourtListJson));
        verify(publishedCourtListRepository).findBy(eq(primaryKey));
        verify(courtListService).emptyCourtList(eq(courtCentreId));

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
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        return newArrayList(hearing1, hearing2);
    }

    private JsonArray hearingsJsonArray() {
        return Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
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
        return new Hearing(UUID.randomUUID(), JacksonUtil.toJsonNode(testJson));
    }

    private Hearing getUnscheduledHearingById() {
        final String testJsonHearing = "{\"id\":\"b7b136da-7156-4391-ab0e-24e90c2bc599\",\"allocated\":false,\"unscheduled:\":true,\"startDate\":\"2020-03-17\",\"listedCases\":[{\"id\":\"523b6826-3e56-48d0-bb1b-a0209e7b9c70\",\"defendants\":[{\"id\":\"367f1f6d-f300-4e35-9a8b-92f0c81a28b5\",\"masterDefendantId\":\"d676f354-ba50-462e-bd55-4e8842d29ebd\"}],\"caseIdentifier\":{\"caseReference\":\"EEE555\"},\"restrictFromCourtList\":false,\"linkedCases\":[{\"caseId\":\"367f1f6d-f300-4e35-9a8b-92f0c81a298e\",\"caseUrn\":\"URN1\"},{\"caseId\":\"367f1f6d-f300-4e35-9a8b-92f0c81a246g\",\"caseUrn\":\"URN2\"}]}]}";
        return new Hearing(UUID.randomUUID(), JacksonUtil.toJsonNode(testJsonHearing));
    }

    private List<Notes> createNotesList() {
        return newArrayList(new Notes(UUID.randomUUID(), UUID.fromString("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18"), LocalDates.from("2020-09-03"), "Note 1"));
    }

    private Matcher<? super ReadContext> getMatcherForNotes(boolean exist) {
        if (exist) {
            return allOf(withJsonPath("$.notes.size()", equalTo(1)),
                    withJsonPath("$.notes[0].courtRoomId", equalTo("6e424105-55f4-4e1a-bb9e-6ffbae3f7c18")));
        } else {
            return allOf(withJsonPath("$.notes.size()", equalTo(0)));
        }

    }
}