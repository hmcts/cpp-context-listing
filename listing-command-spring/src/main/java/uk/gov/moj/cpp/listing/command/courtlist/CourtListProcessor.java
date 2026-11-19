package uk.gov.moj.cpp.listing.command.courtlist;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

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
import uk.gov.moj.cpp.listing.xhibit.webdav.XhibitService;

import java.io.StringReader;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The court-list steps of the JEE event processor (MBD refactor P2.3b, ADR 015): ports of
 * {@code CourtListEventProcessor} ({@code handlePublishCourtListRequested},
 * {@code handleCourtListExportRequested}), {@code CourtListExportService} and the command half of
 * {@code PublishCourtListCommandSender}. Both steps were asynchronous in JEE (processor after
 * commit); here {@link CourtListCommands} schedules them after commit on the court-list executor,
 * each in its own transaction. Where the JEE processor sent an internal command, the step calls the
 * {@link CourtListCommands} method directly, in the same transaction; public messages go to the
 * outbox in that transaction.
 */
@Component
public class CourtListProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListProcessor.class);

    private final TransactionTemplate requiresNew;
    private final PublishedCourtListReader publishedCourtListReader;
    private final CourtListCommands commands;
    private final CourtListMetadataGenerator courtListMetadataGenerator;
    private final CourtListFileGenerator courtListFileGenerator;
    private final XhibitService xhibitService;
    private final CourtListPublicMessages publicMessages;
    private final EventOutbox eventOutbox;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public CourtListProcessor(final PlatformTransactionManager transactionManager, final PublishedCourtListReader publishedCourtListReader,
                              final CourtListCommands commands, final CourtListMetadataGenerator courtListMetadataGenerator,
                              final CourtListFileGenerator courtListFileGenerator, final XhibitService xhibitService,
                              final CourtListPublicMessages publicMessages, final EventOutbox eventOutbox, final Clock clock) {
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.publishedCourtListReader = publishedCourtListReader;
        this.commands = commands;
        this.courtListMetadataGenerator = courtListMetadataGenerator;
        this.courtListFileGenerator = courtListFileGenerator;
        this.xhibitService = xhibitService;
        this.publicMessages = publicMessages;
        this.eventOutbox = eventOutbox;
        this.clock = clock;
    }

    /**
     * {@code handlePublishCourtListRequested}: build the court list from the viewstore (unpublished),
     * store it ({@code store-published-court-list}) and request its export
     * ({@code court-list-request-export}, as the processor sent it: new metadata with the request id
     * as stream id and the requesting user).
     */
    public void onPublishCourtListRequested(final Metadata publishCommand, final PublishCourtListRequest request,
                                            final boolean sendNotificationToParties) {
        requiresNew.executeWithoutResult(status -> {
            final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                    request.getPublishCourtListRequestId(),
                    request.getCourtCentreId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    PublishCourtListType.valueOf(request.getPublishCourtListType().name()),
                    request.getRequestedTime(),
                    sendNotificationToParties);

            LOGGER.info("handlePublishCourtListRequested: parameters={}", parameters);

            final JsonObject courtListJson = publishedCourtListReader.getUnpublishedCourtListForCourtCentre(parameters);

            commands.storePublishedCourtList(request.getCourtCentreId(), request.getPublishCourtListType(), request.getStartDate(),
                    courtListJson.toString());

            final Metadata.Builder exportCommand = Metadata.builder()
                    .withId(randomUUID())
                    .withName(CourtListCommands.COURT_LIST_REQUEST_EXPORT)
                    .withStreamId(request.getPublishCourtListRequestId())
                    .createdAt(now());
            publishCommand.userId().ifPresent(exportCommand::withUserId);

            commands.courtListRequestExport(exportCommand.build(), Json.createObjectBuilder()
                    .add("courtCentreId", request.getCourtCentreId().toString())
                    .add("startDate", request.getStartDate().toString())
                    .add("publishCourtListType", request.getPublishCourtListType().name())
                    .add("courtListJson", courtListJson.toString())
                    .add("sendNotificationToParties", sendNotificationToParties)
                    .add("endDate", request.getEndDate().toString())
                    .build());
        });
    }

    /**
     * {@code handleCourtListExportRequested}: render, validate and export the court list
     * ({@link #exportCourtList}), then publish {@code public.listing.court-list-published} whatever
     * the export outcome. The public messages take their metadata from the export command, as the
     * JEE ones took it from the {@code court-list-export-requested} event that command raised
     * (stream id: the court-list id).
     */
    public void onCourtListExportRequested(final Metadata exportCommand, final CourtListExportRequest request) {
        requiresNew.executeWithoutResult(status -> {
            LOGGER.info("courtListExportRequested: courtListId={}, courtCentreId={}, type={}, startDate={}", request.courtListId(),
                    request.courtCentreId(), request.publishCourtListType(), request.startDate());

            final PublishCourtListType publishCourtListType = PublishCourtListType.valueOf(request.publishCourtListType().name());
            final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                    request.courtListId(),
                    request.courtCentreId(),
                    request.startDate(),
                    calculateEndDate(publishCourtListType, request),
                    publishCourtListType,
                    request.requestedTime(),
                    request.sendNotificationToParties());

            final JsonObject courtListJson = parse(request.courtListJson());
            final List<DomainEvent> publicMessagesToWrite = new ArrayList<>();
            exportCourtList(parameters, courtListJson, publicMessagesToWrite);
            final UUID userId = exportCommand.userId().map(UUID::fromString).orElse(null);
            publicMessages.courtListPublished(parameters, courtListJson, userId).ifPresent(publicMessagesToWrite::add);

            eventOutbox.write(exportCommand, publicMessagesToWrite.stream()
                    .map(message -> new DomainEvent(message.name(), request.courtListId(), message.payload()))
                    .toList());
        });
    }

    /**
     * {@code CourtListExportService.exportCourtList}: any failure is recorded as a failed export.
     * {@code public.listing.court-daily-list} (DRAFT/FINAL) is written before the WebDAV PUT, so it
     * is published even when the export then fails, as before.
     */
    @SuppressWarnings("java:S1181")
    private void exportCourtList(final PublishCourtListRequestParameters parameters, final JsonObject courtListJson,
                                 final List<DomainEvent> publicMessagesToWrite) {
        final uk.gov.justice.listing.event.PublishCourtListType type =
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(parameters.getPublishCourtListType().name());
        try {
            final CourtListMetadata courtListMetadata = courtListMetadataGenerator.generate(parameters);

            final String courtListXml = courtListFileGenerator.generateXml(parameters, courtListMetadata, courtListJson);

            courtListFileGenerator.validateXml(parameters, courtListXml);

            publicMessages.courtDailyList(parameters, courtListXml).ifPresent(publicMessagesToWrite::add);

            xhibitService.sendToXhibit(courtListXml.getBytes(UTF_8), courtListMetadata.getFilename());

            commands.recordCourtListExportSuccessful(parameters.getCourtCentreId(), type, parameters.getStartDate(), parameters.getEndDate(),
                    courtListMetadata.getFilename(), now());
        } catch (final Exception e) {
            LOGGER.error("Court List export failed", e);
            commands.recordCourtListExportFailed(parameters.getCourtCentreId(), type, parameters.getStartDate(), parameters.getEndDate(),
                    getMessage(e), now());
        }
    }

    /** {@code CourtListEventProcessor.calculateEndDate}, unchanged. */
    static LocalDate calculateEndDate(final PublishCourtListType publishCourtListType, final CourtListExportRequest request) {
        if (publishCourtListType.isWeekCommencing() && request.endDate() != null) {
            return request.endDate();
        }
        final LocalDate startDate = request.startDate();
        if (!startDate.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            return publishCourtListType.isWeekCommencing() ? startDate.plusDays(5) : startDate;
        } else {
            return publishCourtListType.isWeekCommencing() ? startDate.plusDays(6) : startDate;
        }
    }

    private static JsonObject parse(final String courtListJson) {
        try (JsonReader reader = Json.createReader(new StringReader(courtListJson))) {
            return reader.readObject();
        }
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
