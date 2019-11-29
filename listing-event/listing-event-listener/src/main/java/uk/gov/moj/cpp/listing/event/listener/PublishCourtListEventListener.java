package uk.gov.moj.cpp.listing.event.listener;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_FAILED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListPublishStatus;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class PublishCourtListEventListener {

    @Inject
    private CourtListPublishStatusJdbcRepository courtListRepository;

    @Handles("listing.event.publish-court-list-requested")
    public void courtListPublishRequested(final Envelope<PublishCourtListRequested> event) {
        final PublishCourtListRequested publishCourtListRequested = event.payload();

        final String startDate = publishCourtListRequested.getStartDate();
        final String endDate = publishCourtListRequested.getEndDate();
        boolean weekCommencing = false;
        if (!startDate.equalsIgnoreCase(endDate)) {
            weekCommencing = true;
        }

        final CourtListPublishStatus publishRequested = new CourtListPublishStatus(randomUUID(),
                publishCourtListRequested.getCourtCentreId(),
                publishCourtListRequested.getPublishCourtListType(), COURT_LIST_REQUESTED,
                publishCourtListRequested.getRequestedTime(), parse(startDate), weekCommencing);
        courtListRepository.save(publishRequested);
    }

    @Handles("listing.event.publish-court-list-produced")
    public void courtListPublishProduced(final Envelope<PublishCourtListProduced> event) {
        final PublishCourtListProduced publishCourtListProduced = event.payload();
        final CourtListPublishStatus publishProduced = new CourtListPublishStatus(
                randomUUID(), publishCourtListProduced.getCourtCentreId(),
                publishCourtListProduced.getPublishCourtListType(), COURT_LIST_PRODUCED, publishCourtListProduced.getProducedTime(),
                publishCourtListProduced.getCourtListFileId(), publishCourtListProduced.getCourtListFileName(), "",
                parse(publishCourtListProduced.getPublishDate()),
                publishCourtListProduced.getWeekCommencing().orElse(false)
        );
        courtListRepository.save(publishProduced);
    }

    @Handles("listing.event.publish-court-list-export-failed")
    public void courtListPublishExportFailed(final Envelope<PublishCourtListExportFailed> event) {
        final PublishCourtListExportFailed publishCourtListExportFailed = event.payload();

        final CourtListPublishStatus exportFailed = new CourtListPublishStatus(
                randomUUID(), publishCourtListExportFailed.getCourtCentreId(),
                publishCourtListExportFailed.getPublishCourtListType(), EXPORT_FAILED, publishCourtListExportFailed.getFailedTime(),
                publishCourtListExportFailed.getCourtListFileId(), publishCourtListExportFailed.getCourtListFileName(),
                publishCourtListExportFailed.getErrorMessage(),
                parse(publishCourtListExportFailed.getPublishDate()),
                publishCourtListExportFailed.getWeekCommencing().orElse(false)
        );
        exportFailed.setErrorMessage(publishCourtListExportFailed.getErrorMessage());
        courtListRepository.save(exportFailed);
    }

    @Handles("listing.event.publish-court-list-export-successful")
    public void courtListPublishExportSuccessful(final Envelope<PublishCourtListExportSuccessful> event) {
        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = event.payload();
        final CourtListPublishStatus exportSuccessful = new CourtListPublishStatus(
                randomUUID(), publishCourtListExportSuccessful.getCourtCentreId(),
                publishCourtListExportSuccessful.getPublishCourtListType(), EXPORT_SUCCESSFUL, publishCourtListExportSuccessful.getPublishedTime(),
                publishCourtListExportSuccessful.getCourtListFileId(), publishCourtListExportSuccessful.getCourtListFileName(), "",
                parse(publishCourtListExportSuccessful.getPublishDate()),
                publishCourtListExportSuccessful.getWeekCommencing().orElse(false)
        );
        courtListRepository.save(exportSuccessful);
    }
}