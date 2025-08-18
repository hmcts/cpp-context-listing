package uk.gov.moj.cpp.listing.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
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
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ONLINE_PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PRISON;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.USHERS_MAGISTRATE;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
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
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
        when(standardPublicCourtListAssembler.assemble(any(JsonEnvelope.class), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
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

        when(standardPublicCourtListAssembler.assemble(any(), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
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

        when(standardPublicCourtListAssembler.assemble(any(), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
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
        when(standardPublicCourtListAssembler.assemble(any(JsonEnvelope.class), any(String.class), any(String.class), any(CourtListType.class), any(boolean.class))).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
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

        when(standardPublicCourtListAssembler.assemble(any(), any(), any(), any(), any())).thenReturn(Optional.of(createObjectBuilder().add("id", "id1").build()));
        when(referenceDataService.isHearingLanguageWelsh(any(), any())).thenReturn(Optional.of(true));
        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchHearingsForCourtListPayload(query);

        assertThat(returnedEnvelope.payloadAsJsonObject().getString("id"), is("id1"));
        assertThat(returnedEnvelope.payloadAsJsonObject().getString("templateName"), is("PrisonCourtList"));
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
}