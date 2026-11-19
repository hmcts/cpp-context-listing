package uk.gov.moj.cpp.listing.command.courtlist;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.event.PublishCourtListType.WARN;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_FAILED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Ports {@code PublishCourtListEventListenerTest}: the rows each would-be private event wrote. */
class CourtListViewstoreTest {

    private static final ZonedDateTime TIME = ZonedDateTime.parse("2026-11-19T10:15:30.123Z");
    private static final LocalDate START = LocalDate.of(2026, 11, 23);

    private final CourtListPublishStatusJdbcRepository statuses = mock(CourtListPublishStatusJdbcRepository.class);
    private final PublishedCourtListRepository publishedCourtLists = mock(PublishedCourtListRepository.class);
    private final PublishCourtListRequestRepository requests = mock(PublishCourtListRequestRepository.class);
    private final CourtListViewstore viewstore = new CourtListViewstore(statuses, publishedCourtLists, requests);
    private final UUID courtCentreId = randomUUID();

    @Test
    void requestedWritesACourtListRequestedRowWeekCommencingWhenTheDatesDiffer() {
        viewstore.publishCourtListRequested("2026-11-23", "2026-11-27", courtCentreId, WARN, TIME);
        viewstore.publishCourtListRequested("2026-11-23", "2026-11-23", courtCentreId, FINAL, TIME);

        final ArgumentCaptor<CourtListPublishStatus> saved = ArgumentCaptor.forClass(CourtListPublishStatus.class);
        verify(statuses, org.mockito.Mockito.times(2)).save(saved.capture());
        final CourtListPublishStatus week = saved.getAllValues().get(0);
        assertThat(week.getPublishStatus()).isEqualTo(COURT_LIST_REQUESTED);
        assertThat(week.getCourtCentreId()).isEqualTo(courtCentreId);
        assertThat(week.getPublishCourtListType()).isEqualTo(WARN);
        assertThat(week.getLastUpdated()).isEqualTo(TIME);
        assertThat(week.getPublishDate()).isEqualTo(START);
        assertThat(week.isWeekCommencing()).isTrue();
        assertThat(week.getPublishCourtListStatusId()).isNotNull();
        assertThat(saved.getAllValues().get(1).isWeekCommencing()).isFalse();
    }

    @Test
    void producedWritesTheFileAndThePublishDateOfTheProducedTime() {
        final UUID fileId = randomUUID();

        viewstore.publishCourtListProduced(courtCentreId, WARN, TIME, fileId, "WarnedList_1.xml", true);

        final ArgumentCaptor<CourtListPublishStatus> saved = ArgumentCaptor.forClass(CourtListPublishStatus.class);
        verify(statuses).save(saved.capture());
        assertThat(saved.getValue().getPublishStatus()).isEqualTo(COURT_LIST_PRODUCED);
        assertThat(saved.getValue().getCourtListFileId()).isEqualTo(fileId);
        assertThat(saved.getValue().getCourtListFileName()).isEqualTo("WarnedList_1.xml");
        assertThat(saved.getValue().getErrorMessage()).isEmpty();
        assertThat(saved.getValue().getPublishDate()).isEqualTo(TIME.toLocalDate());
        assertThat(saved.getValue().isWeekCommencing()).isTrue();
    }

    @Test
    void storedSavesTheParsedJsonUnderTheCompositeKeyWithoutAnExportOutcome() throws Exception {
        final UUID courtListId = randomUUID();

        viewstore.publishedCourtListStored(courtCentreId, FINAL, START, "{\"courtLists\":[]}", TIME, courtListId);

        final ArgumentCaptor<PublishedCourtList> saved = ArgumentCaptor.forClass(PublishedCourtList.class);
        verify(publishedCourtLists).save(saved.capture());
        assertThat(saved.getValue().getCourtCentreId()).isEqualTo(courtCentreId);
        assertThat(saved.getValue().getPublishCourtListType()).isEqualTo(FINAL);
        assertThat(saved.getValue().getStartDate()).isEqualTo(START);
        assertThat(saved.getValue().getCourtListJson()).isEqualTo(new ObjectMapper().readTree("{\"courtLists\":[]}"));
        assertThat(saved.getValue().getLastUpdated()).isEqualTo(TIME);
        assertThat(saved.getValue().getLastExported()).isNull();
        assertThat(saved.getValue().getCourtListId()).isEqualTo(courtListId);
    }

    @Test
    void exportSuccessfulSetsLastExportedAndWritesAnExportSuccessfulRowWithTheFileName() {
        final PublishedCourtList stored = new PublishedCourtList();
        when(publishedCourtLists.findBy(new PublishedCourtListPrimaryKey(courtCentreId, FINAL, START))).thenReturn(stored);

        viewstore.publishCourtListExportSuccessful(courtCentreId, FINAL, START, START, TIME, "DailyList_1.xml");

        assertThat(stored.getLastExported()).isEqualTo(TIME);
        verify(publishedCourtLists).save(stored);
        final ArgumentCaptor<CourtListPublishStatus> saved = ArgumentCaptor.forClass(CourtListPublishStatus.class);
        verify(statuses).save(saved.capture());
        assertThat(saved.getValue().getPublishStatus()).isEqualTo(EXPORT_SUCCESSFUL);
        assertThat(saved.getValue().getCourtListFileName()).isEqualTo("DailyList_1.xml");
        assertThat(saved.getValue().isWeekCommencing()).isFalse();
    }

    @Test
    void exportFailedSetsLastFailedAndTheErrorAndWritesAnExportFailedRow() {
        final PublishedCourtList stored = new PublishedCourtList();
        when(publishedCourtLists.findBy(new PublishedCourtListPrimaryKey(courtCentreId, WARN, START))).thenReturn(stored);

        viewstore.publishCourtListExportFailed(courtCentreId, WARN, START, START.plusDays(4), TIME, "ExportFailedException: boom");

        assertThat(stored.getLastFailed()).isEqualTo(TIME);
        assertThat(stored.getErrorMessage()).isEqualTo("ExportFailedException: boom");
        final ArgumentCaptor<CourtListPublishStatus> saved = ArgumentCaptor.forClass(CourtListPublishStatus.class);
        verify(statuses).save(saved.capture());
        assertThat(saved.getValue().getPublishStatus()).isEqualTo(EXPORT_FAILED);
        assertThat(saved.getValue().getErrorMessage()).isEqualTo("ExportFailedException: boom");
        assertThat(saved.getValue().getCourtListFileName()).isNull();
        assertThat(saved.getValue().isWeekCommencing()).isTrue();
    }

    @Test
    void anExportOutcomeForAListThatWasNeverStoredFailsAsTheJeeListenerDid() {
        assertThatThrownBy(() -> viewstore.publishCourtListExportSuccessful(courtCentreId, FINAL, START, START, TIME, "x.xml"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> viewstore.publishCourtListExportFailed(courtCentreId, FINAL, START, START, TIME, "e"))
                .isInstanceOf(IllegalStateException.class);
        verify(statuses, never()).save(any());
    }

    @Test
    void theRequestsWeekCommencingIsFalseForAnUnknownRequest() {
        final UUID known = randomUUID();
        when(requests.findById(known)).thenReturn(Optional.of(new PublishCourtListRequest(known, courtCentreId, WARN, START, START.plusDays(4), true, TIME)));

        assertThat(viewstore.requestWeekCommencing(known)).isTrue();
        assertThat(viewstore.requestWeekCommencing(randomUUID())).isFalse();
    }
}
