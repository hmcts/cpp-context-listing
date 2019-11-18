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
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.DRAFT;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FINAL;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class ListingServiceTest {

    @Parameterized.Parameter(0)
    public PublishCourtListType publishCourtListType;

    @Parameterized.Parameter(1)
    public boolean shouldUseWeekCommencingQueryParameters;

    @InjectMocks
    private ListingService listingService;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope> requestCaptor;

    @Parameterized.Parameters(name = "{index}: Test with PublishCourtListType={0}, shouldUseWeekCommencingQueryParameters is:{1} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{WARN, true}, {FIRM, true}, {DRAFT, false}, {FINAL, false}};
        return Arrays.asList(data);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldUseCorrectParameters() {

        final JsonObject hearing = createObjectBuilder().add("id", "HEARINGID").build();
        final JsonObject payload = createObjectBuilder().add("hearings", Json.createArrayBuilder().add(hearing)).build();
        final JsonEnvelope inputEnvelope = envelopeFrom(metadataBuilder().withName("listing").withId(randomUUID()), createObjectBuilder());
        final JsonEnvelope responseEnvelope = envelopeFrom(metadataBuilder().withName("listing.range.search.hearings").withId(randomUUID()), payload);

        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);

        final LocalDate startDate = parse("2019-11-13");
        final LocalDate endDate = parse("2019-11-30");
        final UUID courtCentreId = randomUUID();

        final PublishCourtListRequestParameters parameters = PublishCourtListRequestParametersBuilder
                .withDefaults()
                .withCourtCentreId(courtCentreId)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .publishCourtListType(publishCourtListType)
                .build();

        final JsonObject response = listingService.getCourtListForPublishing(inputEnvelope, parameters);

        verifyResponse(response);

        verifyQueryParameters(startDate, endDate, courtCentreId);
    }

    private void verifyResponse(final JsonObject response) {

        assertThat(response.getJsonArray("hearings").getValuesAs(JsonObject.class)
                .get(0).getString("id"), is("HEARINGID"));
    }

    private void verifyQueryParameters(final LocalDate startDate, final LocalDate endDate, final UUID courtCentreId) {

        verify(requester).request(requestCaptor.capture());

        final JsonObject actualRequestParameters = (JsonObject) requestCaptor.getValue().payload();

        assertThat(requestCaptor.getValue().metadata().name(), is("listing.range.search.hearings"));
        assertThat(actualRequestParameters.getString("courtCentreId"), is(courtCentreId.toString()));

        if (shouldUseWeekCommencingQueryParameters) {
            assertThat(actualRequestParameters.getString("jurisdictionType"), is("CROWN"));
            assertThat(actualRequestParameters.getString("weekCommencingStartDate"), is(startDate.toString()));
            assertThat(actualRequestParameters.getString("weekCommencingEndDate"), is(endDate.toString()));
            assertThat(actualRequestParameters.containsKey("startDate"), is(false));
            assertThat(actualRequestParameters.containsKey("endDate"), is(false));

        } else {
            assertThat(actualRequestParameters.getString("startDate"), is(startDate.toString()));
            assertThat(actualRequestParameters.getString("endDate"), is(endDate.toString()));
            assertThat(actualRequestParameters.containsKey("jurisdictionType"), is(false));
            assertThat(actualRequestParameters.containsKey("weekCommencingStartDate"), is(false));
            assertThat(actualRequestParameters.containsKey("weekCommencingEndDate"), is(false));
        }
    }
}
