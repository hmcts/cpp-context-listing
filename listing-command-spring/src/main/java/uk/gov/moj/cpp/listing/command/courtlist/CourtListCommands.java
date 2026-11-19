package uk.gov.moj.cpp.listing.command.courtlist;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.getNextWorkingDay;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.moj.cpp.listing.command.support.AfterCommit;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.domain.json.ZonedDateTimes;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishCourtListRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The court-list commands (MBD refactor slice P2.3b, ADR 015). Each method is one JEE command:
 * the {@code ListingCommandHandler} method, the {@code PublishCourtListRequestAggregate} rule (its
 * only one: a request is week-commencing when its start and end dates differ), the
 * {@code PublishCourtListEventListener} write for the private event the handler raised
 * ({@link CourtListViewstore}), and the {@code CourtListEventProcessor} step that followed it.
 * No private event exists; there is nothing to publish synchronously (the court-list public
 * messages come from the export).
 *
 * <p>The JEE flow publish-court-list → (processor, after commit) build + store the list and request
 * the export → (processor, after commit) render, PUT to XHIBIT, record the outcome, publish was
 * asynchronous; so are the two follow-up steps here ({@link CourtListProcessor}, scheduled with
 * {@link AfterCommit} on the single-threaded court-list executor, in JEE order). The internal
 * commands the processor sent ({@code store-published-court-list}, {@code court-list-request-export},
 * {@code record-court-list-export-successful/-failed}) are the public methods below, called directly.
 */
@Service
public class CourtListCommands {

    public static final String PUBLISH_COURT_LIST = "listing.command.publish-court-list";
    public static final String PUBLISH_COURT_LISTS_FOR_CROWN_COURTS = "listing.command.publish-court-lists-for-crown-courts";
    public static final String RECORD_COURT_LIST_PRODUCED = "listing.command.record-court-list-produced";
    public static final String COURT_LIST_REQUEST_EXPORT = "listing.command.court-list-request-export";

