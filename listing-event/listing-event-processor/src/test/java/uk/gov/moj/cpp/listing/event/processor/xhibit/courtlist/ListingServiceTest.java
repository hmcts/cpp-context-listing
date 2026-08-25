package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingServiceTest {

    @InjectMocks
    private ListingService listingService;

    @Mock
    private HearingQueryView hearingQueryView;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestCaptor;

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

    @BeforeEach
    public void before() {
        final JsonObject hearing = createObjectBuilder().add("id", "HEARINGID").build();
        final JsonObject payload = createObjectBuilder().add("hearings", JsonObjects.createArrayBuilder().add(hearing)).build();
        inputEnvelope = envelopeFrom(metadataBuilder().withName("listing").withId(randomUUID()), createObjectBuilder());

        final JsonEnvelope responseEnvelope = envelopeFrom(metadataBuilder().withName("listing.courtlist").withId(randomUUID()), payload);
        lenient().when(hearingQueryView.retrieveCourtList(any(JsonEnvelope.class))).thenReturn(responseEnvelope);
    }

    @Test
    public void shouldGetPublishedCourtList() {

        final JsonObject response = listingService.getPublishedCourtListForCourtCentre(
                inputEnvelope,
                courtCentreId,
                publishCourtListType,
                startDate);

        verifyResponse(response);

        verifyQueryParameters(startDate, courtCentreId, publishCourtListType, true);
    }

    @Test
    public void shouldGetUnpublishedCourtList() {

        final JsonObject response = listingService.getUnpublishedCourtListForCourtCentre(inputEnvelope, parameters);

        verifyResponse(response);

        verifyQueryParameters(startDate, courtCentreId, publishCourtListType, false);
    }

    @Test
    public void shouldRemoveHearingWhenADefendantHasNoOffencesFromUnpublishedCourtList() {

        final JsonObject offence = createObjectBuilder().add("offenceCode", "TH68001").build();
        final JsonObject defendantWithOffence = createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offences", JsonObjects.createArrayBuilder().add(offence).build())
                .build();
        final JsonObject defendantWithoutOffence = createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offences", JsonObjects.createArrayBuilder().build())
                .build();

        final JsonObject hearingWithoutOffence = createObjectBuilder()
                .add("startTime", "2019-11-13T10:00:00")
                .add("defendants", JsonObjects.createArrayBuilder().add(defendantWithoutOffence).build())
                .build();
        final JsonObject hearingWithOffence = createObjectBuilder()
                .add("startTime", "2019-11-13T11:00:00")
                .add("defendants", JsonObjects.createArrayBuilder().add(defendantWithOffence).build())
                .build();

        final JsonObject sitting = createObjectBuilder()
                .add("sittingDate", "2019-11-13")
                .add("hearings", JsonObjects.createArrayBuilder().add(hearingWithoutOffence).add(hearingWithOffence).build())
                .build();

        final JsonObject courtListPayload = createObjectBuilder()
                .add("courtLists", JsonObjects.createArrayBuilder()
                        .add(createObjectBuilder().add("sittings", JsonObjects.createArrayBuilder().add(sitting).build()).build())
                        .build())
                .build();

        final JsonEnvelope responseEnvelope = envelopeFrom(metadataBuilder().withName("listing.courtlist").withId(randomUUID()), courtListPayload);
        when(hearingQueryView.retrieveCourtList(any(JsonEnvelope.class))).thenReturn(responseEnvelope);

        final JsonObject response = listingService.getUnpublishedCourtListForCourtCentre(inputEnvelope, parameters);

        final JsonArray remainingHearings = response.getJsonArray("courtLists").getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("hearings");

        assertThat(remainingHearings.size(), is(1));
        assertThat(remainingHearings.getJsonObject(0).getString("startTime"), is("2019-11-13T11:00:00"));
    }

    private void verifyResponse(final JsonObject response) {

        assertThat(response.getJsonArray("hearings").getValuesAs(JsonObject.class)
                .get(0).getString("id"), is("HEARINGID"));
    }

    private void verifyQueryParameters(final LocalDate startDate, final UUID courtCentreId,
                                       final PublishCourtListType publishCourtListType,
                                       final boolean isPublished) {

        verify(hearingQueryView).retrieveCourtList(requestCaptor.capture());

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
