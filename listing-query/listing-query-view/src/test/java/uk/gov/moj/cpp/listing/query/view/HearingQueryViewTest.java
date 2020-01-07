package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.parse;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusResult;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;
import uk.gov.moj.cpp.listing.query.view.courtlist.CourtListService;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.NotFoundException;

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
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
    private static final UUID ID = UUID.fromString("7c5e9d0c-9e28-46a9-b139-68fc0813842c");
    private static final boolean ALLOCATED = true;
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

    private static final String COURT_ROOM_QUERY_PARAMETER = "courtRoomId";
    private static final String AUTHORITY_ID_QUERY_PARAMETER = "authorityId";
    private static final String HEARING_TYPE_QUERY_PARAMETER = "hearingTypeId";
    private static final String JURISDICTION_TYPE_QUERY_PARAMETER = "jurisdictionType";
    private static final String LIST_ID_QUERY_PARAMETER = "listId";
    private static final String ID_PARAMETER = "id";
    private static final String AUTHORITY_ID = "efa4e01b-1dc5-48c5-80b5-c3858a7622d6";
    private static final String AUTHORITY_ID_SEARCH = String.format(HearingRepository.AUTHORITY_ID_SEARCH, AUTHORITY_ID);
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final LocalDate SEARCH_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final LocalTime END_TIME = LocalTime.now();

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

    @InjectMocks
    private HearingQueryView hearingsQueryView;

    private static final String PUBLISH_DATE = "2012-12-11";

    private static final String PUBLISH_DATE_WEEK_COMMENCING = "2012-12-13";

    @Test
    public void shouldReturnCorrectPublishCourtListStatusForWeekCommencing() {

        final Set<PublishCourtListType> publishCourtListTypeSet = new HashSet();
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
        assertThat(results, is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.court.list.publish.status"), payloadIsJson(
                allOf(
                        withJsonPath("$.publishCourtListStatuses", hasSize(1)),
                        withJsonPath("$.publishCourtListStatuses[0].publishStatus", equalTo(EXPORT_SUCCESSFUL.name())),
                        withJsonPath("$.publishCourtListStatuses[0].publishCourtListType", equalTo(FINAL.toString()))
                )))));

    }

    @Test
    public void shouldReturnCorrectPublishCourtListStatusForFixedDate() {

        final Set<PublishCourtListType> publishCourtListTypeSet = new HashSet();
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
        assertThat(results, is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.court.list.publish.status"), payloadIsJson(
                allOf(
                        withJsonPath("$.publishCourtListStatuses", hasSize(1)),
                        withJsonPath("$.publishCourtListStatuses[0].publishStatus", equalTo(EXPORT_SUCCESSFUL.name())),
                        withJsonPath("$.publishCourtListStatuses[0].publishCourtListType", equalTo(FINAL.toString()))
                )))));
    }


    private List<CourtListPublishStatusResult> publishCourtListStatuses() {
        final UUID courtCentreId1 = randomUUID();
        final CourtListPublishStatusResult publishCourtListStatus3 = new CourtListPublishStatusResult(courtCentreId1, FINAL, now(), EXPORT_SUCCESSFUL, "");
        return newArrayList(publishCourtListStatus3);
    }

    @Test
    public void searchHearingsWithSearchDateWithAllParametersProvided() {

        final List<Hearing> hearingsJson = hearingsJson();
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID_SEARCH,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.atTime(START_TIME).toString(),
                SEARCH_DATE.atTime(END_TIME).toString()))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);
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

        assertThat(results, is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.search.hearings"),
                payloadIsJson(
                        withJsonPath("$.hearings[0].hello", equalTo("world"))
                ))
        ));

    }

    @Test
    public void testRangeSearch() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQuery.rangeSearchHearings(query)).thenReturn(rangeSearchQueryResponse);

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearings(query);

        assertThat(results, is(rangeSearchQueryResponse));
    }

    @Test
    public void searchHearingsWithDateRangeWithAllOptionalParametersNotProvided() {

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchQueryResponse = mock(JsonEnvelope.class);

        when(rangeSearchQuery.rangeSearchHearings(query)).thenReturn(rangeSearchQueryResponse);

        final JsonEnvelope results = hearingsQueryView.rangeSearchHearings(query);

        assertThat(results, is(rangeSearchQueryResponse));
    }


    @Test
    public void getAlphabeticalCourtListContentWithAllParamsProvided() throws Exception {

        final List<Hearing> hearingsJson = hearingsJson();
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        when(hearingRepository.findHearingsForAlphabeticalList(ALLOCATED, COURT_CENTRE_ID.toString(), LocalDates.to(SEARCH_DATE)))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convertHearingResultForAlphbeticalList(hearingsJson))
                .thenReturn(hearingsJsonArray);

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

        assertThat(results, is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.search.court.list"),
                payloadIsJson(
                        withJsonPath("$.hearings[0].hello", equalTo("world"))
                ))
        ));

    }


    @Test
    public void searchHearingsAllCaseApplicationsEjected() throws Exception {

        final List<Hearing> hearingsJson = hearingJsonForEjected();

        when(hearingRepository.findHearings(
                ALLOCATED,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID_SEARCH,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.atTime(START_TIME).toString(),
                SEARCH_DATE.atTime(END_TIME).toString()))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenCallRealMethod();

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
                is(jsonEnvelope(withMetadataEnvelopedFrom(query)
                                .withName("listing.search.hearing"),
                        payloadIsJson(
                                withJsonPath("$.id", equalTo(ID.toString()))
                        ))
                ));
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
                is(jsonEnvelope(withMetadataEnvelopedFrom(queryEnvelope)
                                .withName("listing.publishedcourtlist"),
                        payloadIsJson(
                                withJsonPath("$", equalTo(expectedPayload))
                        ))
                ));
    }

    @Test
    public void shouldRetrieveUnpublishedCourtList() {

        final UUID courtCentreId = UUID.randomUUID();
        final uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType = uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonEnvelope queryEnvelope = generateQuery(
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("publishCourtListType", publishCourtListType.name())
                        .add("startDate", startDate.toString())
                        .build());

        final JsonObject courtListResponsePayload = Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .build();

        when(courtListService.retrieveCourtList(courtCentreId,publishCourtListType,
                startDate,
                queryEnvelope)).thenReturn(courtListResponsePayload);

        final JsonEnvelope results = hearingsQueryView.retrieveCourtList(queryEnvelope);

        assertThat(results,
                is(jsonEnvelope(withMetadataEnvelopedFrom(queryEnvelope)
                                .withName("listing.courtlist"),
                        payloadIsJson(
                                withJsonPath("$.courtCentreId", equalTo(courtCentreId.toString()))
                        ))
                ));
    }

    @Test
    public void shouldRetrievePublishedCourtList() {

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
                ZonedDateTime.now(), ZonedDateTime.now());

        final PublishedCourtListPrimaryKey primaryKey = new PublishedCourtListPrimaryKey(
                courtCentreId,
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(publishCourtListType.name()),
                startDate);

        when(publishedCourtListRepository.findBy(primaryKey)).thenReturn(publishedCourtList);
        when(publishedCourtListToJsonConverter.convert(publishedCourtList)).thenReturn(expectedPayload);

        final JsonEnvelope results = hearingsQueryView.retrieveCourtList(queryEnvelope);

        assertThat(results.payloadAsJsonObject(), is(courtListJson));
    }

    private JsonEnvelope generateQuery(final JsonValue payload) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(UUID.fromString("a595f500-08f4-44d1-99bb-5547a5bcc9a6"))
                        .withName("event.name"),

                payload
        );
    }

    private Hearing generateMinimalHearing() {
        return new Hearing(ID, JacksonUtil.toJsonNode(String.format("{ \"id\": \"%s\" }", ID.toString())));
    }


    private List<Hearing> hearingsJson() {
        final String testJsonString = "{ \"hello\": \"world\" }";
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

}
