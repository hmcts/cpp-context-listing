package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.event.PublishCourtListExportFailed.publishCourtListExportFailed;
import static uk.gov.justice.listing.event.PublishCourtListExportSuccessful.publishCourtListExportSuccessful;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.moj.cpp.listing.persistence.repository.Status.EXPORT_FAILED;
import static uk.gov.moj.cpp.listing.persistence.repository.Status.EXPORT_SUCCESSFUL;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
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

    private static final UUID COURT_HOUSE_ID = randomUUID();

    @Mock
    private UtcClock clock;

    @Mock
    private CourtListRepository courtListRepository;

    @InjectMocks
    private PublishCourtListEventListener publishCourtListEventListener;

    private final ArgumentCaptor<CourtList> notificationArgumentCaptor = ArgumentCaptor.forClass(CourtList.class);

    @Test
    public void shouldRecordCourtListPublishExportFailed() {
        final ZonedDateTime failedTimeStamp = new UtcClock().now();
        final String documentName = "Document Name";
        final UUID documentId = randomUUID();
        final String errorMessage = "some error";
        final DocumentType documentType = DocumentType.FINAL;

        final PublishCourtListExportFailed publishCourtListExportFailed = publishCourtListExportFailed()
                .withCourtHouseId(COURT_HOUSE_ID)
                .withDocumentId(documentId)
                .withDocumentName(documentName)
                .withFailedTime(failedTimeStamp)
                .withErrorMessage(errorMessage)
                .build();

        final Envelope<PublishCourtListExportFailed> publishCourtListExportFailedEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportFailed);

        final CourtList courtList = new CourtList(COURT_HOUSE_ID, EXPORT_FAILED, documentId, documentName, documentType, failedTimeStamp);
        courtList.setErrorMessage(errorMessage);
        when(courtListRepository.findBy(COURT_HOUSE_ID)).thenReturn(courtList);

        publishCourtListEventListener.courtListPublishExportFailed(publishCourtListExportFailedEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtList courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getErrorMessage(), is(errorMessage));
        assertThat(courtListArg.getCourtHouseId(), is(COURT_HOUSE_ID));
        assertThat(courtListArg.getDocumentId(), is(documentId));
        assertThat(courtListArg.getStatus(), is(EXPORT_FAILED));
        assertThat(courtListArg.getDateActioned(), is(failedTimeStamp));
        assertThat(courtListArg.getDocumentType(), is(documentType));
        assertThat(courtListArg.getDocumentName(), is(documentName));
    }

    @Test
    public void shouldRecordCourtListPublishExportSuccess() {
        final ZonedDateTime publishedTime = new UtcClock().now();
        final String documentName = "Document Name";
        final UUID documentId = randomUUID();
        final String errorMessage = "some error";
        final DocumentType documentType = DocumentType.FINAL;

        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = publishCourtListExportSuccessful()
                .withCourtHouseId(COURT_HOUSE_ID)
                .withDocumentId(documentId)
                .withDocumentName(documentName)
                .withPublishedTime(publishedTime)
                .build();

        final Envelope<PublishCourtListExportSuccessful> publishCourtListExportSuccessfulEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportSuccessful);

        final CourtList courtList = new CourtList(COURT_HOUSE_ID, EXPORT_SUCCESSFUL, documentId, documentName, documentType, publishedTime);
        when(courtListRepository.findBy(COURT_HOUSE_ID)).thenReturn(courtList);

        publishCourtListEventListener.courtListPublishExportSuccessful(publishCourtListExportSuccessfulEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtList courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtHouseId(), is(COURT_HOUSE_ID));
        assertThat(courtListArg.getDocumentId(), is(documentId));
        assertThat(courtListArg.getStatus(), is(EXPORT_SUCCESSFUL));
        assertThat(courtListArg.getDateActioned(), is(publishedTime));
        assertThat(courtListArg.getDocumentType(), is(documentType));
        assertThat(courtListArg.getDocumentName(), is(documentName));
    }
}