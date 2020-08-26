package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
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
    private static final String PUBLISH_COURT_LIST_TYPES = "FIRM,FINAL";
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID ID = UUID.fromString("7c5e9d0c-9e28-46a9-b139-68fc0813842c");
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
    private static final String AUTHORITY_ID_SEARCH = String.format(HearingRepository.AUTHORITY_ID_SEARCH, AUTHORITY_ID);
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final LocalDate SEARCH_DATE = LocalDate.now();

    private static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.now();
    private static final LocalDate WEEK_COMMENCING_END_DATE = LocalDate.now().plusDays(7);

    private static final String EARLIEST_SEARCH_DATE = "1900-01-01";
    private static final String LATEST_SEARCH_DATE = "9999-01-01";

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Logger logger;

    @Mock
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    @InjectMocks
    private RangeSearchQuery rangeSearchQuery;

    @Test
    public void searchHearingsWithDateRangeWithAllParametersProvided() {

        final List<Hearing> hearingsJson = hearingsJson();
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        when(hearingRepository.findHearings(
                ALLOCATEDSTR,
                COURT_CENTRE_ID.toString(),
                COURT_ROOM_ID.toString(),
                AUTHORITY_ID_SEARCH,
                HEARING_TYPE_ID.toString(),
                JURISDICTION_TYPE.toString(),
                SEARCH_DATE.toString(),
                SEARCH_DATE.toString()))
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
                        .add(START_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .add(END_DATE_QUERY_PARAMETER, SEARCH_DATE.toString())
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals("world", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("hello"));
        assertEquals( "listing.search.hearings", results.metadata().name());
    }

    @Test
    public void searchHearingsWithWeekCommencingDateRange() {

        final List<Hearing> hearingsJson = hearingJsonForWeekCommencing();
        final JsonArray hearingsJsonArray = hearingsForWeekCommencingJsonArray();

        when(hearingRepository.findHearingsByWeekCommencingRange(
                null,
                null,
                AUTHORITY_ID_SEARCH,
                null,
                null,
                WEEK_COMMENCING_START_DATE.toString(),
                WEEK_COMMENCING_END_DATE.toString()))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, WEEK_COMMENCING_END_DATE.toString())
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals( "2019-10-13", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("weekCommencingStartDate"));
        assertEquals( "listing.search.hearings", results.metadata().name());
    }

    @Test
    public void searchUnallocatedHearingsWithWeekCommencingDateRange() {

        final List<Hearing> hearingsJson = hearingJsonForWeekCommencing();
        final JsonArray hearingsJsonArray = hearingsForWeekCommencingJsonArray();

        when(hearingRepository.findUnallocatedHearingsByWeekCommencingRange(
                null,
                null,
                AUTHORITY_ID_SEARCH,
                null,
                null,
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE,
                false))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, false)
                        .add(AUTHORITY_ID_QUERY_PARAMETER, AUTHORITY_ID)
                        .add(WEEK_COMMENCING_START_DATE_QUERY_PARAMETER, WEEK_COMMENCING_START_DATE.toString())
                        .add(WEEK_COMMENCING_END_DATE_QUERY_PARAMETER, "")
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals("2019-10-13", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("weekCommencingStartDate"));
        assertEquals( "listing.search.hearings", results.metadata().name());
    }


    @Test
    public void searchHearingsWithDateRangeWithAllOptionalParametersNotProvided() {

        final List<Hearing> hearingsJson = hearingsJson();
        final JsonArray hearingsJsonArray = hearingsJsonArray();

        when(hearingRepository.findHearings(
                ALLOCATEDSTR,
                "null",
                "null",
                HearingRepository.ALL_AUTHORITY_CODES_SEARCH,
                "null",
                "null",
                EARLIEST_SEARCH_DATE,
                LATEST_SEARCH_DATE
        ))
                .thenReturn(hearingsJson);
        when(hearingJsonListConverterFilterEjectCases.convert(hearingsJson))
                .thenReturn(hearingsJsonArray);

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .build());

        final JsonEnvelope results = rangeSearchQuery.rangeSearchHearings(query);

        assertEquals( "world", results.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0).getString("hello"));
        assertEquals("listing.search.hearings", results.metadata().name());
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

    private List<Hearing> hearingJsonForWeekCommencing() {
        final String testJsonString = "{\n" +
                "\t\"hearings\": [{\n" +
                "\t\t\"id\": \"54482cb7-31aa-4c64-8656-3be6e3a4d158\",\n" +
                "\t\t\"weekCommencingStartDate\": \"2019-10-13\",\n" +
                "\t\t\"weekCommencingEndDate\": \"2019-10-25\",\n" +
                "\t\t\"listedCases\": [{\n" +
                "\t\t}],\n" +
                "\t\t\"courtApplications\": [{\n" +
                "\t\t}]\n" +
                "\t}]\n" +
                "}";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        return newArrayList(hearing1, hearing2);

    }

    private JsonArray hearingsForWeekCommencingJsonArray() {
        return Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("weekCommencingStartDate", "2019-10-13"))
                .build();
    }
}
