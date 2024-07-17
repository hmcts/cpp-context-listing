package uk.gov.moj.cpp.listing.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.event.PublishCourtListExportFailed.publishCourtListExportFailed;
import static uk.gov.justice.listing.event.PublishCourtListExportSuccessful.publishCourtListExportSuccessful;
import static uk.gov.justice.listing.event.PublishCourtListProduced.publishCourtListProduced;
import static uk.gov.justice.listing.event.PublishCourtListRequested.publishCourtListRequested;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishedCourtListStored;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatus;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Mock
    private PublishedCourtListRepository publishedCourtListRepository;

    @InjectMocks
    private PublishCourtListEventListener publishCourtListEventListener;

    private final ArgumentCaptor<CourtListPublishStatus> courtListPublishStatusArgumentCaptor =
            ArgumentCaptor.forClass(CourtListPublishStatus.class);

    private final ArgumentCaptor<PublishedCourtList> publishedCourtListArgumentCaptor =
            ArgumentCaptor.forClass(PublishedCourtList.class);

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

        verify(courtListRepository).save(courtListPublishStatusArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = courtListPublishStatusArgumentCaptor.getValue();
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

        verify(courtListRepository).save(courtListPublishStatusArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = courtListPublishStatusArgumentCaptor.getValue();
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
                .withWeekCommencing(false)
                .build();

        final Envelope<PublishCourtListProduced> publishCourtListProducedEvent = envelopeFrom(metadataWithDefaults(), publishCourtListProduced);

        publishCourtListEventListener.courtListPublishProduced(publishCourtListProducedEvent);

        verify(courtListRepository).save(courtListPublishStatusArgumentCaptor.capture());

        final CourtListPublishStatus courtListArg = courtListPublishStatusArgumentCaptor.getValue();
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
        final PublishCourtListType courtListType = FINAL;
        final LocalDate startDate = LocalDate.of(2020, Month.MARCH, 30);
        final LocalDate endDate = LocalDate.of(2020, Month.MARCH, 30);
        final UUID courtListId = randomUUID();

        final PublishCourtListExportFailed publishCourtListExportFailed = publishCourtListExportFailed()
                .withCourtListId(courtListId)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withPublishCourtListType(courtListType)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withFailedTime(failedTimeStamp)
                .withErrorMessage(errorMessage)
                .build();

        final Envelope<PublishCourtListExportFailed> publishCourtListExportFailedEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportFailed);

        final PublishedCourtListPrimaryKey pk = new PublishedCourtListPrimaryKey(COURT_CENTRE_ID, courtListType, startDate);
        final PublishedCourtList publishedCourtList = spy(PublishedCourtList.class);

        when(publishedCourtListRepository.findBy(pk)).thenReturn(publishedCourtList);
        when(publishedCourtListRepository.
                save(publishedCourtList)).thenReturn(publishedCourtList);

        publishCourtListEventListener.courtListPublishExportFailed(publishCourtListExportFailedEvent);

        verify(publishedCourtListRepository).save(publishedCourtListArgumentCaptor.capture());
        verify(courtListRepository).save(courtListPublishStatusArgumentCaptor.capture());

        final PublishedCourtList publishedCourtListArg = publishedCourtListArgumentCaptor.getValue();
        assertThat(publishedCourtListArg.getLastFailed(), is(failedTimeStamp));
        assertThat(publishedCourtListArg.getErrorMessage(), is(errorMessage));
    }

    @Test
    public void shouldRecordPublishCourtListExportSuccessful() {
        final ZonedDateTime exportedTime = new UtcClock().now();
        final PublishCourtListType courtListType = FINAL;
        final LocalDate startDate = LocalDate.of(2020, Month.MARCH, 30);
        final LocalDate endDate = LocalDate.of(2020, Month.MARCH, 30);
        final UUID courtListId = randomUUID();

        final PublishCourtListExportSuccessful publishCourtListExportSuccessful = publishCourtListExportSuccessful()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withPublishCourtListType(courtListType)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withExportedTime(exportedTime)
                .withCourtListId(courtListId)
                .withCourtListFileName("TESTFILENAME")
                .build();

        final Envelope<PublishCourtListExportSuccessful> publishCourtListExportSuccessfulEvent = envelopeFrom(metadataWithDefaults(), publishCourtListExportSuccessful);

        final PublishedCourtListPrimaryKey pk = new PublishedCourtListPrimaryKey(COURT_CENTRE_ID, courtListType, startDate);
        final PublishedCourtList publishedCourtList = spy(PublishedCourtList.class);

        when(publishedCourtListRepository.findBy(pk)).thenReturn(publishedCourtList);
        when(publishedCourtListRepository.
                save(publishedCourtList)).thenReturn(publishedCourtList);

        publishCourtListEventListener.courtListPublishExportSuccessful(publishCourtListExportSuccessfulEvent);

        verify(publishedCourtListRepository).save(publishedCourtListArgumentCaptor.capture());
        verify(courtListRepository).save(courtListPublishStatusArgumentCaptor.capture());

        final PublishedCourtList publishedCourtListArg = publishedCourtListArgumentCaptor.getValue();
        assertThat(publishedCourtListArg.getLastExported(), is(exportedTime));
    }

    @Test
    public void shouldStorePublishedCourtList() throws IOException {

        final UUID courtCentreId = COURT_CENTRE_ID;
        final PublishCourtListType publishCourtListType = FINAL;
        final LocalDate startDate = LocalDate.of(2019, Month.DECEMBER, 13);
        final String courtListJson = "{}";
        final ZonedDateTime lastUpdated = now();

        final PublishedCourtListStored publishedCourtListStored = new PublishedCourtListStored.Builder()
                .withCourtCentreId(courtCentreId)
                .withPublishCourtListType(publishCourtListType)
                .withStartDate(startDate)
                .withCourtListJson(courtListJson)
                .withLastUpdated(lastUpdated)
                .build();

        final PublishedCourtList expectedPublishedCourtList = new PublishedCourtList(
                courtCentreId,
                publishCourtListType,
                startDate,
                new ObjectMapper().readTree(courtListJson),
                lastUpdated,
                null,
                null
        );

        when(publishedCourtListRepository.
                save(expectedPublishedCourtList)).thenReturn(expectedPublishedCourtList);

        publishCourtListEventListener.storePublishedCourtCentreList(envelopeFrom(metadataWithDefaults(), publishedCourtListStored));

        verify(publishedCourtListRepository).save(publishedCourtListArgumentCaptor.capture());

        final PublishedCourtList publishedCourtListArg = publishedCourtListArgumentCaptor.getValue();
        assertThat(publishedCourtListArg.getCourtCentreId(), is(courtCentreId));
        assertThat(publishedCourtListArg.getPublishCourtListType(), is(publishCourtListType));
        assertThat(publishedCourtListArg.getStartDate(), is(startDate));
        assertThat(publishedCourtListArg.getCourtListJson().toString(), is(courtListJson));
        assertThat(publishedCourtListArg.getLastUpdated(), is(lastUpdated));
    }
}
