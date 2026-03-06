package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.time.ZonedDateTime.parse;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.ListingService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersParser;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class CourtListEventProcessorTest {
    @Spy
    @InjectMocks
    CourtListEventProcessor courtListEventProcessor;
    @Mock
    private Logger LOGGER;
    @Mock
    private PublishCourtListCommandSender publishCourtListCommandSender;
    @Mock
    private PublishCourtListRequestParametersParser publishCourtListRequestParametersParser;
    @Mock
    private ListingService listingService;
    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private CourtListExportService courtListExportService;

    @BeforeEach
    public void before() {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandlePublishCourtListRequested() {

        final JsonEnvelope tEnvelope = mock(JsonEnvelope.class);
        final PublishCourtListRequestParameters parameters = mock(PublishCourtListRequestParameters.class);
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json");

        when(publishCourtListRequestParametersParser.parse(tEnvelope)).thenReturn(parameters);
        when(listingService.getUnpublishedCourtListForCourtCentre(tEnvelope, parameters)).thenReturn(courtListJson);

        // Tested method
        courtListEventProcessor.handlePublishCourtListRequested(tEnvelope);

        // Assertions
        assertCourtListPublished(parameters, tEnvelope, courtListJson);
    }

    @Test
    public void shouldHandleCourtListExportRequested() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.now();
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json");

        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");
        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtCentreId,
                startDate,
                startDate.plusDays(5),
                PublishCourtListType.WARN,
                requestedTime,
                true

        );

        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.WARN.name())
                .add("startDate", startDate.toString())
                .add("endDate", startDate.plusDays(5).toString())
                .add("requestedTime", requestedTime.toString())
                .add("courtListJson", courtListJson.toString())
                .add("sendNotificationToParties", true)
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("listing.event.court-list-export-requested")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        // Tested method
        courtListEventProcessor.handleCourtListExportRequested(tEnvelope);
        verify(publishCourtListCommandSender).publishPublicMessageForCourtList(any(),any(),any());
    }




    @Test
    public void shouldHandleCourtListExportRequestedWithCorrectEndDate() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.of(2023,9,18);
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json");

        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");

        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.WARN.name())
                .add("startDate", startDate.toString())
                .add("endDate", startDate.plusDays(6).toString())
                .add("requestedTime", requestedTime.toString())
                .add("courtListJson", courtListJson.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("listing.event.court-list-export-requested")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        // Tested method
        courtListEventProcessor.handleCourtListExportRequested(tEnvelope);
        ArgumentCaptor<PublishCourtListRequestParameters> captor = ArgumentCaptor.forClass(PublishCourtListRequestParameters.class);
        verify(courtListExportService).exportCourtList(eq(tEnvelope), captor.capture(),eq(courtListJson));
        PublishCourtListRequestParameters parameters = captor.getValue();

        assertEquals(parameters.getEndDate(), startDate.plusDays(6));
    }

    @Test
    public void shouldHandleCourtListExportRequestedWithCorrectEndDateWhenEndDateIsSupplied() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.of(2023,9,18);
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json");

        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");

        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.WARN.name())
                .add("startDate", startDate.toString())
                .add("endDate", startDate.plusDays(4).toString())
                .add("requestedTime", requestedTime.toString())
                .add("courtListJson", courtListJson.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("listing.event.court-list-export-requested")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        // Tested method
        courtListEventProcessor.handleCourtListExportRequested(tEnvelope);
        ArgumentCaptor<PublishCourtListRequestParameters> captor = ArgumentCaptor.forClass(PublishCourtListRequestParameters.class);
        verify(courtListExportService).exportCourtList(eq(tEnvelope), captor.capture(),eq(courtListJson));
        PublishCourtListRequestParameters parameters = captor.getValue();

        assertEquals(parameters.getEndDate(), startDate.plusDays(4));
    }




    private void assertCourtListPublished(final PublishCourtListRequestParameters parameters,
                                          final JsonEnvelope tEnvelope,
                                          final JsonObject courtListData) {

        verify(listingService).getUnpublishedCourtListForCourtCentre(tEnvelope, parameters);
        verify(publishCourtListCommandSender).storePublishedCourtList(parameters, courtListData);
    }
}
