package uk.gov.moj.cpp.listing.event.listener;

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
import uk.gov.moj.cpp.listing.persistence.repository.CourtList;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListPK;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class PublishCourtListEventListener {

    @Inject
    private CourtListRepository courtListRepository;

    @Handles("listing.event.publish-court-list-requested")
    public void courtListPublishRequested(final Envelope<PublishCourtListRequested> event) {
        final PublishCourtListRequested publishCourtListRequested = event.payload();
        final CourtListPK courtListPK = new CourtListPK(publishCourtListRequested.getCourtCentreId(),
                publishCourtListRequested.getPublishCourtListType());
        courtListRepository.save(new CourtList(courtListPK, COURT_LIST_REQUESTED, publishCourtListRequested.getRequestedTime()));
    }

    @Handles("listing.event.publish-court-list-produced")
    public void courtListPublishProduced(final Envelope<PublishCourtListProduced> event) {
        final PublishCourtListProduced publishCourtListProduced = event.payload();
        final CourtListPK courtListPK = new CourtListPK(publishCourtListProduced.getCourtCentreId(),
                publishCourtListProduced.getPublishCourtListType());
        courtListRepository.save(new CourtList(courtListPK, COURT_LIST_PRODUCED,
                publishCourtListProduced.getCourtListFileId(),
                publishCourtListProduced.getCourtListFileName(),
                publishCourtListProduced.getProducedTime()));
    }

    @Handles("listing.event.publish-court-list-export-failed")
    public void courtListPublishExportFailed(final Envelope<PublishCourtListExportFailed> event) {
        final PublishCourtListExportFailed publishCourtListExportFailed = event.payload();
        final CourtListPK courtListPK = new CourtListPK(publishCourtListExportFailed.getCourtCentreId(),
                publishCourtListExportFailed.getPublishCourtListType());

        final CourtList courtList = new CourtList(courtListPK, EXPORT_FAILED,
                publishCourtListExportFailed.getCourtListFileId(),
                publishCourtListExportFailed.getCourtListFileName(),
                publishCourtListExportFailed.getFailedTime());
        courtList.setErrorMessage(publishCourtListExportFailed.getErrorMessage());
        courtListRepository.save(courtList);
    }

    @Handles("listing.event.publish-court-list-export-successful")
    public void courtListPublishExportSuccessful(final Envelope<PublishCourtListExportSuccessful> event) {
        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = event.payload();
        final CourtListPK courtListPK = new CourtListPK(publishCourtListExportSuccessful.getCourtCentreId(),
                publishCourtListExportSuccessful.getPublishCourtListType());
        courtListRepository.save(new CourtList(courtListPK, EXPORT_SUCCESSFUL,
                publishCourtListExportSuccessful.getCourtListFileId(),
                publishCourtListExportSuccessful.getCourtListFileName(),
                publishCourtListExportSuccessful.getPublishedTime()));
    }
}