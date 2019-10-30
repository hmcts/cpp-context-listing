package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.CourtList;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListRepository;
import uk.gov.moj.cpp.listing.persistence.repository.Status;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class PublishCourtListEventListener {

    @Inject
    private UtcClock clock;

    @Inject
    private CourtListRepository courtListRepository;

    @Handles("listing.event.publish-court-list-export-failed")
    public void courtListPublishExportFailed(final Envelope<PublishCourtListExportFailed> event) {
        final PublishCourtListExportFailed publishCourtListExportFailed = event.payload();
        final CourtList courtList = courtListRepository.findBy(publishCourtListExportFailed.getCourtHouseId());
        courtList.setErrorMessage(publishCourtListExportFailed.getErrorMessage());
        courtList.setDateActioned(publishCourtListExportFailed.getFailedTime());
        courtList.setStatus(Status.EXPORT_FAILED);
        courtListRepository.save(courtList);
    }

    @Handles("listing.event.publish-court-list-export-successful")
    public void courtListPublishExportSuccessful(final Envelope<PublishCourtListExportSuccessful> event) {
        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = event.payload();
        final CourtList courtList = courtListRepository.findBy(publishCourtListExportSuccessful.getCourtHouseId());
        courtList.setDateActioned(publishCourtListExportSuccessful.getPublishedTime());
        courtList.setStatus(Status.EXPORT_SUCCESSFUL);
        courtListRepository.save(courtList);
    }
}