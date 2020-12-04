package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.RangeSearchQuery;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtListServiceTest {

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

    @InjectMocks
    private CourtListService courtListService;

    @Test
    public void retrieveCourtList() {

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
        when(rangeSearchConverter.generateCourtListQueryPayload(courtCentreId, rangeSearchResponsePayload, startDate, endDate)).thenReturn(courtListResponse);

        final JsonObject response = courtListService.retrieveUnPublishedCourtList(courtCentreId, publishCourtListType, startDate, endDate, queryEnvelope);

        assertThat(response, is(courtListResponse));
    }

    @Test
    public void shouldReturnEmptyCourtList() {

        final UUID courtCentreId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = generateQuery(createObjectBuilder().build());

        final JsonObject courtSite1 = mock(JsonObject.class);
        final List<JsonObject> crestCourtSitesJson = Collections.singletonList(courtSite1);
        when(commonXhibitReferenceDataService.getCrestCourtSitesForCrownCourtCentre(courtCentreId)).thenReturn(crestCourtSitesJson);

        final JsonObject courtList = courtListService.emptyCourtList(courtCentreId);

        final JsonObject actualCourtList = courtList.getJsonArray("courtLists").getJsonObject(0);

        Assert.assertThat(actualCourtList.getJsonArray("sittings").size(), is(0));

        Assert.assertThat(actualCourtList.getJsonObject("crestCourtSite"), is(courtSite1));
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
