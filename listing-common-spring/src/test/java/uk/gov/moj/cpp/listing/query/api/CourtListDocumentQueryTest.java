package uk.gov.moj.cpp.listing.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.domain.json.LocalDates.to;
import static uk.gov.moj.cpp.listing.testutil.EnvelopeMatchers.payloadIsJson;

import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.query.api.service.AlphabeticalCourtListService;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.DocumentGeneratorClient;
import uk.gov.moj.cpp.listing.query.document.generator.JudgeListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.moj.cpp.listing.query.view.exception.QueryBadRequestException;

import java.util.Optional;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ported from listing-query-api's DefaultQueryApiCourtlistResourceTest. The adapter ran the
 * interceptor chain on the {@code listing.search.court.list} query; that is the filters' job now,
 * so the payload checks are made on the query passed to the view. The PDF headers are set by the
 * listing-api controller.
 */
@ExtendWith(MockitoExtension.class)
class CourtListDocumentQueryTest {

    private static final String COURT_CENTRE_ID = randomUUID().toString();
    private static final String COURT_ROOM_ID = randomUUID().toString();
    private static final String START_DATE = to(now());
    private static final String END_DATE = to(now());
    private static final byte[] PDF = "test".getBytes();

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;
    @Mock
    private HearingQueryView hearingQueryView;
    @Mock
    private AlphabeticalCourtListService alphabeticalCourtListService;
    @Mock
    private StandardPublicCourtListTemplateAssembler standardCourtListTemplateAssembler;
    @Mock
    private JudgeListTemplateAssembler judgeListTemplateAssembler;
    @Mock
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonObject> queryCaptor;

    @InjectMocks
    private CourtListDocumentQuery courtListDocumentQuery;

    private final JsonObject documentDetails = createObjectBuilder().build();

    @Test
    void shouldFetchAndRenderDocumentForAlphabetical() {
        when(hearingQueryView.getCourtListContent(any(JsonObject.class))).thenReturn(documentDetails);
        when(documentGeneratorClient.generateDocument(any(JsonObject.class), any(String.class))).thenReturn(PDF);
        when(alphabeticalCourtListService.buildAlphabeticalCourtListData(any(JsonObject.class), any(String.class)))
                .thenReturn(Optional.of(createObjectBuilder().build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.of(false));

        assertThat(courtListDocumentQuery.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, "Alphabetical", START_DATE, END_DATE, true), is(PDF));

        verify(hearingQueryView).getCourtListContent(queryCaptor.capture());
        verifyQuery("Alphabetical");
    }

    @Test
    void shouldFetchAndRenderDocumentForStandard() {
        when(hearingQueryView.getCourtListContent(any(JsonObject.class))).thenReturn(documentDetails);
        when(documentGeneratorClient.generateDocument(any(JsonObject.class), any(String.class))).thenReturn(PDF);
        when(standardCourtListTemplateAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class),
                any(CourtListType.class), any(Boolean.class), any(boolean.class)))
                .thenReturn(Optional.of(createObjectBuilder().build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.of(false));

        assertThat(courtListDocumentQuery.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, "Standard", START_DATE, END_DATE, true), is(PDF));

        verify(hearingQueryView).getCourtListContent(queryCaptor.capture());
        verifyQuery("Standard");
        verify(standardCourtListTemplateAssembler).assemble(any(JsonObject.class), eq(COURT_CENTRE_ID), eq(COURT_ROOM_ID),
                eq(CourtListType.STANDARD), eq(true), eq(false));
    }

    @Test
    void shouldFetchAndRenderDocumentForBench() {
        when(hearingQueryView.getCourtListContent(any(JsonObject.class))).thenReturn(documentDetails);
        when(documentGeneratorClient.generateDocument(any(JsonObject.class), any(String.class))).thenReturn(PDF);
        when(standardCourtListTemplateAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class),
                any(CourtListType.class), any(Boolean.class), any(boolean.class)))
                .thenReturn(Optional.of(createObjectBuilder().build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.of(false));

        assertThat(courtListDocumentQuery.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, "Bench", START_DATE, END_DATE, true), is(PDF));

        verify(hearingQueryView).getCourtListContent(queryCaptor.capture());
        verifyQuery("Bench");
    }

    @Test
    void shouldFetchAndRenderDocumentForJudge() {
        when(hearingQueryView.rangeSearchHearingsForJudge(any(JsonObject.class))).thenReturn(documentDetails);
        when(documentGeneratorClient.generateDocument(any(JsonObject.class), any(String.class))).thenReturn(PDF);
        when(judgeListTemplateAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class),
                any(CourtListType.class), anyString()))
                .thenReturn(Optional.of(createObjectBuilder().build()));
        when(referenceDataService.isHearingLanguageWelsh(any(String.class))).thenReturn(Optional.of(false));

        assertThat(courtListDocumentQuery.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, "Judge", START_DATE, END_DATE, true), is(PDF));

        verify(hearingQueryView).rangeSearchHearingsForJudge(queryCaptor.capture());
        verifyQuery("Judge");
    }

    @Test
    void shouldRejectUnknownListType() {
        final QueryBadRequestException e = assertThrows(QueryBadRequestException.class,
                () -> courtListDocumentQuery.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, "Nope", START_DATE, END_DATE, false));

        assertThat(e.getMessage(), is("Bad request - No matching list type found for Nope"));
        verifyNoInteractions(hearingQueryView, documentGeneratorClient);
    }

    @Test
    void shouldRejectWhenThereIsNoCourtListData() {
        when(hearingQueryView.getCourtListContent(any(JsonObject.class))).thenReturn(documentDetails);
        when(standardCourtListTemplateAssembler.assemble(any(JsonObject.class), any(String.class), any(String.class),
                any(CourtListType.class), any(Boolean.class), any(boolean.class)))
                .thenReturn(Optional.empty());

        final QueryBadRequestException e = assertThrows(QueryBadRequestException.class,
                () -> courtListDocumentQuery.getCourtList(COURT_CENTRE_ID, COURT_ROOM_ID, "Standard", START_DATE, END_DATE, false));

        assertThat(e.getMessage(), is("Bad request - No data found for the supplied parameters"));
        verifyNoInteractions(documentGeneratorClient);
    }

    private void verifyQuery(final String expectedListId) {
        assertThat(queryCaptor.getValue(), payloadIsJson(allOf(
                withJsonPath("$.courtCentreId", equalTo(COURT_CENTRE_ID)),
                withJsonPath("$.listId", equalTo(expectedListId)),
                withJsonPath("$.courtRoomId", equalTo(COURT_ROOM_ID)),
                withJsonPath("$.startDate", equalTo(START_DATE)),
                withJsonPath("$.endDate", equalTo(END_DATE)))));
    }
}
