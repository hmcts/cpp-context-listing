package uk.gov.moj.cpp.listing.event.listener;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.listing.event.PublishedCourtListStored;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatus;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;

import java.io.IOException;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

@ServiceComponent(EVENT_LISTENER)
public class PublishCourtListEventListener {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private CourtListPublishStatusJdbcRepository courtListRepository;

    @Inject
    private PublishedCourtListRepository publishedCourtListRepository;

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
                publishCourtListProduced.getProducedTime().toLocalDate(),
                publishCourtListProduced.getWeekCommencing().orElse(false)
        );
        courtListRepository.save(publishProduced);
    }

    @Handles("listing.event.published-court-list-stored")
    public void storePublishedCourtCentreList(final Envelope<PublishedCourtListStored> event) throws IOException {

        final PublishedCourtListStored publishedCourtListStored = event.payload();

        final PublishedCourtList proposedPublishedCourtList =
                new PublishedCourtList(
                        publishedCourtListStored.getCourtCentreId(),
                        publishedCourtListStored.getPublishCourtListType(),
                        publishedCourtListStored.getStartDate(),
                        new ObjectMapper().readTree(publishedCourtListStored.getCourtListJson()),
                        publishedCourtListStored.getLastUpdated(),
                        null,
                        publishedCourtListStored.getCourtListId()
                );

        publishedCourtListRepository.save(proposedPublishedCourtList);
    }

    @Handles("listing.event.publish-court-list-export-successful")
    public void courtListPublishExportSuccessful(final Envelope<PublishCourtListExportSuccessful> event) {
        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = event.payload();

        final PublishedCourtListPrimaryKey pk = new PublishedCourtListPrimaryKey(
                publishCourtListExportSuccessful.getCourtCentreId(),
                publishCourtListExportSuccessful.getPublishCourtListType(),
                publishCourtListExportSuccessful.getStartDate());

        final PublishedCourtList publishedCourtList = publishedCourtListRepository.findBy(pk);

        publishedCourtList.setLastExported(publishCourtListExportSuccessful.getExportedTime());

        publishedCourtListRepository.save(publishedCourtList);
    }

    @Handles("listing.event.publish-court-list-export-failed")
    public void courtListPublishExportFailed(final Envelope<PublishCourtListExportFailed> event) {
        final PublishCourtListExportFailed publishCourtListExportFailed = event.payload();

        final PublishedCourtListPrimaryKey pk = new PublishedCourtListPrimaryKey(
                publishCourtListExportFailed.getCourtCentreId(),
                publishCourtListExportFailed.getPublishCourtListType(),
                publishCourtListExportFailed.getStartDate());

        final PublishedCourtList publishedCourtList = publishedCourtListRepository.findBy(pk);

        publishedCourtList.setLastFailed(publishCourtListExportFailed.getFailedTime());
        publishedCourtList.setErrorMessage(publishCourtListExportFailed.getErrorMessage());

        publishedCourtListRepository.save(publishedCourtList);
    }
}
