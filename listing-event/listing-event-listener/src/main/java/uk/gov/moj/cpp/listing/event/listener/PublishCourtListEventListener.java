package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.listing.persistence.repository.CourtListType.valueOf;
import static uk.gov.moj.cpp.listing.persistence.repository.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.moj.cpp.listing.persistence.repository.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.moj.cpp.listing.persistence.repository.PublishStatus.EXPORT_FAILED;
import static uk.gov.moj.cpp.listing.persistence.repository.PublishStatus.EXPORT_SUCCESSFUL;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.CourtList;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class PublishCourtListEventListener {


    @Inject
    private CourtListRepository courtListRepository;

    @Handles("listing.event.publish-court-list-requested")
    public void courtListPublishRequested(final Envelope<PublishCourtListRequested> event) {
        final PublishCourtListRequested publishCourtListRequested = event.payload();
        final CourtList courtList = new CourtList(
                publishCourtListRequested.getCourtCentreId(),
                COURT_LIST_REQUESTED,
                valueOf(publishCourtListRequested.getCourtListType()),
                publishCourtListRequested.getRequestedTime());
        courtListRepository.save(courtList);
    }

    @Handles("listing.event.publish-court-list-produced")
    public void courtListPublishProduced(final Envelope<PublishCourtListProduced> event) {
        final PublishCourtListProduced publishCourtListProduced = event.payload();
        final CourtList courtList = new CourtList(
                publishCourtListProduced.getCourtCentreId(),
                COURT_LIST_PRODUCED,
                publishCourtListProduced.getCourtListFileId(),
                publishCourtListProduced.getCourtListFileName(),
                valueOf(publishCourtListProduced.getCourtListType()),
                publishCourtListProduced.getProducedTime());
        courtListRepository.save(courtList);
    }

    @Handles("listing.event.publish-court-list-export-failed")
    public void courtListPublishExportFailed(final Envelope<PublishCourtListExportFailed> event) {
        final PublishCourtListExportFailed publishCourtListExportFailed = event.payload();
        final CourtList courtList = new CourtList(
                publishCourtListExportFailed.getCourtCentreId(),
                EXPORT_FAILED,
                publishCourtListExportFailed.getCourtListFileId(),
                publishCourtListExportFailed.getCourtListFileName(),
                valueOf(publishCourtListExportFailed.getCourtListType()),
                publishCourtListExportFailed.getFailedTime());
        courtList.setErrorMessage(publishCourtListExportFailed.getErrorMessage());
        courtListRepository.save(courtList);
    }

    @Handles("listing.event.publish-court-list-export-successful")
    public void courtListPublishExportSuccessful(final Envelope<PublishCourtListExportSuccessful> event) {
        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = event.payload();
        final CourtList courtList = new CourtList(
                publishCourtListExportSuccessful.getCourtCentreId(),
                EXPORT_SUCCESSFUL,
                publishCourtListExportSuccessful.getCourtListFileId(),
                publishCourtListExportSuccessful.getCourtListFileName(),
                valueOf(publishCourtListExportSuccessful.getCourtListType()),
                publishCourtListExportSuccessful.getPublishedTime());
        courtListRepository.save(courtList);
    }
}