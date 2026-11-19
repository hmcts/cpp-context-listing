package uk.gov.moj.cpp.listing.command.courtlist;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishCourtListRequest;
import uk.gov.moj.cpp.listing.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.xhibit.courtlist.PublishedCourtListReader;
import uk.gov.moj.cpp.listing.xhibit.exception.GenerationFailedException;
import uk.gov.moj.cpp.listing.xhibit.webdav.ExportFailedException;
import uk.gov.moj.cpp.listing.xhibit.webdav.XhibitService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Ports {@code CourtListEventProcessorTest} and {@code CourtListExportServiceTest}: the two
 * asynchronous steps of the court-list saga, without private events (ADR 015).
 */
class CourtListProcessorTest {

    private static final Instant NOW = Instant.parse("2026-11-19T10:15:30.123Z");
    private static final ZonedDateTime NOW_UTC = ZonedDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final LocalDate MONDAY = LocalDate.of(2023, 9, 18);

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final PublishedCourtListReader reader = mock(PublishedCourtListReader.class);
    private final CourtListCommands commands = mock(CourtListCommands.class);
    private final CourtListMetadataGenerator metadataGenerator = mock(CourtListMetadataGenerator.class);
    private final CourtListFileGenerator fileGenerator = mock(CourtListFileGenerator.class);
    private final XhibitService xhibitService = mock(XhibitService.class);
    private final CourtListPublicMessages publicMessages = mock(CourtListPublicMessages.class);
    private final EventOutbox eventOutbox = mock(EventOutbox.class);
    private final CourtListProcessor processor = new CourtListProcessor(transactionManager, reader, commands, metadataGenerator, fileGenerator,
            xhibitService, publicMessages, eventOutbox, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID courtCentreId = randomUUID();
    private final UUID courtListId = randomUUID();
    private final UUID userId = randomUUID();
    private final Metadata exportCommand = Metadata.builder().withId(randomUUID()).withName("listing.command.court-list-request-export")
            .withUserId(userId.toString()).build();
    private final JsonObject courtListJson = Json.createObjectBuilder().add("courtLists", Json.createArrayBuilder()).build();

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void publishRequestedStoresTheUnpublishedListAndRequestsItsExport() {
        final UUID requestId = randomUUID();
        final PublishCourtListRequest request = new PublishCourtListRequest(requestId, courtCentreId,
                uk.gov.justice.listing.event.PublishCourtListType.WARN, MONDAY, MONDAY.plusDays(4), true, NOW_UTC);
        when(reader.getUnpublishedCourtListForCourtCentre(any())).thenReturn(courtListJson);
        final Metadata publishCommand = Metadata.builder().withId(randomUUID()).withName("listing.command.publish-court-list")
                .withUserId("user-1").withSessionId("session-1").build();

        processor.onPublishCourtListRequested(publishCommand, request, true);

        final ArgumentCaptor<PublishCourtListRequestParameters> parameters = ArgumentCaptor.forClass(PublishCourtListRequestParameters.class);
        verify(reader).getUnpublishedCourtListForCourtCentre(parameters.capture());
        assertThat(parameters.getValue().getCourtListId()).isEqualTo(requestId);
        assertThat(parameters.getValue().getCourtCentreId()).isEqualTo(courtCentreId);
        assertThat(parameters.getValue().getStartDate()).isEqualTo(MONDAY);
        assertThat(parameters.getValue().getEndDate()).isEqualTo(MONDAY.plusDays(4));
        assertThat(parameters.getValue().getPublishCourtListType()).isEqualTo(PublishCourtListType.WARN);
        assertThat(parameters.getValue().getRequestedTime()).isEqualTo(NOW_UTC);
        assertThat(parameters.getValue().getSendNotificationToParties()).isTrue();

        final InOrder order = inOrder(commands);
        order.verify(commands).storePublishedCourtList(courtCentreId, uk.gov.justice.listing.event.PublishCourtListType.WARN, MONDAY,
                courtListJson.toString());
        final ArgumentCaptor<Metadata> metadata = ArgumentCaptor.forClass(Metadata.class);
        final ArgumentCaptor<JsonObject> payload = ArgumentCaptor.forClass(JsonObject.class);
        order.verify(commands).courtListRequestExport(metadata.capture(), payload.capture());

        // PublishCourtListCommandSender.sendCommandWithUser: new metadata, the requesting user only.
        assertThat(metadata.getValue().name()).isEqualTo("listing.command.court-list-request-export");
        assertThat(metadata.getValue().id()).isNotEqualTo(publishCommand.id());
        assertThat(metadata.getValue().streamId()).contains(requestId);
        assertThat(metadata.getValue().userId()).contains("user-1");
        assertThat(metadata.getValue().sessionId()).isEmpty();
        assertThat(metadata.getValue().causation()).isEmpty();
        assertThat(metadata.getValue().createdAt()).contains(NOW_UTC);
        assertThat(payload.getValue()).isEqualTo(Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", MONDAY.toString())
                .add("publishCourtListType", "WARN")
                .add("courtListJson", courtListJson.toString())
                .add("sendNotificationToParties", true)
                .add("endDate", MONDAY.plusDays(4).toString())
                .build());
        assertThat(definition().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void aSuccessfulExportPublishesTheDailyListSendsItAndRecordsTheSuccess() throws Exception {
        final CourtListMetadata metadata = new CourtListMetadata("DailyList_421_20261119101530.xml", "doc", NOW_UTC);
        when(metadataGenerator.generate(any())).thenReturn(metadata);
        when(fileGenerator.generateXml(any(), eq(metadata), eq(courtListJson))).thenReturn("<xml/>");
        final DomainEvent dailyList = new DomainEvent("public.listing.court-daily-list", courtListId, Json.createObjectBuilder().build());
        final DomainEvent published = new DomainEvent("public.listing.court-list-published", courtListId, Json.createObjectBuilder().build());
        when(publicMessages.courtDailyList(any(), eq("<xml/>"))).thenReturn(Optional.of(dailyList));
        when(publicMessages.courtListPublished(any(), eq(courtListJson), eq(userId))).thenReturn(Optional.of(published));

        processor.onCourtListExportRequested(exportCommand, exportRequest(PublishCourtListType.DRAFT, MONDAY, null, true));

        final InOrder order = inOrder(fileGenerator, xhibitService, commands, eventOutbox);
        order.verify(fileGenerator).validateXml(any(), eq("<xml/>"));
        order.verify(xhibitService).sendToXhibit("<xml/>".getBytes(UTF_8), "DailyList_421_20261119101530.xml");
        order.verify(commands).recordCourtListExportSuccessful(courtCentreId, uk.gov.justice.listing.event.PublishCourtListType.DRAFT, MONDAY,
                MONDAY, "DailyList_421_20261119101530.xml", NOW_UTC);
        order.verify(eventOutbox).write(exportCommand, List.of(dailyList, published));
        verify(commands, never()).recordCourtListExportFailed(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aFailedWebDavPutIsRecordedAsAFailedExportAndTheDailyListIsStillPublished() throws Exception {
        final CourtListMetadata metadata = new CourtListMetadata("DailyList.xml", "doc", NOW_UTC);
        when(metadataGenerator.generate(any())).thenReturn(metadata);
        when(fileGenerator.generateXml(any(), any(), any())).thenReturn("<xml/>");
        final DomainEvent dailyList = new DomainEvent("public.listing.court-daily-list", courtListId, Json.createObjectBuilder().build());
        when(publicMessages.courtDailyList(any(), anyString())).thenReturn(Optional.of(dailyList));
        when(publicMessages.courtListPublished(any(), any(), any())).thenReturn(Optional.empty());
        doThrow(new ExportFailedException("Failed to send to Xhibit,CPF01: Failed to put file, for document with name: DailyList.xml", null))
                .when(xhibitService).sendToXhibit(any(), anyString());

        processor.onCourtListExportRequested(exportCommand, exportRequest(PublishCourtListType.FINAL, MONDAY, null, true));

        verify(commands).recordCourtListExportFailed(courtCentreId, uk.gov.justice.listing.event.PublishCourtListType.FINAL, MONDAY, MONDAY,
                "ExportFailedException: Failed to send to Xhibit,CPF01: Failed to put file, for document with name: DailyList.xml", NOW_UTC);
        verify(commands, never()).recordCourtListExportSuccessful(any(), any(), any(), any(), any(), any());
        verify(eventOutbox).write(exportCommand, List.of(dailyList));
    }

    @Test
    void anInvalidXmlIsRecordedAsAFailedExportWithoutDailyListOrPut() throws Exception {
        when(metadataGenerator.generate(any())).thenReturn(new CourtListMetadata("WarnedList.xml", "doc", NOW_UTC));
        when(fileGenerator.generateXml(any(), any(), any())).thenReturn("<xml/>");
        doThrow(new GenerationFailedException("Could not validate XML")).when(fileGenerator).validateXml(any(), anyString());
        final DomainEvent published = new DomainEvent("public.listing.court-list-published", courtListId, Json.createObjectBuilder().build());
        when(publicMessages.courtListPublished(any(), any(), any())).thenReturn(Optional.of(published));

        processor.onCourtListExportRequested(exportCommand, exportRequest(PublishCourtListType.WARN, MONDAY, MONDAY.plusDays(4), true));

        verify(publicMessages, never()).courtDailyList(any(), anyString());
        verify(xhibitService, never()).sendToXhibit(any(), anyString());
        verify(commands).recordCourtListExportFailed(eq(courtCentreId), eq(uk.gov.justice.listing.event.PublishCourtListType.WARN), eq(MONDAY),
                eq(MONDAY.plusDays(4)), eq("GenerationFailedException: Could not validate XML"), eq(NOW_UTC));
        verify(eventOutbox).write(exportCommand, List.of(published));
    }

    @Test
    void theExportParametersCarryTheRequestAndTheCourtListId() {
        when(metadataGenerator.generate(any())).thenThrow(new IllegalStateException("no refdata"));
        when(publicMessages.courtListPublished(any(), any(), any())).thenReturn(Optional.empty());

        processor.onCourtListExportRequested(exportCommand, exportRequest(PublishCourtListType.FIRM, MONDAY, null, false));

        final ArgumentCaptor<PublishCourtListRequestParameters> parameters = ArgumentCaptor.forClass(PublishCourtListRequestParameters.class);
        verify(publicMessages).courtListPublished(parameters.capture(), eq(courtListJson), eq(userId));
        assertThat(parameters.getValue().getCourtListId()).isEqualTo(courtListId);
        assertThat(parameters.getValue().getCourtCentreId()).isEqualTo(courtCentreId);
        assertThat(parameters.getValue().getRequestedTime()).isEqualTo(NOW_UTC.minusHours(1));
        assertThat(parameters.getValue().getSendNotificationToParties()).isFalse();
        assertThat(parameters.getValue().getEndDate()).isEqualTo(MONDAY.plusDays(6));
    }

    @Test
    void endDateIsTheRequestsForAWeekCommencingListThatHasOne() {
        assertThat(CourtListProcessor.calculateEndDate(PublishCourtListType.WARN, exportRequest(PublishCourtListType.WARN, MONDAY, MONDAY.plusDays(4), true)))
                .isEqualTo(MONDAY.plusDays(4));
    }

    @Test
    void endDateOfAWeekCommencingListWithoutOneIsTheSundayOrFiveDaysOn() {
        assertThat(CourtListProcessor.calculateEndDate(PublishCourtListType.FIRM, exportRequest(PublishCourtListType.FIRM, MONDAY, null, true)))
                .isEqualTo(MONDAY.plusDays(6));
        final LocalDate tuesday = MONDAY.plusDays(1);
        assertThat(CourtListProcessor.calculateEndDate(PublishCourtListType.WARN, exportRequest(PublishCourtListType.WARN, tuesday, null, true)))
                .isEqualTo(tuesday.plusDays(5));
    }

    @Test
    void endDateOfADailyListIsItsStartDateEvenWhenOneIsGiven() {
        assertThat(CourtListProcessor.calculateEndDate(PublishCourtListType.DRAFT, exportRequest(PublishCourtListType.DRAFT, MONDAY, MONDAY.plusDays(3), true)))
                .isEqualTo(MONDAY);
    }

    private CourtListExportRequest exportRequest(final PublishCourtListType type, final LocalDate startDate, final LocalDate endDate,
                                                 final Boolean sendNotificationToParties) {
        return new CourtListExportRequest(courtCentreId, courtListId, courtListJson.toString(), endDate,
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(type.name()), NOW_UTC.minusHours(1), sendNotificationToParties, startDate);
    }

    private TransactionDefinition definition() {
        final ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        return definition.getValue();
    }
}
