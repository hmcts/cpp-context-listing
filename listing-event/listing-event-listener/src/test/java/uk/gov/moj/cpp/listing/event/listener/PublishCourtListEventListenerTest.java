package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.event.PublishCourtListExportFailed.publishCourtListExportFailed;
import static uk.gov.justice.listing.event.PublishCourtListExportSuccessful.publishCourtListExportSuccessful;
import static uk.gov.justice.listing.event.PublishCourtListProduced.publishCourtListProduced;
import static uk.gov.justice.listing.event.PublishCourtListRequested.publishCourtListRequested;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_FAILED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListPublishStatus;

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
    private CourtListPublishStatusJdbcRepository courtListRepository;

    @InjectMocks
    private PublishCourtListEventListener publishCourtListEventListener;

    private final ArgumentCaptor<CourtListPublishStatus> notificationArgumentCaptor = ArgumentCaptor.forClass(CourtListPublishStatus.class);

    @Test
    public void shouldRecordPublishCourtListRequestedForFixedDate() {
        final ZonedDateTime requestedTime = new UtcClock().now();
        final PublishCourtListType courtListType = FINAL;

        final PublishCourtListRequested publishCourtListRequested = publishCourtListRequested()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withPublishCourtListType(courtListType)
                .withRequestedTime(requestedTime)
                .withStartDate("2018-11-20")
                .withEndDate("2018-11-20")
                .build();

        final Envelope<PublishCourtListRequested> publishCourtListRequestedEnvelope =
                envelopeFrom(metadataWithDefaults(), publishCourtListRequested);

        publishCourtListEventListener.courtListPublishRequested(publishCourtListRequestedEnvelope);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtCentreId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getPublishCourtListType(), is(courtListType));
        assertThat(courtListArg.getPublishStatus(), is(COURT_LIST_REQUESTED));
        assertThat(courtListArg.getLastUpdated(), is(requestedTime));
        assertThat(courtListArg.isWeekCommencing(), is(false));

    }

    @Test
    public void shouldRecordPublishCourtListRequestedForWeekCommencingDate() {
        final ZonedDateTime requestedTime = new UtcClock().now();
        final PublishCourtListType courtListType = FINAL;

        final PublishCourtListRequested publishCourtListRequested = publishCourtListRequested()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withPublishCourtListType(courtListType)
                .withRequestedTime(requestedTime)
                .withStartDate("2018-11-20")
                .withEndDate("2018-11-22")
                .build();

        final Envelope<PublishCourtListRequested> publishCourtListRequestedEnvelope =
                envelopeFrom(metadataWithDefaults(), publishCourtListRequested);

        publishCourtListEventListener.courtListPublishRequested(publishCourtListRequestedEnvelope);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtCentreId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getPublishCourtListType(), is(courtListType));
        assertThat(courtListArg.getPublishStatus(), is(COURT_LIST_REQUESTED));
        assertThat(courtListArg.getLastUpdated(), is(requestedTime));
        assertThat(courtListArg.isWeekCommencing(), is(true));
    }

    @Test
    public void shouldRecordPublishCourtListProduced() {
        final ZonedDateTime producedTime = new UtcClock().now();
        final String courtListFileName = "Document Name";
        final UUID courtListFileId = randomUUID();
        final PublishCourtListType courtListType = FINAL;

        final PublishCourtListProduced publishCourtListProduced = publishCourtListProduced()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withPublishCourtListType(courtListType)
                .withProducedTime(producedTime)
                .withWeekCommencing(of(false))
                .build();

        final Envelope<PublishCourtListProduced> publishCourtListProducedEvent = envelopeFrom(metadataWithDefaults(), publishCourtListProduced);

        publishCourtListEventListener.courtListPublishProduced(publishCourtListProducedEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtCentreId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getPublishCourtListType(), is(courtListType));
        assertThat(courtListArg.getCourtListFileId(), is(courtListFileId));
        assertThat(courtListArg.getPublishStatus(), is(COURT_LIST_PRODUCED));
        assertThat(courtListArg.getLastUpdated(), is(producedTime));
        assertThat(courtListArg.getCourtListFileName(), is(courtListFileName));
    }

    @Test
    public void shouldRecordPublishCourtListExportFailed() {
        final ZonedDateTime failedTimeStamp = new UtcClock().now();
        final String errorMessage = "some error";
        final String courtListFileName = "Document Name";
        final UUID courtListFileId = randomUUID();
        final PublishCourtListType courtListType = FINAL;

        final PublishCourtListExportFailed publishCourtListExportFailed = publishCourtListExportFailed()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withPublishCourtListType(courtListType)
                .withFailedTime(failedTimeStamp)
                .withErrorMessage(errorMessage)
                .withWeekCommencing(of(false))
                .build();

        final Envelope<PublishCourtListExportFailed> publishCourtListExportFailedEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportFailed);

        publishCourtListEventListener.courtListPublishExportFailed(publishCourtListExportFailedEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getErrorMessage(), is(errorMessage));
        assertThat(courtListArg.getCourtCentreId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getPublishCourtListType(), is(courtListType));
        assertThat(courtListArg.getCourtListFileId(), is(courtListFileId));
        assertThat(courtListArg.getPublishStatus(), is(EXPORT_FAILED));
        assertThat(courtListArg.getLastUpdated(), is(failedTimeStamp));
        assertThat(courtListArg.getCourtListFileName(), is(courtListFileName));
    }

    @Test
    public void shouldRecordPublishCourtListExportSuccessful() {
        final ZonedDateTime publishedTime = new UtcClock().now();
        final String courtListFileName = "Document Name";
        final UUID courtListFileId = randomUUID();
        final PublishCourtListType courtListType = FINAL;
        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = publishCourtListExportSuccessful()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withPublishCourtListType(courtListType)
                .withPublishedTime(publishedTime)
                .withWeekCommencing(of(false))
                .build();

        final Envelope<PublishCourtListExportSuccessful> publishCourtListExportSuccessfulEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportSuccessful);

        publishCourtListEventListener.courtListPublishExportSuccessful(publishCourtListExportSuccessfulEvent);

        verify(courtListRepository).save(notificationArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = notificationArgumentCaptor.getValue();
        assertThat(courtListArg.getCourtCentreId(), is(COURT_CENTRE_ID));
        assertThat(courtListArg.getPublishCourtListType(), is(courtListType));
        assertThat(courtListArg.getCourtListFileId(), is(courtListFileId));
        assertThat(courtListArg.getPublishStatus(), is(EXPORT_SUCCESSFUL));
        assertThat(courtListArg.getLastUpdated(), is(publishedTime));
        assertThat(courtListArg.getCourtListFileName(), is(courtListFileName));
    }
}