    static final String REFERENCEDATA_QUERY_COURT_CENTRES = "referencedata.query.courtrooms";

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListCommands.class);

    private final CourtListViewstore viewstore;
    private final CourtListProcessor processor;
    private final AfterCommit afterCommit;
    private final CppQueryClient cppQueryClient;
    private final Clock clock;

    public CourtListCommands(final CourtListViewstore viewstore, @Lazy final CourtListProcessor processor, final AfterCommit afterCommit,
                             final CppQueryClient cppQueryClient, final Clock clock) {
        this.viewstore = viewstore;
        this.processor = processor;
        this.afterCommit = afterCommit;
        this.cppQueryClient = cppQueryClient;
        this.clock = clock;
    }

    /**
     * {@code listing.command.publish-court-list}: a new request (random id, as the JEE handler's
     * aggregate stream id), requested now (the payload's {@code requestedTime} is ignored, as before).
     */
    @Transactional
    public void publishCourtList(final Metadata command, final JsonObject payload) {
        publish(command,
                UUID.fromString(payload.getString("courtCentreId")),
                LocalDate.parse(payload.getString("startDate")),
                LocalDate.parse(payload.getString("endDate")),
                PublishCourtListType.valueOf(payload.getString("publishCourtListType")),
                optionalBoolean(payload, "sendNotificationToParties"));
    }

    /**
     * {@code listing.command.publish-court-lists-for-crown-courts}: a {@code FINAL} list for the next
     * working day for every crown court centre (reference data {@code oucodeL1Code=C}, as admin). A
     * failure for one centre is logged and the others still go ahead, as before.
     */
    @Transactional
    public void publishCourtListsForCrownCourts(final Metadata command, final JsonObject payload) {
        final List<UUID> crownCourtCentres = allCrownCourtCentres();
        LOGGER.debug("Attempting to request publication of the final court lists for [{}] crown court centres ...", crownCourtCentres.size());
        final LocalDate nextWorkingDay = getNextWorkingDay(now().toLocalDate());
        for (final UUID courtCentreId : crownCourtCentres) {
            try {
                publish(command, courtCentreId, nextWorkingDay, nextWorkingDay, PublishCourtListType.FINAL, null);
            } catch (final RuntimeException e) {
                LOGGER.error(String.format("Exception thrown While trying to publish the final court list for Court Centre [%s]", courtCentreId), e);
            }
        }
    }

    /**
     * {@code listing.command.record-court-list-produced} (no sender in JEE either; ported for the
     * contract): a {@code COURT_LIST_PRODUCED} status row, week-commencing as the request's aggregate
     * was ({@code false} for an unknown request).
     */
    @Transactional
    public void recordCourtListProduced(final Metadata command, final JsonObject payload) {
        final UUID publishCourtListRequestId = UUID.fromString(payload.getString("publishCourtListRequestId"));
        final ZonedDateTime producedTime = ZonedDateTimes.fromString(payload.getString("producedTime"));
        LocalDate.parse(payload.getString("publishDate")); // parsed (and so validated) by the JEE handler; the listener did not use it
        viewstore.publishCourtListProduced(
                UUID.fromString(payload.getString("courtCentreId")),
                PublishCourtListType.valueOf(payload.getString("publishCourtListType")),
                producedTime,
                UUID.fromString(payload.getString("courtListFileId")),
                payload.getString("courtListFileName"),
                viewstore.requestWeekCommencing(publishCourtListRequestId));
    }

    /**
     * {@code listing.command.court-list-request-export} (REST, and the processor's internal command
     * after storing a list): the export of the given court-list JSON, after commit. The JEE handler
     * appended {@code court-list-export-requested} to the deterministic court-list stream; nothing
     * was written to the viewstore.
     */
    @Transactional
    public void courtListRequestExport(final Metadata command, final JsonObject payload) {
        final UUID courtCentreId = UUID.fromString(payload.getString("courtCentreId"));
        final PublishCourtListType publishCourtListType = PublishCourtListType.valueOf(payload.getString("publishCourtListType"));
        final LocalDate startDate = LocalDate.parse(payload.getString("startDate"));
        final CourtListExportRequest request = new CourtListExportRequest(
                courtCentreId,
                courtListId(courtCentreId, publishCourtListType, startDate),
                payload.getString("courtListJson"),
                optionalString(payload, "endDate") == null ? null : LocalDate.parse(payload.getString("endDate")),
                publishCourtListType,
                now(),
                optionalBoolean(payload, "sendNotificationToParties"),
                startDate);
        afterCommit.run("export court list " + request.courtListId(), () -> processor.onCourtListExportRequested(command, request));
    }

    /**
     * {@code listing.command.store-published-court-list} (internal): the court-list JSON stored
     * under its deterministic court-list id, last updated now.
     */
    @Transactional
    public void storePublishedCourtList(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate,
                                        final String courtListJson) {
        viewstore.publishedCourtListStored(courtCentreId, publishCourtListType, startDate, courtListJson, now(),
                courtListId(courtCentreId, publishCourtListType, startDate));
    }

    /** {@code listing.command.record-court-list-export-successful} (internal). */
    @Transactional
    public void recordCourtListExportSuccessful(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate,
                                                final LocalDate endDate, final String courtListFileName, final ZonedDateTime exportedTime) {
        viewstore.publishCourtListExportSuccessful(courtCentreId, publishCourtListType, startDate, endDate, exportedTime, courtListFileName);
    }

    /** {@code listing.command.record-court-list-export-failed} (internal). */
    @Transactional
    public void recordCourtListExportFailed(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate,
                                            final LocalDate endDate, final String errorMessage, final ZonedDateTime failedTime) {
        viewstore.publishCourtListExportFailed(courtCentreId, publishCourtListType, startDate, endDate, failedTime, errorMessage);
    }

    /**
     * {@code UUIDService.getCourtListId}: one court-list id per court centre, type and start date
     * (a name-based UUID of {@code "<courtCentreId>/<type>/<startDate>"}).
     */
    public static UUID courtListId(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {
        return nameUUIDFromBytes(String.format("%s/%s/%s", courtCentreId, publishCourtListType.name(), startDate).getBytes(UTF_8));
    }

    private void publish(final Metadata command, final UUID courtCentreId, final LocalDate startDate, final LocalDate endDate,
                         final PublishCourtListType publishCourtListType, final Boolean sendNotificationToParties) {
        final UUID publishCourtListRequestId = randomUUID();
        final ZonedDateTime requestedTime = now();
        // PublishCourtListRequestAggregate.recordCourtListRequested: week commencing when the dates differ.
        final boolean weekCommencing = !startDate.equals(endDate);
        final PublishCourtListRequest request = new PublishCourtListRequest(publishCourtListRequestId, courtCentreId, publishCourtListType,
                startDate, endDate, weekCommencing, requestedTime);

        viewstore.recordRequest(request);
        viewstore.publishCourtListRequested(startDate.toString(), endDate.toString(), courtCentreId, publishCourtListType, requestedTime);

        // The requested event's sendNotificationToParties was omitted when null; the processor read it as false.
        final boolean notify = sendNotificationToParties != null && sendNotificationToParties;
        afterCommit.run("publish court list request " + publishCourtListRequestId,
                () -> processor.onPublishCourtListRequested(command, request, notify));
    }

    private List<UUID> allCrownCourtCentres() {
        final JsonObject response = cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_COURT_CENTRES,
                        Json.createObjectBuilder().add("oucodeL1Code", "C").build())
                .orElseThrow(() -> new IllegalStateException("No response from " + REFERENCEDATA_QUERY_COURT_CENTRES));
        return response.getJsonArray("organisationunits").stream()
                .filter(value -> value.getValueType() == JsonValue.ValueType.OBJECT)
                .map(JsonObject.class::cast)
                .map(CourtListCommands::toCourtCentreId)
                .toList();
    }

    /** {@code toCourtCentreDetails}: the id, after reading the fields the JEE handler parsed (a bad centre fails the command, as before). */
    private static UUID toCourtCentreId(final JsonObject courtCentre) {
        final UUID courtCentreId = UUID.fromString(courtCentre.getString("id"));
        courtCentre.getString("defaultDurationHrs");
        LocalTime.parse(courtCentre.getString("defaultStartTime"));
        return courtCentreId;
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    private static Boolean optionalBoolean(final JsonObject payload, final String name) {
        return payload.containsKey(name) && !payload.isNull(name) ? payload.getBoolean(name) : null;
    }

    private static String optionalString(final JsonObject payload, final String name) {
        return payload.containsKey(name) && !payload.isNull(name) ? payload.getString(name) : null;
    }
}
