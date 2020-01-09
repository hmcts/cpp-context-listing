package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingServiceTest {

    @InjectMocks
    private ListingService listingService;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope> requestCaptor;

    private JsonEnvelope inputEnvelope;
    private LocalDate startDate = parse("2019-11-13");
    private LocalDate endDate = parse("2019-11-30");
    private UUID courtCentreId = randomUUID();
    private PublishCourtListType publishCourtListType = WARN;
    private PublishCourtListRequestParameters parameters = PublishCourtListRequestParametersBuilder
            .withDefaults()
            .withCourtCentreId(courtCentreId)
            .withStartDate(startDate)
            .withEndDate(endDate)
            .publishCourtListType(publishCourtListType)
            .build();

    @Before
    public void before() {
        final JsonObject hearing = createObjectBuilder().add("id", "HEARINGID").build();
        final JsonObject payload = createObjectBuilder().add("hearings", Json.createArrayBuilder().add(hearing)).build();
        inputEnvelope = envelopeFrom(metadataBuilder().withName("listing").withId(randomUUID()), createObjectBuilder());

        final JsonEnvelope responseEnvelope = envelopeFrom(metadataBuilder().withName("listing.courtlist").withId(randomUUID()), payload);
        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);
    }

    @Test
    public void shouldGetPublishedCourtList() {

        final JsonObject response = listingService.getPublishedCourtListForCourtCentre(inputEnvelope, parameters);

        verifyResponse(response);

        verifyQueryParameters(startDate, courtCentreId, publishCourtListType, true);
    }

    @Test
    public void shouldGetUnpublishedCourtList() {

        final JsonObject response = listingService.getUnpublishedCourtListForCourtCentre(inputEnvelope, parameters);

        verifyResponse(response);

        verifyQueryParameters(startDate, courtCentreId, publishCourtListType, false);
    }

    private void verifyResponse(final JsonObject response) {

        assertThat(response.getJsonArray("hearings").getValuesAs(JsonObject.class)
                .get(0).getString("id"), is("HEARINGID"));
    }

    private void verifyQueryParameters(final LocalDate startDate, final UUID courtCentreId,
                                       final PublishCourtListType publishCourtListType,
                                       final boolean isPublished) {

        verify(requester).request(requestCaptor.capture());

        final JsonObject actualRequestParameters = (JsonObject) requestCaptor.getValue().payload();

        assertThat(requestCaptor.getValue().metadata().name(), is("listing.courtlist"));
        assertThat(actualRequestParameters.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(actualRequestParameters.getString("startDate"), is(startDate.toString()));
        assertThat(actualRequestParameters.getString("publishCourtListType"), is(publishCourtListType.name()));
        if (isPublished) {
            assertThat(actualRequestParameters.getBoolean("published"), is(true));
        } else {
            assertThat(actualRequestParameters.containsKey("published"), is(false));
        }
    }
}
