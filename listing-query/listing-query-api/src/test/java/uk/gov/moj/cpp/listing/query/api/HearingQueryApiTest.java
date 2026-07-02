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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ONLINE_PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PRISON;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.USHERS_MAGISTRATE;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataLoader;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
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

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

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
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private HearingQueryApi hearingQueryApi;

    @Mock
    Requester requester;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JudiciaryNameMapper judiciaryNameMapper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requesterCaptor;


    @BeforeEach
    public void setup() {
        apiMethodsToHandlerNames = stream(HearingQueryApi.class.getMethods())
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
    }

    @Test
    public void ensureThatActionAndHandlerNamesMatchTheRaml() throws Exception {
        final List<String> ramlActionNames = readLines(new File(PATH_TO_RAML)).stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(NAME))
                .filter(line -> !line.contains(HEARING_SLOTS))
                .filter(line -> line.contains(LISTING_SEARCH)
                        || line.contains(LISTING_AVAILABLE_HEARING_SEARCH)
                        || line.contains(LISTING_RANGE_SEARCH)
                        || line.contains(LISTING_RANGE_SEARCH_HEARINGS_COURT_CALENDAR)
                        || line.contains(LISTING_COURT_LIST_PUBLISH_STATUS)
                        || line.contains(LISTING_SEARCH_HEARING)
                        || line.contains(LISTING_UNSCHEDULED_SEARCH_HEARING)
                        || line.contains(LISTING_ALLOCATED_AND_UNALLOCATED_HEARING)
                        || line.contains(LISTING_ANY_ALLOCATION_SEARCH_HEARINGS)
                        || line.contains(LISTING_COTR_SEARCH_HEARING)
                        || line.contains(LISTING_SEARCH_BY_PERSON_DEFENDANT)
                        || line.contains(LISTING_SEARCH_BY_ORGANISATION_DEFENDANT)
                )
                .map(line -> line.replaceAll(NAME, "").trim())
                .collect(toList());

        assertThat(apiMethodsToHandlerNames.values(), containsInAnyOrder(ramlActionNames.toArray()));
    }

    @Test
    public void searchForHearingByIdWhenIdIsNotPresent() {


        final JsonEnvelope proposedQuery = generateQuery(createObjectBuilder().build());

        final IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> hearingQueryApi.searchForHearingById(proposedQuery));

        assertThat(illegalArgumentException.getMessage(), is("Attempted to search for a Hearing without an ID."));
    }

    @Test
    public void searchForHearingByIdWhenIdIsNotValid() {
        final JsonEnvelope proposedQuery = generateQuery(
                createObjectBuilder()
                        .add("id", "849afced-85b")
                        .build());

        final BadRequestException badRequestException = assertThrows(
                BadRequestException.class,
                () -> hearingQueryApi.searchForHearingById(proposedQuery));

        assertThat(badRequestException.getMessage(), is("Please ensure that the id is a valid UUID."));
    }

    @Test
    public void searchForHearingByIdWhenIdIsValid() {

        final JsonEnvelope proposedQuery = generateQuery(
                createObjectBuilder()
                        .add("id", "d279360b-c42d-481a-8f54-9f7985bf2df1")
                        .build());

        final JsonEnvelope envelopeReturnedFromRequester = mock(JsonEnvelope.class);
        when(hearingQueryView.getHearingById(proposedQuery)).thenReturn(envelopeReturnedFromRequester);

        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchForHearingById(proposedQuery);

        assertSame(returnedEnvelope, envelopeReturnedFromRequester);
    }

    @Test
    public void searchUnscheduledHearingsWithoutOucodeL2Code() {

        final JsonEnvelope proposedQuery = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.unscheduled.search.hearings"),
                createObjectBuilder()
                        .add("courtCentreId", "a7e61b9a-f1c2-427a-845a-abebcf9068c4")
                        .add("caseUrn", "caseUrnValue")
                        .add("typeOfList", "typeOfListValue")
                        .build());

        ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        final Envelope<JsonObject> results = mock(Envelope.class);
        when(hearingQueryView.searchUnscheduledHearings(envelopeArgumentCaptor.capture())).thenReturn(results);


        final Envelope<JsonObject> returnedEnvelope = hearingQueryApi.searchUnscheduledHearings(proposedQuery);

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

        final JsonEnvelope proposedQuery = envelopeFrom(
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
        ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        final Envelope<JsonObject> results = mock(Envelope.class);
        when(hearingQueryView.searchUnscheduledHearings(envelopeArgumentCaptor.capture())).thenReturn(results);


        final Envelope<JsonObject> returnedEnvelope = hearingQueryApi.searchUnscheduledHearings(proposedQuery);

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
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PUBLIC.toString())
                        .build());

        final JsonEnvelope courtListContent = mock(JsonEnvelope.class);
        when(hearingQueryView.getCourtListContent(query)).thenReturn(courtListContent);
        when(standardPublicCourtListAssembler.assemble(any(JsonEnvelope.class), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.empty());
        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().getString("id"), is("id1"));
        assertThat(returnedEnvelope.payloadAsJsonObject().getString("templateName"), is("PublicCourtList"));
    }

    @Test
    public void shouldReturnPayloadWithTemplateNameWhenUshersMagistrateList() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, USHERS_MAGISTRATE.toString())
                        .build());

        when(standardPublicCourtListAssembler.assemble(any(), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().getString("id"), is("id1"));
        assertThat(returnedEnvelope.payloadAsJsonObject().getString("templateName"), is("UshersMagistrateList"));
    }

    @Test
    public void shouldReturnPayloadWithTemplateNameWhenOnlinePublicCourtList() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, ONLINE_PUBLIC.toString())
                        .build());

        when(standardPublicCourtListAssembler.assemble(any(), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(), any(String.class))).thenReturn(Optional.empty());
        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().getString("id"), is("id1"));
        assertThat(returnedEnvelope.payloadAsJsonObject().getString("templateName"), is("OnlinePublicCourtList"));
    }

    @Test
    public void shouldReturnPayloadWithBilingualTemplateNameWhenCallPayloadApi() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PUBLIC.toString())
                        .build());

        final JsonEnvelope queryResponse = mock(JsonEnvelope.class);
        when(hearingQueryView.getCourtListContent(query)).thenReturn(queryResponse);
        when(standardPublicCourtListAssembler.assemble(any(JsonEnvelope.class), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.of(true));
        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().getString("id"), is("id1"));
        assertThat(returnedEnvelope.payloadAsJsonObject().getString("templateName"), is("PublicCourtListEnglishWelsh"));
    }

    @Test
    public void shouldGetHearingCourtListForPrison() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PRISON.toString())
                        .build());

        when(standardPublicCourtListAssembler.assemble(any(), any(), any(), any(), any(), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(), any())).thenReturn(Optional.of(true));
        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().getString("id"), is("id1"));
        assertThat(returnedEnvelope.payloadAsJsonObject().getString("templateName"), is("PrisonCourtList"));
    }

    @Test
    public void shouldGetEmptyResponseWhenCourtListTypeIsEmpty() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, "")
                        .build());
        
        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().size(), is(0));
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
        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("listing.get.cases-by-person-defendant", createObjectBuilder()
                .add("firstName", randomAlphabetic(5))
                .add("lastName", randomAlphabetic(5))
                .add("dateOfBirth", now().toString())
                .add("hearingDate", now().toString())
                .build());

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-person-defendant");

        final Envelope responseEnvelope = Envelope.envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(requester.request(any(), any())).thenReturn(responseEnvelope);

        hearingQueryApi.getCasesByPersonDefendantAndHearingDate(envelope);

        verify(hearingQueryView).getCasesByDefendantAndHearingDate(any(), any(), any(), any());
    }

    @Test
    public void shouldGetCasesByOrganisationDefendant() {
        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("listing.get.cases-by-organisation-defendant", createObjectBuilder()
                .add("organisationName", randomAlphabetic(5))
                .add("hearingDate", now().toString())
                .build());

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-organisation-defendant");

        final Envelope responseEnvelope = Envelope.envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(requester.request(any(), any())).thenReturn(responseEnvelope);

        hearingQueryApi.getCasesByOrganisationDefendantAndHearingDate(envelope);

        verify(hearingQueryView).getCasesByDefendantAndHearingDate(any(), any(), any(), any());
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
        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("listing.get.cases-by-person-defendant", jsonObjectBuilder.build());

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-person-defendant");

        final Envelope responseEnvelope = Envelope.envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(requester.request(any(), any())).thenReturn(responseEnvelope);
        hearingQueryApi.getCasesByPersonDefendantAndHearingDate(envelope);

        verify(requester).request(requesterCaptor.capture(), any());

        final JsonObject jsonObject = requesterCaptor.getValue().payloadAsJsonObject();

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
        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("listing.get.cases-by-person-defendant", jsonObjectBuilder.build());

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-person-defendant");

        final Envelope responseEnvelope = Envelope.envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(requester.request(any(), any())).thenReturn(responseEnvelope);
        hearingQueryApi.getCasesByPersonDefendantAndHearingDate(envelope);

        verify(requester).request(requesterCaptor.capture(), any());

        final JsonObject jsonObject = requesterCaptor.getValue().payloadAsJsonObject();

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

    private JsonEnvelope generateQuery(JsonValue jsonValue) {
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

        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("listing.get.cases-by-organisation-defendant", jsonObjectBuilder.build());
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("defence.query.get-case-by-organisation-defendant");

        final Envelope responseEnvelope = Envelope.envelopeFrom(metadataBuilder.build(), createObjectBuilder().add("caseIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("defendants", createArrayBuilder().add(randomUUID().toString()).build()).build());
        when(requester.request(any(), any())).thenReturn(responseEnvelope);
        hearingQueryApi.getCasesByOrganisationDefendantAndHearingDate(envelope);
        verify(hearingQueryView).getCasesByDefendantAndHearingDate(any(), any(), any(), any());
        verify(requester).request(requesterCaptor.capture(), any());

        final JsonObject jsonObject = requesterCaptor.getValue().payloadAsJsonObject();

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

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings-enriched.json")
                );

        when(hearingQueryView.searchHearings(query)).thenReturn(viewResponse);

        final JsonObject appDetails = createObjectBuilder()
                .add("courtApplication", createObjectBuilder().build())
                .build();

        when(progressionService.getApplicationDetails(viewResponse, applicationId1)).thenReturn(of(appDetails));
        when(progressionService.getApplicationDetails(viewResponse, applicationId2)).thenReturn(of(appDetails));

        final CourtApplication courtApplication = mock(CourtApplication.class);
        final CourtApplicationType type = mock(CourtApplicationType.class);
        when(type.getCode()).thenReturn(applicationTypeCode);
        when(courtApplication.getType()).thenReturn(type);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(CourtApplication.class))).thenReturn(courtApplication);

        final JsonEnvelope result = hearingQueryApi.searchHearings(query);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings-enriched.json");
        assertThat(result.payloadAsJsonObject(), equalTo(expectedPayload));

    }

    @Test
    public void shouldNotEnrichHearingsWithApplicationTypeCodeWhenNoApplicationOnHearing() {

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings-enriched-no-application.json")
        );

        when(hearingQueryView.searchHearings(query)).thenReturn(viewResponse);

        final JsonEnvelope result = hearingQueryApi.searchHearings(query);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings-enriched-no-application.json");
        assertThat(result.payloadAsJsonObject(), equalTo(expectedPayload));

    }

    @Test
    public void shouldNotEnrichHearingsWithApplicationTypeCodeWhenNoHearing() {

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings-enriched-no-hearing.json")
        );

        when(hearingQueryView.searchHearings(query)).thenReturn(viewResponse);

        final JsonEnvelope result = hearingQueryApi.searchHearings(query);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings-enriched-no-hearing.json");
        assertThat(result.payloadAsJsonObject(), equalTo(expectedPayload));

    }


    @Test
    public void shouldEnrichCourtCalendarHearingsWithApplicationTypeCode() {
        final UUID applicationId1 = fromString("b731f138-5ee3-4601-8cba-2b9b5f58c855");
        final UUID applicationId2 = fromString("9ddaff06-36f0-4c17-add9-82e84571f0fe");
        final String applicationTypeCode = "TYPE_CODE";

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.range.search.hearings.court.calendar"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.range.search.hearings.court.calendar"), returnAsJsonObject("listing.search.hearings.court.calendar.json")
        );

        when(hearingQueryView.rangeSearchHearingsForCourtCalendar(query)).thenReturn(viewResponse);

        final JsonObject appDetails = createObjectBuilder()
                .add("courtApplication", createObjectBuilder().build())
                .build();

        when(progressionService.getApplicationDetails(viewResponse, applicationId1)).thenReturn(of(appDetails));
        when(progressionService.getApplicationDetails(viewResponse, applicationId2)).thenReturn(of(appDetails));

        final CourtApplication courtApplication = mock(CourtApplication.class);
        final CourtApplicationType type = mock(CourtApplicationType.class);
        when(type.getCode()).thenReturn(applicationTypeCode);
        when(courtApplication.getType()).thenReturn(type);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(CourtApplication.class))).thenReturn(courtApplication);

        final JsonEnvelope result = hearingQueryApi.rangeSearchHearingsForCourtCalendar(query);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings.court.calendar.json");
        assertThat(result.payloadAsJsonObject(), equalTo(expectedPayload));
    }

    @Test
    public void shouldNotEnrichCourtCalendarHearingsWithApplicationTypeCodeWhenNoApplicationOnHearing() {

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings.court.calendar-no-application.json")
        );

        when(hearingQueryView.searchHearings(query)).thenReturn(viewResponse);

        final JsonEnvelope result = hearingQueryApi.searchHearings(query);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings.court.calendar-no-application.json");
        assertThat(result.payloadAsJsonObject(), equalTo(expectedPayload));

    }

    @Test
    public void shouldNotEnrichCourtCalendarHearingsWithApplicationTypeCodeWhenNoHearing() {

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("listing.search.hearings"), returnAsJsonObject("listing.search.hearings.court.calendar-no-hearing.json")
        );

        when(hearingQueryView.searchHearings(query)).thenReturn(viewResponse);

        final JsonEnvelope result = hearingQueryApi.searchHearings(query);

        final JsonObject expectedPayload = returnAsJsonObject("expected/listing.search.hearings.court.calendar-no-hearing.json");
        assertThat(result.payloadAsJsonObject(), equalTo(expectedPayload));

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

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("oucodeL3WelshName", welshCourtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);

        final JsonEnvelope result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue().payloadAsJsonObject();
        assertThat(captor.getValue().metadata().name(), is("listing.courtlist"));
        assertThat(forwardedPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(forwardedPayload.getString("startDate"), is(startDate));
        assertThat(forwardedPayload.getString("publishCourtListType"), is(publishCourtListType));
        assertThat(forwardedPayload.containsKey("endDate"), is(false));

        final JsonObject resultPayload = result.payloadAsJsonObject();
        assertThat(result.metadata().name(), is("listing.search.daily.list.payload"));
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

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentreEnvelopePayload = createObjectBuilder().add("isWelsh", false).build();
        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentreEnvelopePayload);

        final JsonObject judiciaryReferenceData = createObjectBuilder()
                .add("judiciaries", createArrayBuilder()
                        .add(createObjectBuilder().add("id", judicialId).add("surname", "Williams").build())
                        .build())
                .build();
        final JsonEnvelope judiciaryReferenceDataEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.judiciaries"),
                judiciaryReferenceData);

        when(hearingQueryView.retrieveCourtList(any(JsonEnvelope.class))).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);
        when(referenceDataService.getJudiciariesByIdList(eq(List.of(fromString(judicialId))), any(JsonEnvelope.class))).thenReturn(judiciaryReferenceDataEnvelope);
        when(judiciaryNameMapper.getName(any(JsonObject.class))).thenReturn(judiciaryName);

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query).payloadAsJsonObject();

        final JsonObject enrichedJudiciary = resultPayload.getJsonArray("courtLists").getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("judiciary").getJsonObject(0);

        assertThat(enrichedJudiciary.getString("judicialId"), is(judicialId));
        assertThat(enrichedJudiciary.getString("judiciaryName"), is(judiciaryName));
    }

    @Test
    void shouldGetDailyListWithProsecutorOrganisationName() {
        final String courtCentreId = randomUUID().toString();
        final String startDate = "2026-05-07";
        final String publishCourtListType = "FINAL";
        final String prosecutorId = randomUUID().toString();
        final String organisationName = "Cardiff and Vale LDU";

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentreEnvelopePayload = createObjectBuilder().add("isWelsh", false).build();
        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentreEnvelopePayload);

        final JsonObject prosecutorReferenceData = createObjectBuilder()
                .add("id", prosecutorId)
                .add("fullName", organisationName)
                .build();
        final JsonEnvelope prosecutorReferenceDataEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.prosecutor"),
                prosecutorReferenceData);

        when(hearingQueryView.retrieveCourtList(any(JsonEnvelope.class))).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);
        when(referenceDataService.getProsecutorById(eq(prosecutorId), any(JsonEnvelope.class))).thenReturn(prosecutorReferenceDataEnvelope);

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query).payloadAsJsonObject();

        final JsonObject enrichedHearing = resultPayload.getJsonArray("courtLists").getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);

        final JsonObject enrichedProsecutor = enrichedHearing.getJsonObject("prosecutor");
        assertThat(enrichedProsecutor.getString("organisationName"), is(organisationName));
        assertThat(enrichedProsecutor.containsKey("prosecutorId"), is(false));
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

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);

        final JsonEnvelope result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue().payloadAsJsonObject();
        assertThat(captor.getValue().metadata().name(), is("listing.courtlist"));
        assertThat(forwardedPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(forwardedPayload.getString("startDate"), is(startDate));
        assertThat(forwardedPayload.getString("publishCourtListType"), is(publishCourtListType));
        assertThat(forwardedPayload.getString("endDate"), is(endDate));

        final JsonObject resultPayload = result.payloadAsJsonObject();
        assertThat(result.metadata().name(), is("listing.search.daily.list.payload"));
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

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("oucodeL3WelshName", welshCourtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);

        final JsonEnvelope result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue().payloadAsJsonObject();
        assertThat(captor.getValue().metadata().name(), is("listing.courtlist"));
        assertThat(forwardedPayload.getString("courtCentreId"), is(courtCentreId));
        assertThat(forwardedPayload.getString("startDate"), is(weekCommencingStartDate));
        assertThat(forwardedPayload.getString("endDate"), is(weekCommencingEndDate));
        assertThat(forwardedPayload.getString("publishCourtListType"), is(publishCourtListType));

        final JsonObject resultPayload = result.payloadAsJsonObject();
        assertThat(result.metadata().name(), is("listing.search.daily.list.payload"));
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

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtListResponse = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.courtlist"),
                courtListPayload);

        final JsonObject courtCentrePayload = createObjectBuilder()
                .add("oucodeL3Name", courtCentreName)
                .add("address1", address1)
                .add("address2", address2)
                .add("isWelsh", false)
                .build();
        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                courtCentrePayload);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(hearingQueryView.retrieveCourtList(captor.capture())).thenReturn(courtListResponse);
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);

        final JsonEnvelope result = hearingQueryApi.getDailyList(query);

        final JsonObject forwardedPayload = captor.getValue().payloadAsJsonObject();
        assertThat(forwardedPayload.getString("startDate"), is(weekCommencingStartDate));
        assertThat(forwardedPayload.containsKey("endDate"), is(false));

        final JsonObject resultPayload = result.payloadAsJsonObject();
        assertThat(result.metadata().name(), is("listing.search.daily.list.payload"));
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

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                createObjectBuilder()
                        .add("oucodeL3Name", "Cardiff Crown Court")
                        .add("address1", "Cardiff Crown Court")
                        .add("welshAddress1", welshAddress1)
                        .add("welshAddress2", welshAddress2)
                        .add("isWelsh", true)
                        .build());

        when(hearingQueryView.retrieveCourtList(any(JsonEnvelope.class))).thenReturn(
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.courtlist"), courtListPayload));
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(true));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);

        final JsonEnvelope result = hearingQueryApi.getDailyList(query);
        final JsonObject enrichedSite = result.payloadAsJsonObject()
                .getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");

        assertThat(enrichedSite.getString("welshCourtCentreAddress1"), is(welshAddress1));
        assertThat(enrichedSite.getString("welshCourtCentreAddress2"), is(welshAddress2));
        assertThat(result.payloadAsJsonObject().getBoolean("isWelsh"), is(true));
    }

    @Test
    void shouldGetDailyListWhenCourtCentreNameIsAbsent() {
        final String courtCentreId = randomUUID().toString();

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                createObjectBuilder().add("isWelsh", false).build());

        when(hearingQueryView.retrieveCourtList(any(JsonEnvelope.class))).thenReturn(
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.courtlist"), courtListPayload));
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);

        final JsonObject resultPayload = hearingQueryApi.getDailyList(query).payloadAsJsonObject();

        assertThat(resultPayload.containsKey("courtCentreName"), is(false));
        assertThat(resultPayload.containsKey("welshCourtCentreName"), is(false));
    }

    @Test
    void shouldGetDailyListWhenAllAddressFieldsAbsent() {
        final String courtCentreId = randomUUID().toString();

        final JsonEnvelope query = envelopeFrom(
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

        final JsonEnvelope courtCentreEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.courtroom"),
                createObjectBuilder().add("oucodeL3Name", "Test Crown Court").add("isWelsh", false).build());

        when(hearingQueryView.retrieveCourtList(any(JsonEnvelope.class))).thenReturn(
                envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.courtlist"), courtListPayload));
        when(referenceDataService.isHearingLanguageWelsh(any(), eq(courtCentreId))).thenReturn(Optional.of(false));
        when(referenceDataService.getCourtCentreById(any(UUID.class), any(JsonEnvelope.class))).thenReturn(courtCentreEnvelope);

        final JsonObject enrichedSite = hearingQueryApi.getDailyList(query).payloadAsJsonObject()
                .getJsonArray("courtLists").getJsonObject(0).getJsonObject("crestCourtSite");

        assertThat(enrichedSite.containsKey("courtCentreAddress1"), is(false));
        assertThat(enrichedSite.containsKey("courtCentreAddress2"), is(false));
        assertThat(enrichedSite.containsKey("welshCourtCentreAddress1"), is(false));
        assertThat(enrichedSite.containsKey("welshCourtCentreAddress2"), is(false));
    }

    @Test
    void shouldReturnEmptyResponseWhenAssemblerReturnsEmpty() {
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder()
                        .withId(fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("listing.search.court.list.payload"),
                createObjectBuilder()
                        .add(COURT_CENTRE_ID, randomUUID().toString())
                        .add(COURT_ROOM_ID, randomUUID().toString())
                        .add(LIST_ID, PUBLIC.toString())
                        .build());

        when(hearingQueryView.getCourtListContent(query)).thenReturn(mock(JsonEnvelope.class));
        when(standardPublicCourtListAssembler.assemble(any(JsonEnvelope.class), any(String.class), any(String.class),
                any(CourtListType.class), any(boolean.class), any(boolean.class))).thenReturn(Optional.empty());

        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().size(), is(0));
    }

    @Test
    void shouldReturnWelshTemplateForOnlinePublicCourtListWhenWelsh() {
        final JsonEnvelope query = envelopeFrom(
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
        when(referenceDataService.isHearingLanguageWelsh(any(), any(String.class))).thenReturn(Optional.of(true));

        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().getString("templateName"), is("OnlinePublicCourtListEnglishWelsh"));
    }

    @Test
    void shouldNotEnrichApplicationWhenDetailsNotFound() {
        final String applicationId = "7ab2ed6a-bf9a-4539-848b-48012032c97c";

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
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

        when(hearingQueryView.searchHearings(query)).thenReturn(viewResponse);
        when(progressionService.getApplicationDetails(any(JsonEnvelope.class), eq(fromString(applicationId))))
                .thenReturn(Optional.empty());

        final JsonEnvelope result = hearingQueryApi.searchHearings(query);

        final JsonObject resultApplication = result.payloadAsJsonObject()
                .getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("courtApplications").getJsonObject(0);
        assertThat(resultApplication.containsKey("applicationTypeCode"), is(false));
    }

    @Test
    void shouldNotEnrichApplicationWhenFetchThrowsException() {
        final String applicationId = "170fec9f-b8a3-4e64-92ce-ed051cfd405e";

        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("listing.search.hearings"),
                createObjectBuilder().build());

        final JsonEnvelope viewResponse = envelopeFrom(
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

        when(hearingQueryView.searchHearings(query)).thenReturn(viewResponse);
        when(progressionService.getApplicationDetails(any(JsonEnvelope.class), eq(fromString(applicationId))))
                .thenThrow(new RuntimeException("Service unavailable"));

        final JsonEnvelope result = hearingQueryApi.searchHearings(query);

        final JsonObject resultApplication = result.payloadAsJsonObject()
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