package uk.gov.moj.cpp.listing.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FileUtils.readLines;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.testutil.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createArrayBuilder;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createReader;
import static uk.gov.moj.cpp.listing.testutil.Envelopes.metadataBuilder;
import static uk.gov.moj.cpp.listing.testutil.EnvelopeMatchers.jsonEnvelope;
import static uk.gov.moj.cpp.listing.testutil.EnvelopeMatchers.metadata;
import static uk.gov.moj.cpp.listing.testutil.EnvelopeMatchers.payloadIsJson;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ALPHABETICAL;
import static uk.gov.moj.cpp.listing.domain.CourtListType.JUDGE;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ONLINE_PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PRISON;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.USHERS_MAGISTRATE;

import uk.gov.moj.cpp.listing.query.view.exception.QueryBadRequestException;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import uk.gov.moj.cpp.listing.testutil.Envelopes;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataLoader;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.query.api.service.AlphabeticalCourtListService;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.document.generator.JudgeListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.document.generator.JudiciaryNameMapper;
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingQueryApiTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private static final String PATH_TO_RAML = "src/raml/listing-query-api.raml";
    private static final String NAME = "name:";
    private static final String LISTING_SEARCH = "listing.search";
    private static final String LISTING_SEARCH_HEARING = "listing.search.hearing";
    private static final String LISTING_UNSCHEDULED_SEARCH_HEARING = "listing.unscheduled.search.hearings";
    private static final String LISTING_ALLOCATED_AND_UNALLOCATED_HEARING = "listing.allocated.and.unallocated.hearings";
    private static final String LISTING_ANY_ALLOCATION_SEARCH_HEARINGS = "listing.any-allocation.search.hearings";
    private static final String LISTING_COTR_SEARCH_HEARING = "listing.cotr.search.hearings";
    private static final String LISTING_RANGE_SEARCH = "listing.range";
    private static final String LISTING_COURT_LIST_PUBLISH_STATUS = "listing.court.list.publish.status";
    private static final String HEARING_SLOTS = "listing.search.hearing.slots";
    private static final String LISTING_AVAILABLE_HEARING_SEARCH = "listing.available";
    private static final String LISTING_SEARCH_BY_PERSON_DEFENDANT = "listing.get.cases-by-person-defendant";
    private static final String LISTING_SEARCH_BY_ORGANISATION_DEFENDANT = "listing.get.cases-by-organisation-defendant";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String LIST_ID = "listId";

    private static final List<String> METHODS_WHICH_ARE_NOT_MERELY_PASS_THROUGH = ImmutableList.of(
            "searchForHearingById",
            "searchHearingSlots",
            "searchUnscheduledHearings");
    public static final String LISTING_RANGE_SEARCH_HEARINGS_COURT_CALENDAR = "listing.range.search.hearings.court.calendar";

    private Map<String, String> apiMethodsToHandlerNames;

    @Mock
    private HearingQueryView hearingQueryView;

    @Mock
    private ReferenceDataLoader referenceDataLoader;

    @Mock
    private StandardPublicCourtListTemplateAssembler standardPublicCourtListAssembler;

    @Mock
    private AlphabeticalCourtListService alphabeticalCourtListService;

    @Mock
    private JudgeListTemplateAssembler judgeListTemplateAssembler;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private HearingQueryApi hearingQueryApi;

    @Mock
    CppQueryClient cppQueryClient;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JudiciaryNameMapper judiciaryNameMapper;

    @Captor
    private ArgumentCaptor<JsonObject> requesterCaptor;




    @Test
    public void searchForHearingByIdWhenIdIsNotPresent() {


        final JsonObject proposedQuery = generateQuery(createObjectBuilder().build());

        final IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> hearingQueryApi.searchForHearingById(proposedQuery));

        assertThat(illegalArgumentException.getMessage(), is("Attempted to search for a Hearing without an ID."));
    }

    @Test
    public void searchForHearingByIdWhenIdIsNotValid() {
        final JsonObject proposedQuery = generateQuery(
                createObjectBuilder()
                        .add("id", "849afced-85b")
                        .build());

        final QueryBadRequestException badRequestException = assertThrows(
                QueryBadRequestException.class,
                () -> hearingQueryApi.searchForHearingById(proposedQuery));

        assertThat(badRequestException.getMessage(), is("Please ensure that the id is a valid UUID."));
    }

    @Test
    public void searchForHearingByIdWhenIdIsValid() {

        final JsonObject proposedQuery = generateQuery(
                createObjectBuilder()
                        .add("id", "d279360b-c42d-481a-8f54-9f7985bf2df1")
                        .build());

        final JsonObject envelopeReturnedFromRequester = mock(JsonObject.class);
        when(hearingQueryView.getHearingById(proposedQuery)).thenReturn(envelopeReturnedFromRequester);

        final JsonObject returnedEnvelope = hearingQueryApi.searchForHearingById(proposedQuery);

        assertSame(returnedEnvelope, envelopeReturnedFromRequester);
    }

    @Test
    public void searchUnscheduledHearingsWithoutOucodeL2Code() {

        final JsonObject proposedQuery = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.unscheduled.search.hearings"),
                createObjectBuilder()
                        .add("courtCentreId", "a7e61b9a-f1c2-427a-845a-abebcf9068c4")
                        .add("caseUrn", "caseUrnValue")
                        .add("typeOfList", "typeOfListValue")
                        .build());

        ArgumentCaptor<JsonObject> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final JsonObject results = mock(JsonObject.class);
        when(hearingQueryView.searchUnscheduledHearings(envelopeArgumentCaptor.capture())).thenReturn(results);


        final JsonObject returnedEnvelope = hearingQueryApi.searchUnscheduledHearings(proposedQuery);

        assertSame(returnedEnvelope, results);
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                metadata()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.unscheduled.search.hearings"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.caseUrn", equalTo("caseUrnValue")),
                                withJsonPath("$.typeOfList", equalTo("typeOfListValue")),
                                withJsonPath("$.courtCentreIds", equalTo("a7e61b9a-f1c2-427a-845a-abebcf9068c4"))
                        )
                ))
        ));
    }


    @Test
    public void searchUnscheduledHearingsWithOucodeL2Code() {

        final JsonObject proposedQuery = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.unscheduled.search.hearings"),
                createObjectBuilder()
                        .add("oucodeL2Code", "OP565")
                        .add("caseUrn", "caseUrnValue")
                        .add("typeOfList", "typeOfListValue")
                        .build());

        when(referenceDataLoader.fetchOrganisationUnitsByOucodeL2Code(eq("OP565"))).thenReturn(
                Arrays.asList(
                        new OrganisationUnit(fromString("ab774cac-ffac-4bd8-8c94-4a422feb267c"), "OP565"),
                        new OrganisationUnit(fromString("c9f485a3-dda3-4909-8a34-d599ae3316a4"), "OP565"))
        );
        ArgumentCaptor<JsonObject> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final JsonObject results = mock(JsonObject.class);
        when(hearingQueryView.searchUnscheduledHearings(envelopeArgumentCaptor.capture())).thenReturn(results);


        final JsonObject returnedEnvelope = hearingQueryApi.searchUnscheduledHearings(proposedQuery);

        assertSame(returnedEnvelope, results);
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                metadata()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.unscheduled.search.hearings"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.caseUrn", equalTo("caseUrnValue")),
                                withJsonPath("$.typeOfList", equalTo("typeOfListValue")),
                                withJsonPath("$.courtCentreIds",
                                        equalTo("ab774cac-ffac-4bd8-8c94-4a422feb267c,c9f485a3-dda3-4909-8a34-d599ae3316a4"))
                        )
                ))
        ));
    }

    @Test
    public void shouldReturnPayloadWithTemplateNameWhenCallPayloadApi() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PUBLIC.toString())
                        .build());

        final JsonObject courtListContent = mock(JsonObject.class);
        when(hearingQueryView.getCourtListContent(query)).thenReturn(courtListContent);
        when(standardPublicCourtListAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.empty());
        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("id"), is("id1"));
        assertThat(returnedEnvelope.getString("templateName"), is("PublicCourtList"));
    }

    @Test
    public void shouldReturnPayloadWithTemplateNameWhenUshersMagistrateList() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, USHERS_MAGISTRATE.toString())
                        .build());

        when(standardPublicCourtListAssembler.assemble(any(), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("id"), is("id1"));
        assertThat(returnedEnvelope.getString("templateName"), is("UshersMagistrateList"));
    }

    @Test
    public void shouldRouteAlphabeticalToAlphabeticalServiceAndReturnCourtListTemplate() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, ALPHABETICAL.toString())
                        .build());

        final JsonObject courtListContent = mock(JsonObject.class);
        when(hearingQueryView.getCourtListContent(query)).thenReturn(courtListContent);
        when(alphabeticalCourtListService.buildAlphabeticalCourtListData(any(JsonObject.class), any(String.class)))
                .thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.empty());

        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("id"), is("id1"));
        assertThat(returnedEnvelope.getString("templateName"), is("CourtList"));
        verify(alphabeticalCourtListService).buildAlphabeticalCourtListData(any(JsonObject.class), any(String.class));
        verify(standardPublicCourtListAssembler, never()).assemble(any(), any(), any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    public void shouldRouteJudgeToJudgeAssemblerUsingJudgeRangeSearchAndReturnJudgeListTemplate() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add("startDate", "2026-06-03")
                        .add(LIST_ID, JUDGE.toString())
                        .build());

        final JsonObject judgeContent = mock(JsonObject.class);
        when(hearingQueryView.rangeSearchHearingsForJudge(query)).thenReturn(judgeContent);
        when(judgeListTemplateAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class), any(CourtListType.class), any(String.class)))
                .thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.empty());

        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("id"), is("id1"));
        assertThat(returnedEnvelope.getString("templateName"), is("JudgeList"));
        verify(hearingQueryView).rangeSearchHearingsForJudge(query);
        verify(hearingQueryView, never()).getCourtListContent(query);
        verify(judgeListTemplateAssembler).assemble(any(JsonObject.class), any(String.class), any(String.class), any(CourtListType.class), any(String.class));
    }

    @Test
    public void shouldReturnPayloadWithTemplateNameWhenOnlinePublicCourtList() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, ONLINE_PUBLIC.toString())
                        .build());

        when(standardPublicCourtListAssembler.assemble(any(), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.empty());
        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("id"), is("id1"));
        assertThat(returnedEnvelope.getString("templateName"), is("OnlinePublicCourtList"));
    }

    @Test
    public void shouldReturnPayloadWithBilingualTemplateNameWhenCallPayloadApi() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PUBLIC.toString())
                        .build());

        final JsonObject queryResponse = mock(JsonObject.class);
        when(hearingQueryView.getCourtListContent(query)).thenReturn(queryResponse);
        when(standardPublicCourtListAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.of(true));
        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("id"), is("id1"));
        assertThat(returnedEnvelope.getString("templateName"), is("PublicCourtListEnglishWelsh"));
    }

    @Test
    public void shouldGetHearingCourtListForPrison() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PRISON.toString())
                        .build());

        when(standardPublicCourtListAssembler.assemble(any(), any(), any(), any(), any(), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any())).thenReturn(Optional.of(true));
        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("id"), is("id1"));
        assertThat(returnedEnvelope.getString("templateName"), is("PrisonCourtList"));
    }

    @Test
    public void shouldGetEmptyResponseWhenCourtListTypeIsEmpty() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, "")
                        .build());
        
        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.size(), is(0));
    }


    @Test
    public void shouldGetCasesByPersonDefendantWithIsCivilAndIsGroupMemberFlagTRUE() {
        assertGetCasesByPersonDefendant(true, true);
    }

    @Test
    public void shouldGetCasesByPersonDefendantWithIsCivilAndIsGroupMemberFlagFALSE() {
        assertGetCasesByPersonDefendant(false, false);
    }

    @Test
    public void shouldGetCasesByPersonDefendantWithoutIsCivilAndIsGroupMemberFlag() {
        assertGetCasesByPersonDefendant(null, null);
    }

    @Test
    public void shouldGetCasesByPersonDefendantWithIsCivilAndIsGroupMemberFlagFALSEAndDobAbsent() {
        assertGetCasesByPersonDefendantWithoutDob(false, false);
    }


    @Test
    public void shouldGetCasesByPersonDefendant() {
        final JsonObject envelope = Envelopes.createEnvelope("listing.get.cases-by-person-defendant", createObjectBuilder()
                .add("firstName", randomAlphabetic(5))
                .add("lastName", randomAlphabetic(5))
                .add("dateOfBirth", now().toString())
                .add("hearingDate", now().toString())
                .build());

        final Envelopes.Metadata metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-person-defendant");

        final JsonObject responseEnvelope = envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(cppQueryClient.request(any(), any(), any())).thenReturn(Optional.of(responseEnvelope));

        hearingQueryApi.getCasesByPersonDefendantAndHearingDate(envelope, USER_ID);

        verify(hearingQueryView).getCasesByDefendantAndHearingDate(any(), any(), any());
    }

    @Test
    public void shouldGetCasesByOrganisationDefendant() {
        final JsonObject envelope = Envelopes.createEnvelope("listing.get.cases-by-organisation-defendant", createObjectBuilder()
                .add("organisationName", randomAlphabetic(5))
                .add("hearingDate", now().toString())
                .build());

        final Envelopes.Metadata metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-organisation-defendant");

        final JsonObject responseEnvelope = envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(cppQueryClient.request(any(), any(), any())).thenReturn(Optional.of(responseEnvelope));

        hearingQueryApi.getCasesByOrganisationDefendantAndHearingDate(envelope, USER_ID);

        verify(hearingQueryView).getCasesByDefendantAndHearingDate(any(), any(), any());
    }

    private void assertGetCasesByPersonDefendant(final Boolean isCivil, final Boolean isGroupMember) {
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("firstName", randomAlphabetic(5))
                .add("lastName", randomAlphabetic(5))
                .add("dateOfBirth", now().toString())
                .add("hearingDate", now().toString());

        if (nonNull(isCivil)) {
            jsonObjectBuilder.add("isCivil", isCivil);
        }
        if (nonNull(isGroupMember)) {
            jsonObjectBuilder.add("isGroupMember", isGroupMember);
        }
        final JsonObject envelope = Envelopes.createEnvelope("listing.get.cases-by-person-defendant", jsonObjectBuilder.build());

        final Envelopes.Metadata metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-person-defendant");

        final JsonObject responseEnvelope = envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(cppQueryClient.request(any(), any(), any())).thenReturn(Optional.of(responseEnvelope));
        hearingQueryApi.getCasesByPersonDefendantAndHearingDate(envelope, USER_ID);

        verify(cppQueryClient).request(any(), requesterCaptor.capture(), any());

        final JsonObject jsonObject = requesterCaptor.getValue();

        if (nonNull(isCivil)) {
            assertThat(jsonObject.getBoolean("isCivil"), is(isCivil));
        } else {
            assertThat(jsonObject.containsKey("isCivil"), is(false));
        }
        if (nonNull(isGroupMember)) {
            assertThat(jsonObject.getBoolean("isGroupMember"), is(isGroupMember));
        } else {
            assertThat(jsonObject.containsKey("isGroupMember"), is(false));
        }
    }

    private void assertGetCasesByPersonDefendantWithoutDob(final Boolean isCivil, final Boolean isGroupMember) {
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("firstName", randomAlphabetic(5))
                .add("lastName", randomAlphabetic(5))
                .add("hearingDate", now().toString());

        if (nonNull(isCivil)) {
            jsonObjectBuilder.add("isCivil", isCivil);
        }
        if (nonNull(isGroupMember)) {
            jsonObjectBuilder.add("isGroupMember", isGroupMember);
        }
        final JsonObject envelope = Envelopes.createEnvelope("listing.get.cases-by-person-defendant", jsonObjectBuilder.build());

        final Envelopes.Metadata metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-person-defendant");

        final JsonObject responseEnvelope = envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(cppQueryClient.request(any(), any(), any())).thenReturn(Optional.of(responseEnvelope));
        hearingQueryApi.getCasesByPersonDefendantAndHearingDate(envelope, USER_ID);

        verify(cppQueryClient).request(any(), requesterCaptor.capture(), any());

        final JsonObject jsonObject = requesterCaptor.getValue();

        if (nonNull(isCivil)) {
            assertThat(jsonObject.getBoolean("isCivil"), is(isCivil));
        } else {
            assertThat(jsonObject.containsKey("isCivil"), is(false));
        }
        if (nonNull(isGroupMember)) {
            assertThat(jsonObject.getBoolean("isGroupMember"), is(isGroupMember));
        } else {
            assertThat(jsonObject.containsKey("isGroupMember"), is(false));
        }
    }

    private JsonObject generateQuery(JsonValue jsonValue) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("_"), jsonValue);

    }

    @Test
    public void shouldGetCasesByOrganisationDefendantWithIsCivilAndIsGroupMemberFlagTRUE() {
        assertGetCasesByOrganisationDefendant(true, true);
    }

    @Test
    public void shouldGetCasesByOrganisationDefendantWithIsCivilAndIsGroupMemberFlagFALSE() {
        assertGetCasesByOrganisationDefendant(false, false);
    }

    @Test
    public void shouldGetCasesByOrganisationDefendantWithoutIsCivilAndIsGroupMemberFlag() {
        assertGetCasesByOrganisationDefendant(null, null);
    }

    private void assertGetCasesByOrganisationDefendant(final Boolean isCivil, final Boolean isGroupMember) {
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("organisationName", randomAlphabetic(5))
                .add("hearingDate", now().toString());

        if (nonNull(isCivil)) {
            jsonObjectBuilder.add("isCivil", isCivil);
        }
        if (nonNull(isGroupMember)) {
            jsonObjectBuilder.add("isGroupMember", isGroupMember);
        }

        final JsonObject envelope = Envelopes.createEnvelope("listing.get.cases-by-organisation-defendant", jsonObjectBuilder.build());
        final Envelopes.Metadata metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-organisation-defendant");

        final JsonObject responseEnvelope = envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(cppQueryClient.request(any(), any(), any())).thenReturn(Optional.of(responseEnvelope));
        hearingQueryApi.getCasesByOrganisationDefendantAndHearingDate(envelope, USER_ID);
        verify(hearingQueryView).getCasesByDefendantAndHearingDate(any(), any(), any());
        verify(cppQueryClient).request(any(), requesterCaptor.capture(), any());

        final JsonObject jsonObject = requesterCaptor.getValue();

        if (nonNull(isCivil)) {
            assertThat(jsonObject.getBoolean("isCivil"), is(isCivil));
        } else {
            assertThat(jsonObject.containsKey("isCivil"), is(false));
        }
        if (nonNull(isCivil)) {
            assertThat(jsonObject.getBoolean("isGroupMember"), is(isGroupMember));
        } else {
            assertThat(jsonObject.containsKey("isGroupMember"), is(false));
        }
    }


    @Test
    public void shouldEnrichHearingsWithApplicationTypeCode() {
        final UUID applicationId1 = fromString("7ab2ed6a-bf9a-4539-848b-48012032c97c");
        final UUID applicationId2 = fromString("170fec9f-b8a3-4e64-92ce-ed051cfd405e");
        final String applicationTypeCode = "TYPE_CODE";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings-enriched.json")
                );

        when(hearingQueryView.searchHearings(query, USER_ID)).thenReturn(viewResponse);

        final JsonObject appDetails = createObjectBuilder()
                .add("courtApplication", createObjectBuilder().add("type", createObjectBuilder().add("code", applicationTypeCode)).build())
                .build();

        when(progressionService.getApplicationDetails(USER_ID, applicationId1)).thenReturn(of(appDetails));
        when(progressionService.getApplicationDetails(USER_ID, applicationId2)).thenReturn(of(appDetails));

        final JsonObject result = hearingQueryApi.searchHearings(query, USER_ID);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings-enriched.json");
        assertThat(result, equalTo(expectedPayload));

    }

    @Test
    public void shouldNotEnrichHearingsWithApplicationTypeCodeWhenNoApplicationOnHearing() {

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings-enriched-no-application.json")
        );

        when(hearingQueryView.searchHearings(query, USER_ID)).thenReturn(viewResponse);

        final JsonObject result = hearingQueryApi.searchHearings(query, USER_ID);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings-enriched-no-application.json");
        assertThat(result, equalTo(expectedPayload));

    }

    @Test
    public void shouldNotEnrichHearingsWithApplicationTypeCodeWhenNoHearing() {

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings-enriched-no-hearing.json")
        );

        when(hearingQueryView.searchHearings(query, USER_ID)).thenReturn(viewResponse);

        final JsonObject result = hearingQueryApi.searchHearings(query, USER_ID);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings-enriched-no-hearing.json");
        assertThat(result, equalTo(expectedPayload));

    }


    @Test
    public void shouldEnrichCourtCalendarHearingsWithApplicationTypeCode() {
        final UUID applicationId1 = fromString("b731f138-5ee3-4601-8cba-2b9b5f58c855");
        final UUID applicationId2 = fromString("9ddaff06-36f0-4c17-add9-82e84571f0fe");
        final String applicationTypeCode = "TYPE_CODE";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.range.search.hearings.court.calendar"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.range.search.hearings.court.calendar"), returnAsJsonObject("listing.search.hearings.court.calendar.json")
        );

        when(hearingQueryView.rangeSearchHearingsForCourtCalendar(query, USER_ID)).thenReturn(viewResponse);

        final JsonObject appDetails = createObjectBuilder()
                .add("courtApplication", createObjectBuilder().add("type", createObjectBuilder().add("code", applicationTypeCode)).build())
                .build();

        when(progressionService.getApplicationDetails(USER_ID, applicationId1)).thenReturn(of(appDetails));
        when(progressionService.getApplicationDetails(USER_ID, applicationId2)).thenReturn(of(appDetails));

        final JsonObject result = hearingQueryApi.rangeSearchHearingsForCourtCalendar(query, USER_ID);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings.court.calendar.json");
        assertThat(result, equalTo(expectedPayload));
    }

    @Test
    public void shouldNotEnrichCourtCalendarHearingsWithApplicationTypeCodeWhenNoApplicationOnHearing() {

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings.court.calendar-no-application.json")
        );

        when(hearingQueryView.searchHearings(query, USER_ID)).thenReturn(viewResponse);

        final JsonObject result = hearingQueryApi.searchHearings(query, USER_ID);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings.court.calendar-no-application.json");
        assertThat(result, equalTo(expectedPayload));

    }

    @Test
    public void shouldNotEnrichCourtCalendarHearingsWithApplicationTypeCodeWhenNoHearing() {

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings.court.calendar-no-hearing.json")
        );

        when(hearingQueryView.searchHearings(query, USER_ID)).thenReturn(viewResponse);

        final JsonObject result = hearingQueryApi.searchHearings(query, USER_ID);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings.court.calendar-no-hearing.json");
        assertThat(result, equalTo(expectedPayload));

    }

    @Test
    public void shouldGetDailyListWithoutEndDate() {
        final String courtCentreId = randomUUID().toString();
        final String startDate = "2026-05-07";
        final String publishCourtListType = "FINAL";
        final String courtCentreName = "Test Crown Court";
        final String welshCourtCentreName = "Llys y Goron Test";
        final String address1 = "The Queen Elizabeth II Law Courts";
        final String address2 = "Derby Square, Liverpool L2 1XA";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("publishCourtListType", publishCourtListType)
                        .build());

        final JsonObject crestCourtSite = createObjectBuilder()
                .add("crestCourtSiteId", "415")
                .add("crestCourtSiteName", "Kingston Crown Court")
                .add("courtType", "CROWN")
                .build();
        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", crestCourtSite)
                                .add("sittings", createArrayBuilder().build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("oucodeL3WelshName", welshCourtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue();
        assertThat(forwardedPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(forwardedPayload.getString("startDate"), is(startDate));
        assertThat(forwardedPayload.getString("publishCourtListType"), is(publishCourtListType));
        assertThat(forwardedPayload.containsKey("endDate"), is(false));

        final JsonObject resultPayload = result;
        assertThat(resultPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(resultPayload.getString("sittingDate"), is(startDate));
        assertThat(resultPayload.getString("publishedAt"), is(now().toString()));
        assertThat(resultPayload.getString("documentType"), is(publishCourtListType));
        assertThat(resultPayload.getBoolean("isWelsh"), is(false));
        assertThat(resultPayload.getString("courtCentreName"), is(courtCentreName));
        assertThat(resultPayload.getString("welshCourtCentreName"), is(welshCourtCentreName));

        final JsonObject enrichedSite = resultPayload.getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");
        assertThat(enrichedSite.getString("courtCentreAddress1"), is(address1));
        assertThat(enrichedSite.getString("courtCentreAddress2"), is(address2));
    }

    @Test
    void shouldGetDailyListWithJudiciaryName() {
        final String courtCentreId = randomUUID().toString();
        final String startDate = "2026-05-07";
        final String publishCourtListType = "FINAL";
        final String judicialId = randomUUID().toString();
        final String judiciaryName = "His Honour Judge Williams";
        final String welshJudiciaryName = "Ei Anrhydedd Barnwr Williams";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("publishCourtListType", publishCourtListType)
                        .build());

        final JsonObject judiciaryEntry = createObjectBuilder()
                .add("judicialId", judicialId)
                .add("isBenchChairman", true)
                .build();

        final JsonObject sitting = createObjectBuilder()
                .add("sittingDate", startDate)
                .add("judiciary", createArrayBuilder().add(judiciaryEntry).build())
                .build();

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", createObjectBuilder()
                                        .add("crestCourtSiteId", "415")
                                        .add("crestCourtSiteName", "Kingston Crown Court")
                                        .add("courtType", "CROWN")
                                        .build())
                                .add("sittings", createArrayBuilder().add(sitting).build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentreEnvelopePayload = createObjectBuilder().add("isWelsh", false).build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentreEnvelopePayload);

        final JsonObject judiciaryReferenceData = createObjectBuilder()
                .add("judiciaries", createArrayBuilder()
                        .add(createObjectBuilder().add("id", judicialId).add("surname", "Williams").build())
                        .build())
                .build();
        final JsonObject judiciaryReferenceDataEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.judiciaries"),
                judiciaryReferenceData);

        when(hearingQueryView.retrieveCourtList(any(JsonObject.class))).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);
        when(referenceDataService.getJudiciariesByIdList(eq(List.of(fromString(judicialId))))).thenReturn(judiciaryReferenceDataEnvelope);
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn(judiciaryName);
        when(judiciaryNameMapper.getWelshName(any(JsonObject.class))).thenReturn(welshJudiciaryName);

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query);

        final JsonObject enrichedJudiciary = resultPayload.getJsonArray("courtLists").getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("judiciary").getJsonObject(0);

        assertThat(enrichedJudiciary.getString("judicialId"), is(judicialId));
        assertThat(enrichedJudiciary.getString("judiciaryName"), is(judiciaryName));
        assertThat(enrichedJudiciary.getString("welshJudiciaryName"), is(welshJudiciaryName));
    }

    @Test
    void shouldKeepHearingsAndDefendantsWithoutOffences() {
        final String courtCentreId = randomUUID().toString();
        final String startDate = "2026-05-07";
        final String publishCourtListType = "FINAL";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("publishCourtListType", publishCourtListType)
                        .build());

        final JsonObject offence = createObjectBuilder()
                .add("offenceCode", "TH68001")
                .build();

        final JsonObject defendantWithOffence = createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offences", createArrayBuilder().add(offence).build())
                .build();

        final JsonObject defendantWithoutOffence = createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offences", createArrayBuilder().build())
                .build();

        final JsonObject hearingWithoutOffence = createObjectBuilder()
                .add("startTime", "2026-05-07T10:00:00")
                .add("defendants", createArrayBuilder().add(defendantWithoutOffence).build())
                .build();

        final JsonObject hearingWithOffence = createObjectBuilder()
                .add("startTime", "2026-05-07T11:00:00")
                .add("defendants", createArrayBuilder().add(defendantWithOffence).build())
                .build();

        final JsonObject hearingWithMixedDefendants = createObjectBuilder()
                .add("startTime", "2026-05-07T12:00:00")
                .add("defendants", createArrayBuilder().add(defendantWithOffence).add(defendantWithoutOffence).build())
                .build();

        final JsonObject sitting = createObjectBuilder()
                .add("sittingDate", startDate)
                .add("judiciary", createArrayBuilder().build())
                .add("hearings", createArrayBuilder()
                        .add(hearingWithoutOffence)
                        .add(hearingWithOffence)
                        .add(hearingWithMixedDefendants)
                        .build())
                .build();

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", createObjectBuilder()
                                        .add("crestCourtSiteId", "415")
                                        .add("crestCourtSiteName", "Kingston Crown Court")
                                        .add("courtType", "CROWN")
                                        .build())
                                .add("sittings", createArrayBuilder().add(sitting).build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentreEnvelopePayload = createObjectBuilder().add("isWelsh", false).build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentreEnvelopePayload);

        when(hearingQueryView.retrieveCourtList(any(JsonObject.class))).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query);

        final JsonArray remainingHearings = resultPayload.getJsonArray("courtLists").getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("hearings");

        assertThat(remainingHearings.size(), is(3));
        assertThat(remainingHearings.getJsonObject(0).getString("startTime"), is("2026-05-07T10:00:00"));
        assertThat(remainingHearings.getJsonObject(1).getString("startTime"), is("2026-05-07T11:00:00"));
        assertThat(remainingHearings.getJsonObject(2).getString("startTime"), is("2026-05-07T12:00:00"));

        final JsonArray hearingWithoutOffenceDefendants = remainingHearings.getJsonObject(0).getJsonArray("defendants");
        assertThat(hearingWithoutOffenceDefendants.size(), is(1));
        assertThat(hearingWithoutOffenceDefendants.getJsonObject(0), is(defendantWithoutOffence));

        final JsonArray mixedHearingDefendants = remainingHearings.getJsonObject(2).getJsonArray("defendants");
        assertThat(mixedHearingDefendants.size(), is(2));
        assertThat(mixedHearingDefendants.getJsonObject(0), is(defendantWithOffence));
        assertThat(mixedHearingDefendants.getJsonObject(1), is(defendantWithoutOffence));
    }

    @Test
    void shouldGetDailyListWithProsecutorOrganisationName() {
        final String courtCentreId = randomUUID().toString();
        final String startDate = "2026-05-07";
        final String publishCourtListType = "FINAL";
        final String prosecutorId = randomUUID().toString();
        final String organisationName = "Cardiff and Vale LDU";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("publishCourtListType", publishCourtListType)
                        .build());

        final JsonObject hearing = createObjectBuilder()
                .add("startTime", "2026-05-07T10:00:00")
                .add("prosecutor", createObjectBuilder()
                        .add("prosecutorId", prosecutorId)
                        .add("prosecutorCode", "CPS-EM")
                        .build())
                .build();

        final JsonObject sitting = createObjectBuilder()
                .add("sittingDate", startDate)
                .add("judiciary", createArrayBuilder().build())
                .add("hearings", createArrayBuilder().add(hearing).build())
                .build();

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", createObjectBuilder()
                                        .add("crestCourtSiteId", "415")
                                        .add("crestCourtSiteName", "Kingston Crown Court")
                                        .add("courtType", "CROWN")
                                        .build())
                                .add("sittings", createArrayBuilder().add(sitting).build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentreEnvelopePayload = createObjectBuilder().add("isWelsh", false).build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentreEnvelopePayload);

        final JsonObject prosecutorReferenceData = createObjectBuilder()
                .add("id", prosecutorId)
                .add("fullName", organisationName)
                .build();
        final JsonObject prosecutorReferenceDataEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.prosecutor"),
                prosecutorReferenceData);

        when(hearingQueryView.retrieveCourtList(any(JsonObject.class))).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);
        when(referenceDataService.getProsecutorById(eq(prosecutorId))).thenReturn(Optional.ofNullable(prosecutorReferenceDataEnvelope));

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query);

        final JsonObject enrichedHearing = resultPayload.getJsonArray("courtLists").getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);

        final JsonObject enrichedProsecutor = enrichedHearing.getJsonObject("prosecutor");
        assertThat(enrichedProsecutor.getString("organisationName"), is(organisationName));
        assertThat(enrichedProsecutor.containsKey("prosecutorId"), is(false));
    }

    @Test
    void shouldGetDailyListWithProsecutorOrganisationNameFromCaseIdentifierAuthorityIdWhenProsecutorMissing() {
        final String courtCentreId = randomUUID().toString();
        final String startDate = "2026-05-07";
        final String publishCourtListType = "FINAL";
        final String authorityId = randomUUID().toString();
        final String organisationName = "Crown Prosecution Service";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("publishCourtListType", publishCourtListType)
                        .build());

        final JsonObject hearing = createObjectBuilder()
                .add("startTime", "2026-05-07T10:00:00")
                .add("caseIdentifier", createObjectBuilder()
                        .add("authorityId", authorityId)
                        .add("authorityCode", "CPS002")
                        .add("caseReference", "T20260002")
                        .build())
                .build();

        final JsonObject sitting = createObjectBuilder()
                .add("sittingDate", startDate)
                .add("judiciary", createArrayBuilder().build())
                .add("hearings", createArrayBuilder().add(hearing).build())
                .build();

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", createObjectBuilder()
                                        .add("crestCourtSiteId", "415")
                                        .add("crestCourtSiteName", "Kingston Crown Court")
                                        .add("courtType", "CROWN")
                                        .build())
                                .add("sittings", createArrayBuilder().add(sitting).build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentreEnvelopePayload = createObjectBuilder().add("isWelsh", false).build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentreEnvelopePayload);

        final JsonObject prosecutorReferenceData = createObjectBuilder()
                .add("id", authorityId)
                .add("fullName", organisationName)
                .build();
        final JsonObject prosecutorReferenceDataEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.prosecutor"),
                prosecutorReferenceData);

        when(hearingQueryView.retrieveCourtList(any(JsonObject.class))).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);
        when(referenceDataService.getProsecutorById(eq(authorityId))).thenReturn(Optional.ofNullable(prosecutorReferenceDataEnvelope));

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query);

        final JsonObject enrichedHearing = resultPayload.getJsonArray("courtLists").getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);

        final JsonObject enrichedProsecutor = enrichedHearing.getJsonObject("prosecutor");
        assertThat(enrichedProsecutor.getString("organisationName"), is(organisationName));
    }

    @Test
    void shouldGetDailyListWithEndDate() {
        final String courtCentreId = randomUUID().toString();
        final String startDate = "2026-05-07";
        final String endDate = "2026-05-09";
        final String publishCourtListType = "DRAFT";
        final String courtCentreName = "Test Crown Court";
        final String address1 = "The Queen Elizabeth II Law Courts";
        final String address2 = "Derby Square, Liverpool L2 1XA";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", startDate)
                        .add("publishCourtListType", publishCourtListType)
                        .add("endDate", endDate)
                        .build());

        final JsonObject crestCourtSite = createObjectBuilder()
                .add("crestCourtSiteId", "415")
                .add("crestCourtSiteName", "Kingston Crown Court")
                .add("courtType", "CROWN")
                .build();
        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", crestCourtSite)
                                .add("sittings", createArrayBuilder().build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue();
        assertThat(forwardedPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(forwardedPayload.getString("startDate"), is(startDate));
        assertThat(forwardedPayload.getString("publishCourtListType"), is(publishCourtListType));
        assertThat(forwardedPayload.getString("endDate"), is(endDate));

        final JsonObject resultPayload = result;
        assertThat(resultPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(resultPayload.getString("sittingDate"), is(startDate));
        assertThat(resultPayload.getString("publishedAt"), is(now().toString()));
        assertThat(resultPayload.getString("documentType"), is(publishCourtListType));
        assertThat(resultPayload.getBoolean("isWelsh"), is(false));
        assertThat(resultPayload.getString("courtCentreName"), is(courtCentreName));

        final JsonObject enrichedSite = resultPayload.getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");
        assertThat(enrichedSite.getString("courtCentreAddress1"), is(address1));
        assertThat(enrichedSite.getString("courtCentreAddress2"), is(address2));
    }

    @Test
    void shouldGetWeekCommencingListWithBothDates() {
        final String courtCentreId = randomUUID().toString();
        final String weekCommencingStartDate = "2026-05-04";
        final String weekCommencingEndDate = "2026-05-08";
        final String publishCourtListType = "WARN";
        final String courtCentreName = "Test Crown Court";
        final String welshCourtCentreName = "Llys y Goron Test";
        final String address1 = "The Queen Elizabeth II Law Courts";
        final String address2 = "Derby Square, Liverpool L2 1XA";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("weekCommencingStartDate", weekCommencingStartDate)
                        .add("weekCommencingEndDate", weekCommencingEndDate)
                        .add("publishCourtListType", publishCourtListType)
                        .build());

        final JsonObject crestCourtSite = createObjectBuilder()
                .add("crestCourtSiteId", "415")
                .add("crestCourtSiteName", "Kingston Crown Court")
                .add("courtType", "CROWN")
                .build();
        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", crestCourtSite)
                                .add("sittings", createArrayBuilder().build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("oucodeL3WelshName", welshCourtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue();
        assertThat(forwardedPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(forwardedPayload.getString("startDate"), is(weekCommencingStartDate));
        assertThat(forwardedPayload.getString("endDate"), is(weekCommencingEndDate));
        assertThat(forwardedPayload.getString("publishCourtListType"), is(publishCourtListType));

        final JsonObject resultPayload = result;
        assertThat(resultPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(resultPayload.getString("weekCommencingStartDate"), is(weekCommencingStartDate));
        assertThat(resultPayload.getString("weekCommencingEndDate"), is(weekCommencingEndDate));
        assertThat(resultPayload.getString("publishedAt"), is(now().toString()));
        assertThat(resultPayload.getString("documentType"), is(publishCourtListType));
        assertThat(resultPayload.getBoolean("isWelsh"), is(false));
        assertThat(resultPayload.getString("courtCentreName"), is(courtCentreName));
        assertThat(resultPayload.getString("welshCourtCentreName"), is(welshCourtCentreName));

        final JsonObject enrichedSite = resultPayload.getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");
        assertThat(enrichedSite.getString("courtCentreAddress1"), is(address1));
        assertThat(enrichedSite.getString("courtCentreAddress2"), is(address2));
    }

    @Test
    void shouldGetWeekCommencingListWithoutEndDate() {
        final String courtCentreId = randomUUID().toString();
        final String weekCommencingStartDate = "2026-05-04";
        final String publishCourtListType = "FIRM";
        final String courtCentreName = "Test Crown Court";
        final String address1 = "The Queen Elizabeth II Law Courts";
        final String address2 = "Derby Square, Liverpool L2 1XA";

        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("weekCommencingStartDate", weekCommencingStartDate)
                        .add("publishCourtListType", publishCourtListType)
                        .build());

        final JsonObject crestCourtSite = createObjectBuilder()
                .add("crestCourtSiteId", "415")
                .add("crestCourtSiteName", "Kingston Crown Court")
                .add("courtType", "CROWN")
                .build();
        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", crestCourtSite)
                                .add("sittings", createArrayBuilder().build())
                                .build())
                        .build())
                .build();

        final JsonObject courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue();
        assertThat(forwardedPayload.getString("startDate"), is(weekCommencingStartDate));
        assertThat(forwardedPayload.containsKey("endDate"), is(false));

        final JsonObject resultPayload = result;
        assertThat(resultPayload.getString("weekCommencingStartDate"), is(weekCommencingStartDate));
        assertThat(resultPayload.containsKey("weekCommencingEndDate"), is(false));
        assertThat(resultPayload.getString("documentType"), is(publishCourtListType));
        assertThat(resultPayload.getString("courtCentreName"), is(courtCentreName));

        final JsonObject enrichedSite = resultPayload.getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");
        assertThat(enrichedSite.getString("courtCentreAddress1"), is(address1));
        assertThat(enrichedSite.getString("courtCentreAddress2"), is(address2));
    }

    @Test
    void shouldGetDailyListWithWelshAddresses() {
        final String courtCentreId = randomUUID().toString();
        final String welshAddress1 = "Llys y Goron Caerdydd";
        final String welshAddress2 = "2 Stryd y Parc, Caerdydd CF10 1ET";

        final JsonObject query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", "2026-06-09")
                        .add("publishCourtListType", "FINAL")
                        .build());

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", createObjectBuilder()
                                        .add("crestCourtSiteId", "100")
                                        .add("crestCourtSiteName", "Cardiff Crown Court")
                                        .add("courtType", "CROWN")
                                        .build())
                                .build())
                        .build())
                .build();

        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                createObjectBuilder()
                        .add("oucodeL3Name", "Cardiff Crown Court")
                        .add("address1", "Cardiff Crown Court")
                        .add("welshAddress1", welshAddress1)
                        .add("welshAddress2", welshAddress2)
                        .add("isWelsh", true)
                        .build());

        when(hearingQueryView.retrieveCourtList(any(JsonObject.class))).thenReturn(
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.courtlist"), courtListPayload));
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(true));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject result = hearingQueryApi.getDailyList(query);
        final JsonObject enrichedSite = result
                .getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");

        assertThat(enrichedSite.getString("welshCourtCentreAddress1"), is(welshAddress1));
        assertThat(enrichedSite.getString("welshCourtCentreAddress2"), is(welshAddress2));
        assertThat(result.getBoolean("isWelsh"), is(true));
    }

    @Test
    void shouldGetDailyListWhenCourtCentreNameIsAbsent() {
        final String courtCentreId = randomUUID().toString();

        final JsonObject query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", "2026-06-09")
                        .add("publishCourtListType", "DRAFT")
                        .build());

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", createObjectBuilder()
                                        .add("crestCourtSiteId", "100")
                                        .add("crestCourtSiteName", "Test Crown Court")
                                        .add("courtType", "CROWN")
                                        .build())
                                .build())
                        .build())
                .build();

        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                createObjectBuilder().add("isWelsh", false).build());

        when(hearingQueryView.retrieveCourtList(any(JsonObject.class))).thenReturn(
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.courtlist"), courtListPayload));
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query);

        assertThat(resultPayload.containsKey("courtCentreName"), is(false));
        assertThat(resultPayload.containsKey("welshCourtCentreName"), is(false));
    }

    @Test
    void shouldGetDailyListWhenAllAddressFieldsAbsent() {
        final String courtCentreId = randomUUID().toString();

        final JsonObject query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.daily.list.payload"),
                createObjectBuilder()
                        .add("courtCentreId", courtCentreId)
                        .add("startDate", "2026-06-09")
                        .add("publishCourtListType", "DRAFT")
                        .build());

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("courtLists", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("crestCourtSite", createObjectBuilder()
                                        .add("crestCourtSiteId", "100")
                                        .add("crestCourtSiteName", "Test Crown Court")
                                        .add("courtType", "CROWN")
                                        .build())
                                .build())
                        .build())
                .build();

        final JsonObject courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                createObjectBuilder().add("oucodeL3Name", "Test Crown Court").add("isWelsh", false).build());

        when(hearingQueryView.retrieveCourtList(any(JsonObject.class))).thenReturn(
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.courtlist"), courtListPayload));
        when(referenceDataService.isHearingLanguageWelsh(eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class))).thenReturn(courtCentreEnvelope);

        final JsonObject enrichedSite = hearingQueryApi.getDailyList(query)
                .getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");

        assertThat(enrichedSite.containsKey("courtCentreAddress1"), is(false));
        assertThat(enrichedSite.containsKey("courtCentreAddress2"), is(false));
        assertThat(enrichedSite.containsKey("welshCourtCentreAddress1"), is(false));
        assertThat(enrichedSite.containsKey("welshCourtCentreAddress2"), is(false));
    }

    @Test
    void shouldReturnEmptyResponseWhenAssemblerReturnsEmpty() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PUBLIC.toString())
                        .build());

        when(hearingQueryView.getCourtListContent(query)).thenReturn(mock(JsonObject.class));
        when(standardPublicCourtListAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class),
                any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.empty());

        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.size(), is(0));
    }

    @Test
    void shouldReturnWelshTemplateForOnlinePublicCourtListWhenWelsh() {
        final JsonObject query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, ONLINE_PUBLIC.toString())
                        .build());

        when(standardPublicCourtListAssembler.assemble(any(), any(String.class), any(String.class),
                any(CourtListType.class), any(boolean.class), any(boolean.class)))
                .thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.of(true));

        final JsonObject returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.getString("templateName"), is("OnlinePublicCourtListEnglishWelsh"));
    }

    @Test
    void shouldNotEnrichApplicationWhenDetailsNotFound() {
        final String applicationId = "7ab2ed6a-bf9a-4539-848b-48012032c97c";

        final JsonObject query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add("hearings", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", randomUUID().toString())
                                        .add("courtApplications", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", applicationId).build())
                                                .build())
                                        .build())
                                .build())
                        .build());

        when(hearingQueryView.searchHearings(query, USER_ID)).thenReturn(viewResponse);
        when(progressionService.getApplicationDetails(any(UUID.class), eq(fromString(applicationId))))
                .thenReturn(Optional.empty());

        final JsonObject result = hearingQueryApi.searchHearings(query, USER_ID);

        final JsonObject resultApplication = result
                .getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("courtApplications").getJsonObject(0);
        assertThat(resultApplication.containsKey("applicationTypeCode"), is(false));
    }

    @Test
    void shouldNotEnrichApplicationWhenFetchThrowsException() {
        final String applicationId = "170fec9f-b8a3-4e64-92ce-ed051cfd405e";

        final JsonObject query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonObject viewResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add("hearings", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", randomUUID().toString())
                                        .add("courtApplications", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", applicationId).build())
                                                .build())
                                        .build())
                                .build())
                        .build());

        when(hearingQueryView.searchHearings(query, USER_ID)).thenReturn(viewResponse);
        when(progressionService.getApplicationDetails(any(UUID.class), eq(fromString(applicationId))))
                .thenThrow(new RuntimeException("Service unavailable"));

        final JsonObject result = hearingQueryApi.searchHearings(query, USER_ID);

        final JsonObject resultApplication = result
                .getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("courtApplications").getJsonObject(0);
        assertThat(resultApplication.containsKey("applicationTypeCode"), is(false));
    }

    private JsonObject returnAsJsonObject(final String expectedJsonPath) {
        final String payload = FileUtil.getPayload(expectedJsonPath);
        try (JsonReader jsonReader = createReader(new StringReader(payload))) {
            return jsonReader.readObject();
        }
    }

}
