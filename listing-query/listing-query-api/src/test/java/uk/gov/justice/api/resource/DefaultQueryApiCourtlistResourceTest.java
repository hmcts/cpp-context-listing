package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.COURT_LIST_QUERY_NAME;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.DISPOSITION;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.query.api.service.AlphabeticalCourtListService;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.DocumentGeneratorClient;
import uk.gov.moj.cpp.listing.query.document.generator.JudgeListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultQueryApiCourtlistResourceTest {
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    private HearingQueryView hearingQueryView;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Mock
    private AlphabeticalCourtListService alphabeticalCourtListService;


    @Mock
    private StandardPublicCourtListTemplateAssembler standardCourtListTemplateAssembler;

    @Mock
    private JudgeListTemplateAssembler judgeListTemplateAssembler;

    @Mock
    private Response documentContentResponse;

    @Mock
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextCaptor;

    @InjectMocks
    private DefaultQueryApiCourtlistResource endpointHandler;

    private final UUID userId = randomUUID();
    private final UUID materialId = randomUUID();
    private final UUID systemUserId = randomUUID();
    private final byte[] documentResponseBinary = "test".getBytes();
    private static final String COURT_CENTRE_ID = randomUUID().toString();
    private static final String COURT_ROOM_ID = randomUUID().toString();
    private static final String LIST_ID_ALPHABETICAL = "Alphabetical";
    private static final String LIST_ID_STANDARD = "Standard";
    private static final String LIST_ID_BENCH = "Bench";
    private static final String LIST_ID_JUDGE = "Judge";
    private static final String START_DATE = to(now());
    private static final String END_DATE = to(now());

    @Before
    public void init() {
        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));

        final JsonEnvelope documentDetails = documentDetails(materialId);
        when(interceptorChainProcessor.process(argThat((Matchers.any(InterceptorContext.class))))).thenReturn(Optional.ofNullable(documentDetails));
        when(hearingQueryView.getCourtListContent(argThat((Matchers.any(JsonEnvelope.class))))).thenReturn(documentDetails);
        when(hearingQueryView.rangeSearchHearingsForJudge(argThat((Matchers.any(JsonEnvelope.class))))).thenReturn(documentDetails);
        when(documentGeneratorClient.generateDocument(any(JsonObject.class), any(String.class))).thenReturn(documentResponseBinary);
        when(documentContentResponse.getStatus()).thenReturn(SC_OK);
    }

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocumentForAlphabetical() {
        final MultivaluedMap headers = givenHeaders();
        when(alphabeticalCourtListService.buildAlphabeticalCourtListData(
                any(JsonEnvelope.class), any(String.class)))
                .thenReturn(Optional.of(Json.createObjectBuilder().build()));

        when(referenceDataService.isHearingLanguageWelsh(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.ofNullable(false));

        final Response documentContentResponse = endpointHandler.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, LIST_ID_ALPHABETICAL, START_DATE, END_DATE, TRUE, UUID.randomUUID());

        verifyResponse(headers, documentContentResponse, LIST_ID_ALPHABETICAL);

    }

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocumentForStandard() {

        final MultivaluedMap headers = givenHeaders();
        when(standardCourtListTemplateAssembler.assemble(
                any(JsonEnvelope.class), any(String.class), any(String.class), any(CourtListType.class), any(Boolean.class)))
                .thenReturn(Optional.of(Json.createObjectBuilder().build()));
        when(referenceDataService.isHearingLanguageWelsh(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.ofNullable(false));

        final Response documentContentResponse = endpointHandler.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, LIST_ID_STANDARD, START_DATE, END_DATE, TRUE, UUID.randomUUID());

        verifyResponse(headers, documentContentResponse, LIST_ID_STANDARD);

    }

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocumentForBench() {

        final MultivaluedMap headers = givenHeaders();
        when(standardCourtListTemplateAssembler.assemble(
                any(JsonEnvelope.class), any(String.class), any(String.class), any(CourtListType.class), any(Boolean.class)))
                .thenReturn(Optional.of(Json.createObjectBuilder().build()));
        when(referenceDataService.isHearingLanguageWelsh(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.ofNullable(false));

        final Response documentContentResponse = endpointHandler.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, LIST_ID_BENCH, START_DATE, END_DATE, TRUE, UUID.randomUUID());

        verifyResponse(headers, documentContentResponse, LIST_ID_BENCH);

    }

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocumentForJudge() {

        final MultivaluedMap headers = givenHeaders();
        when(judgeListTemplateAssembler.assemble(
                any(JsonEnvelope.class), any(String.class), any(String.class), any(CourtListType.class), anyString()))
                .thenReturn(Optional.of(Json.createObjectBuilder().build()));
        when(referenceDataService.isHearingLanguageWelsh(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.ofNullable(false));

        final Response documentContentResponse = endpointHandler.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, LIST_ID_JUDGE, START_DATE, END_DATE, TRUE, UUID.randomUUID());

        verifyResponse(headers, documentContentResponse, LIST_ID_JUDGE);

    }

    private void verifyResponse(MultivaluedMap headers, Response documentContentResponse, String listIdStandard) {
        assertThat(documentContentResponse.getStatus(), is(SC_OK));
        assertThat(documentContentResponse.getHeaders(), is(headers));
        verifyInterceptorChainExecution(listIdStandard);
    }

    private MultivaluedMap givenHeaders() {
        final MultivaluedMap headers = new MultivaluedHashMap(ImmutableMap.of(
                CONTENT_TYPE, PDF_CONTENT_TYPE, CONTENT_DISPOSITION, DISPOSITION));
        when(documentContentResponse.getHeaders()).thenReturn(headers);
        return headers;
    }


    private JsonEnvelope documentDetails(final UUID materialId) {
        return documentDetails(createObjectBuilder().build());
    }

    private JsonEnvelope missingDocumentDetails() {
        return documentDetails(JsonValue.NULL);
    }

    private JsonEnvelope documentDetails(final JsonValue payload) {
        return envelopeFrom(metadataWithRandomUUID(COURT_LIST_QUERY_NAME), payload);
    }

    private void verifyInterceptorChainExecution(String expectedListId) {
        verify(interceptorChainProcessor).process(interceptorContextCaptor.capture());

        assertThat(interceptorContextCaptor.getValue().inputEnvelope(), jsonEnvelope(metadata().withName(COURT_LIST_QUERY_NAME),
                payload().isJson(allOf(
                        withJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID)),
                        withJsonPath("$.listId", equalTo(expectedListId)),
                        withJsonPath("$.courtRoomId", equalTo(COURT_ROOM_ID)),
                        withJsonPath("$.startDate", equalTo(START_DATE)),
                        withJsonPath("$.endDate", equalTo(END_DATE))

                ))
        ));
    }
}
