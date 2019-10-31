package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.event.PublishCourtListExportFailed.publishCourtListExportFailed;
import static uk.gov.justice.listing.event.PublishCourtListExportSuccessful.publishCourtListExportSuccessful;
import static uk.gov.justice.listing.event.PublishCourtListProduced.publishCourtListProduced;
import static uk.gov.justice.listing.event.PublishCourtListRequested.publishCourtListRequested;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.moj.cpp.listing.persistence.repository.DocumentType.FINAL;
import static uk.gov.moj.cpp.listing.persistence.repository.Status.COURT_LIST_PRODUCED;
import static uk.gov.moj.cpp.listing.persistence.repository.Status.COURT_LIST_REQUESTED;
import static uk.gov.moj.cpp.listing.persistence.repository.Status.EXPORT_FAILED;
import static uk.gov.moj.cpp.listing.persistence.repository.Status.EXPORT_SUCCESSFUL;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.CourtList;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListRepository;
import uk.gov.moj.cpp.listing.persistence.repository.DocumentType;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PublishCourtListEventListenerTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();

    @Mock
    private UtcClock clock;

    @Mock
    private CourtListRepository courtListRepository;

    @InjectMocks
    private PublishCourtListEventListener publishCourtListEventListener;

    private final ArgumentCaptor<CourtList> notificationArgumentCaptor = ArgumentCaptor.forClass(CourtList.class);

    @Test
    public void shouldRecordPublishCourtListRequested() {
        final ZonedDateTime requestedTime = new UtcClock().now();
        final DocumentType courtListType = FINAL;

        final PublishCourtListRequested publishCourtListRequested = publishCourtListRequested()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtListType(courtListType.toString())
                .withRequestedTime(requestedTime)
                .build();

        final Envelope<PublishCourtListRequested> publishCourtListRequestedEnvelope =
                envelopeFrom(metadataWithDefaults(), publishCourtListRequested);

        publishCourtListEventListener.courtListPublishRequested(publishCourtListRequestedEnvelope);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtList courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtHouseId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getStatus(), is(COURT_LIST_REQUESTED));
        assertThat(courtListArg.getDateActioned(), is(requestedTime));
        assertThat(courtListArg.getDocumentType(), is(courtListType));
    }

    @Test
    public void shouldRecordPublishCourtListProduced() {
        final ZonedDateTime producedTime = new UtcClock().now();
        final String courtListFileName = "Document Name";
        final UUID courtListFileId = randomUUID();
        final DocumentType courtListType = FINAL;

        final PublishCourtListProduced publishCourtListProduced = publishCourtListProduced()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withCourtListType(courtListType.toString())
                .withProducedTime(producedTime)
                .build();

        final Envelope<PublishCourtListProduced> publishCourtListProducedEvent = envelopeFrom(metadataWithDefaults(), publishCourtListProduced);

        publishCourtListEventListener.courtListPublishProduced(publishCourtListProducedEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtList courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtHouseId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getDocumentId(), is(courtListFileId));
        assertThat(courtListArg.getStatus(), is(COURT_LIST_PRODUCED));
        assertThat(courtListArg.getDateActioned(), is(producedTime));
        assertThat(courtListArg.getDocumentType(), is(courtListType));
        assertThat(courtListArg.getDocumentName(), is(courtListFileName));
    }

    @Test
    public void shouldRecordPublishCourtListExportFailed() {
        final ZonedDateTime failedTimeStamp = new UtcClock().now();
        final String errorMessage = "some error";
        final String courtListFileName = "Document Name";
        final UUID courtListFileId = randomUUID();
        final DocumentType courtListType = FINAL;

        final PublishCourtListExportFailed publishCourtListExportFailed = publishCourtListExportFailed()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withCourtListType(courtListType.toString())
                .withFailedTime(failedTimeStamp)
                .withErrorMessage(errorMessage)
                .build();

        final Envelope<PublishCourtListExportFailed> publishCourtListExportFailedEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportFailed);

        publishCourtListEventListener.courtListPublishExportFailed(publishCourtListExportFailedEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtList courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getErrorMessage(), is(errorMessage));
        assertThat(courtListArg.getCourtHouseId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getDocumentId(), is(courtListFileId));
        assertThat(courtListArg.getStatus(), is(EXPORT_FAILED));
        assertThat(courtListArg.getDateActioned(), is(failedTimeStamp));
        assertThat(courtListArg.getDocumentType(), is(courtListType));
        assertThat(courtListArg.getDocumentName(), is(courtListFileName));
    }

    @Test
    public void shouldRecordPublishCourtListExportSuccessful() {
        final ZonedDateTime publishedTime = new UtcClock().now();
        final String courtListFileName = "Document Name";
        final UUID courtListFileId = randomUUID();
        final DocumentType courtListType = FINAL;
        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = publishCourtListExportSuccessful()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withCourtListType(courtListType.toString())
                .withPublishedTime(publishedTime)
                .build();

        final Envelope<PublishCourtListExportSuccessful> publishCourtListExportSuccessfulEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportSuccessful);

        publishCourtListEventListener.courtListPublishExportSuccessful(publishCourtListExportSuccessfulEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtList courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtHouseId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getDocumentId(), is(courtListFileId));
        assertThat(courtListArg.getStatus(), is(EXPORT_SUCCESSFUL));
        assertThat(courtListArg.getDateActioned(), is(publishedTime));
        assertThat(courtListArg.getDocumentType(), is(courtListType));
        assertThat(courtListArg.getDocumentName(), is(courtListFileName));
    }
}