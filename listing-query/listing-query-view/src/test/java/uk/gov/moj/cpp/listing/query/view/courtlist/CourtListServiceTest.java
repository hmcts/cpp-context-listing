package uk.gov.moj.cpp.listing.query.view.courtlist;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.RangeSearchQuery;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtListServiceTest {

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private RangeSearchQueryRequestFactory rangeSearchQueryRequestFactory;

    @Mock
    private RangeSearchConverter rangeSearchConverter;

    @Mock
    private RangeSearchQuery rangeSearchQuery;

    @Mock
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @Mock
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    @Mock
    private ExParteLinkedCaseApplicationFilter exParteLinkedCaseApplicationFilter;

    @InjectMocks
    private CourtListService courtListService;

    @Test
    void retrieveCourtList() {

        final UUID courtCentreId = UUID.randomUUID();
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        final String endDate = LocalDate.now().toString();

        final JsonEnvelope queryEnvelope = generateQuery(createObjectBuilder().build());

        final JsonEnvelope rangeSearchQueryEnvelope = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchResponse = mock(JsonEnvelope.class);
        final JsonObject rangeSearchResponsePayload = mock(JsonObject.class);
        final JsonObject courtListResponse = mock(JsonObject.class);

        when(rangeSearchQueryRequestFactory.buildRangeSearchQueryEnvelope(courtCentreId, publishCourtListType, startDate, queryEnvelope)).thenReturn(rangeSearchQueryEnvelope);
        when(rangeSearchQuery.rangeSearchHearings(rangeSearchQueryEnvelope)).thenReturn(rangeSearchResponse);
        when(rangeSearchResponse.payloadAsJsonObject()).thenReturn(rangeSearchResponsePayload);
        when(exParteLinkedCaseApplicationFilter.removeApplicationsLinkedToExParteCases(rangeSearchResponsePayload, queryEnvelope)).thenReturn(rangeSearchResponsePayload);
        when(rangeSearchConverter.generateCourtListQueryPayload(courtCentreId, rangeSearchResponsePayload, startDate, endDate)).thenReturn(courtListResponse);

        final JsonObject response = courtListService.retrieveUnPublishedCourtList(courtCentreId, publishCourtListType, startDate, endDate, queryEnvelope);

        assertThat(response, is(courtListResponse));
    }

    @Test
    void shouldApplyExParteFilterToHearingsWhenPublishCourtListTypeIsWarn() {
        assertExParteFilterAppliedForListType(PublishCourtListType.WARN);
    }

    @Test
    void shouldApplyExParteFilterToHearingsWhenPublishCourtListTypeIsFirm() {
        assertExParteFilterAppliedForListType(PublishCourtListType.FIRM);
    }

    private void assertExParteFilterAppliedForListType(final PublishCourtListType publishCourtListType) {
        // CAD-1710
        final UUID courtCentreId = UUID.randomUUID();
        final LocalDate startDate = LocalDate.now();
        final String endDate = LocalDate.now().toString();

        final JsonEnvelope queryEnvelope = generateQuery(createObjectBuilder().build());

        final JsonEnvelope rangeSearchQueryEnvelope = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchResponse = mock(JsonEnvelope.class);

        final JsonArray unfilteredHearings = createArrayBuilder()
                .add(createObjectBuilder().add("id", "unfiltered-hearing").build())
                .build();
        final JsonArray filteredHearings = createArrayBuilder()
                .add(createObjectBuilder().add("id", "filtered-hearing").build())
                .build();
        final JsonObject rangeSearchResponsePayload = createObjectBuilder()
                .add("hearings", unfilteredHearings)
                .build();
        final JsonObject courtListResponse = mock(JsonObject.class);

        when(rangeSearchQueryRequestFactory.buildRangeSearchQueryEnvelope(courtCentreId, publishCourtListType, startDate, queryEnvelope)).thenReturn(rangeSearchQueryEnvelope);
        when(rangeSearchQuery.rangeSearchHearings(rangeSearchQueryEnvelope)).thenReturn(rangeSearchResponse);
        when(rangeSearchResponse.payloadAsJsonObject()).thenReturn(rangeSearchResponsePayload);
        final JsonObject exParteCasesRemovedPayload = createObjectBuilder().add("hearings", filteredHearings).build();
        final JsonObject linkedApplicationsRemovedPayload = createObjectBuilder().add("hearings", createArrayBuilder()).build();

        when(hearingJsonListConverterFilterEjectCases.filterExParteOffencesFromHearings(unfilteredHearings)).thenReturn(filteredHearings);
        when(exParteLinkedCaseApplicationFilter.removeApplicationsLinkedToExParteCases(exParteCasesRemovedPayload, queryEnvelope)).thenReturn(linkedApplicationsRemovedPayload);
        when(rangeSearchConverter.generateCourtListQueryPayload(any(), any(), any(), any())).thenReturn(courtListResponse);

        final JsonObject response = courtListService.retrieveUnPublishedCourtList(courtCentreId, publishCourtListType, startDate, endDate, queryEnvelope);

        assertThat(response, is(courtListResponse));
        verify(rangeSearchConverter).generateCourtListQueryPayload(courtCentreId, linkedApplicationsRemovedPayload, startDate, endDate);
    }

    @Test
    void shouldApplyExParteFilterToHearingsWhenPublishCourtListTypeIsDraft() {
        // CAD-1760 - DRAFT also backs the "Daily list" download
        assertExParteFilterAppliedForListType(PublishCourtListType.DRAFT);
    }

    @Test
    void shouldApplyExParteFilterToHearingsWhenPublishCourtListTypeIsFinal() {
        // CAD-1760
        assertExParteFilterAppliedForListType(PublishCourtListType.FINAL);
    }

    @Test
    void shouldReturnEmptyCourtList() {

        final UUID courtCentreId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = generateQuery(createObjectBuilder().build());

        final JsonObject courtSite1 = mock(JsonObject.class);
        final List<JsonObject> crestCourtSitesJson = Collections.singletonList(courtSite1);
        when(commonXhibitReferenceDataService.getCrestCourtSitesForCrownCourtCentre(courtCentreId)).thenReturn(crestCourtSitesJson);

        final JsonObject courtList = courtListService.emptyCourtList(courtCentreId);

        final JsonObject actualCourtList = courtList.getJsonArray("courtLists").getJsonObject(0);

        assertThat(actualCourtList.getJsonArray("sittings").size(), is(0));

        assertThat(actualCourtList.getJsonObject("crestCourtSite"), is(courtSite1));
    }

    private JsonEnvelope generateQuery(final JsonValue payload) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(UUID.fromString("a595f500-08f4-44d1-99bb-5547a5bcc9a6"))
                        .withName("event.name"),
                payload
        );
    }
}
