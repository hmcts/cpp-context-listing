package uk.gov.moj.cpp.listing.command.courtlist;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_FAILED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishStatus;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatus;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishCourtListRequest;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishCourtListRequestRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * The viewstore writes of the court-list slice (MBD refactor P2.3.1, ADR 015): the JEE
 * {@code PublishCourtListEventListener} handlers ported as plain methods, one per private event the
 * JEE command would have raised, writing exactly the same rows. Called from
 * {@link CourtListCommands} inside the command's transaction.
 *
 * <p>Plus the additive state of ADR 015 §3: {@code publish_court_list_request} (changeset 032), the
 * {@code PublishCourtListRequestAggregate}'s {@code weekCommencing}.
 */
@Component
public class CourtListViewstore {

    private final CourtListPublishStatusJdbcRepository courtListPublishStatusRepository;
    private final PublishedCourtListRepository publishedCourtListRepository;
    private final PublishCourtListRequestRepository publishCourtListRequestRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourtListViewstore(final CourtListPublishStatusJdbcRepository courtListPublishStatusRepository,
                              final PublishedCourtListRepository publishedCourtListRepository,
                              final PublishCourtListRequestRepository publishCourtListRequestRepository) {
        this.courtListPublishStatusRepository = courtListPublishStatusRepository;
        this.publishedCourtListRepository = publishedCourtListRepository;
        this.publishCourtListRequestRepository = publishCourtListRequestRepository;
    }

    /** The aggregate state of a publish request ({@code PublishCourtListRequestAggregate}). */
    public void recordRequest(final PublishCourtListRequest request) {
        publishCourtListRequestRepository.save(request);
    }

    /** The {@code weekCommencing} the request's aggregate held; {@code false} for an unknown request (a new aggregate). */
    public boolean requestWeekCommencing(final UUID publishCourtListRequestId) {
        return publishCourtListRequestRepository.findById(publishCourtListRequestId)
                .map(PublishCourtListRequest::isWeekCommencing)
                .orElse(false);
    }

    /** {@code listing.event.publish-court-list-requested} → {@code courtListPublishRequested}. */
    public void publishCourtListRequested(final String startDate, final String endDate, final UUID courtCentreId,
                                          final PublishCourtListType publishCourtListType, final ZonedDateTime requestedTime) {
        courtListPublishStatusRepository.save(publishStatus(startDate, endDate, courtCentreId, publishCourtListType,
                COURT_LIST_REQUESTED, requestedTime));
    }

    /** {@code listing.event.publish-court-list-produced} → {@code courtListPublishProduced}. */
    public void publishCourtListProduced(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final ZonedDateTime producedTime,
                                         final UUID courtListFileId, final String courtListFileName, final boolean weekCommencing) {
        courtListPublishStatusRepository.save(new CourtListPublishStatus(randomUUID(), courtCentreId, publishCourtListType, COURT_LIST_PRODUCED,
                producedTime, courtListFileId, courtListFileName, "", producedTime.toLocalDate(), weekCommencing));
    }

    /**
     * {@code listing.event.published-court-list-stored} → {@code storePublishedCourtCentreList}: the
     * court-list JSON, parsed as the JEE listener did ({@code new ObjectMapper().readTree}), saved
     * (merged) under (court centre, type, start date); a re-store clears the export outcome.
     */
    public void publishedCourtListStored(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate,
                                         final String courtListJson, final ZonedDateTime lastUpdated, final UUID courtListId) {
        try {
            publishedCourtListRepository.save(new PublishedCourtList(courtCentreId, publishCourtListType, startDate,
                    objectMapper.readTree(courtListJson), lastUpdated, null, courtListId));
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("courtListJson is not JSON", e);
        }
    }

    /** {@code listing.event.publish-court-list-export-successful} → {@code courtListPublishExportSuccessful}. */
    public void publishCourtListExportSuccessful(final UUID courtCentreId, final PublishCourtListType publishCourtListType,
                                                 final LocalDate startDate, final LocalDate endDate, final ZonedDateTime exportedTime,
                                                 final String courtListFileName) {
        final PublishedCourtList publishedCourtList = storedCourtList(courtCentreId, publishCourtListType, startDate);
        publishedCourtList.setLastExported(exportedTime);
        publishedCourtListRepository.save(publishedCourtList);

        final CourtListPublishStatus exportSuccessful = publishStatus(startDate.toString(), endDate.toString(), courtCentreId,
                publishCourtListType, EXPORT_SUCCESSFUL, exportedTime);
        exportSuccessful.setCourtListFileName(courtListFileName);
        courtListPublishStatusRepository.save(exportSuccessful);
    }

    /** {@code listing.event.publish-court-list-export-failed} → {@code courtListPublishExportFailed}. */
    public void publishCourtListExportFailed(final UUID courtCentreId, final PublishCourtListType publishCourtListType,
                                             final LocalDate startDate, final LocalDate endDate, final ZonedDateTime failedTime,
                                             final String errorMessage) {
        final PublishedCourtList publishedCourtList = storedCourtList(courtCentreId, publishCourtListType, startDate);
        publishedCourtList.setLastFailed(failedTime);
        publishedCourtList.setErrorMessage(errorMessage);
        publishedCourtListRepository.save(publishedCourtList);

        final CourtListPublishStatus exportFailed = publishStatus(startDate.toString(), endDate.toString(), courtCentreId,
                publishCourtListType, EXPORT_FAILED, failedTime);
        exportFailed.setErrorMessage(errorMessage);
        courtListPublishStatusRepository.save(exportFailed);
    }

    /**
     * The stored court list; the JEE listener dereferenced {@code findBy(pk)} without a check, so an
     * export outcome for a list that was never stored failed the transaction. Same here, with a message.
     */
    private PublishedCourtList storedCourtList(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {
        return Optional.ofNullable(publishedCourtListRepository.findBy(new PublishedCourtListPrimaryKey(courtCentreId, publishCourtListType, startDate)))
                .orElseThrow(() -> new IllegalStateException("No published court list for court centre " + courtCentreId + ", "
                        + publishCourtListType + ", " + startDate));
    }

    /** {@code generateCourtListPublishStatus}: week commencing when the start and end dates differ. */
    private static CourtListPublishStatus publishStatus(final String startDate, final String endDate, final UUID courtCentreId,
                                                       final PublishCourtListType publishCourtListType, final PublishStatus publishStatus,
                                                       final ZonedDateTime lastUpdated) {
        final boolean weekCommencing = !startDate.equalsIgnoreCase(endDate);
        return new CourtListPublishStatus(randomUUID(), courtCentreId, publishCourtListType, publishStatus, lastUpdated,
                LocalDate.parse(startDate), weekCommencing);
    }
}
