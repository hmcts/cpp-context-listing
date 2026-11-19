package uk.gov.moj.cpp.listing.command.courtlist;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.event.PublishCourtListType.DRAFT;
import static uk.gov.justice.listing.event.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.event.PublishCourtListType.WARN;

import uk.gov.moj.cpp.listing.command.support.AfterCommit;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishCourtListRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The court-list commands (ADR 015): the {@code ListingCommandHandler} court-list methods and the
 * {@code PublishCourtListRequestAggregate} rule ({@code PublishCourtListRequestAggregateTest},
 * the handler tests), the listener writes they caused and the processor step that followed.
 */
class CourtListCommandsTest {

    /** Thursday 2026-11-19, 10:15:30.123 UTC. */
    private static final Instant NOW = Instant.parse("2026-11-19T10:15:30.123Z");
    private static final ZonedDateTime NOW_UTC = ZonedDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private final CourtListViewstore viewstore = mock(CourtListViewstore.class);
    private final CourtListProcessor processor = mock(CourtListProcessor.class);
    private final CppQueryClient cppQueryClient = mock(CppQueryClient.class);
    /** No transaction in a unit test: AfterCommit runs the step at once, on the calling thread. */
    private final AfterCommit afterCommit = new AfterCommit(Runnable::run);
    private final CourtListCommands commands = new CourtListCommands(viewstore, processor, afterCommit, cppQueryClient,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final Metadata command = Metadata.builder().withId(randomUUID()).withName("listing.command.publish-court-list")
            .withUserId("user-1").build();
    private final UUID courtCentreId = randomUUID();

    @Test
    void publishCourtListRecordsAWeekCommencingRequestWhenTheDatesDiffer() {
        commands.publishCourtList(command, publishPayload("2026-11-23", "2026-11-27", "WARN").add("sendNotificationToParties", true).build());

        final PublishCourtListRequest request = recordedRequest();
        assertThat(request.getPublishCourtListRequestId()).isNotNull();
        assertThat(request.getCourtCentreId()).isEqualTo(courtCentreId);
        assertThat(request.getPublishCourtListType()).isEqualTo(WARN);
        assertThat(request.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(request.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(request.isWeekCommencing()).isTrue();
        assertThat(request.getRequestedTime()).isEqualTo(NOW_UTC);
        verify(viewstore).publishCourtListRequested("2026-11-23", "2026-11-27", courtCentreId, WARN, NOW_UTC);
        verify(processor).onPublishCourtListRequested(command, request, true);
    }

    @Test
    void aSingleDayRequestIsNotWeekCommencingAndAMissingNotificationFlagIsFalse() {
        commands.publishCourtList(command, publishPayload("2026-11-23", "2026-11-23", "DRAFT").build());

        final PublishCourtListRequest request = recordedRequest();
        assertThat(request.isWeekCommencing()).isFalse();
        verify(processor).onPublishCourtListRequested(command, request, false);
    }

    @Test
    void eachPublishIsANewRequest() {
        commands.publishCourtList(command, publishPayload("2026-11-23", "2026-11-23", "DRAFT").build());
        commands.publishCourtList(command, publishPayload("2026-11-23", "2026-11-23", "DRAFT").build());

        final ArgumentCaptor<PublishCourtListRequest> requests = ArgumentCaptor.forClass(PublishCourtListRequest.class);
        verify(viewstore, times(2)).recordRequest(requests.capture());
        assertThat(requests.getAllValues().get(0).getPublishCourtListRequestId())
                .isNotEqualTo(requests.getAllValues().get(1).getPublishCourtListRequestId());
    }

    @Test
    void theRequestedTimeOfThePayloadIsIgnored() {
        commands.publishCourtList(command, publishPayload("2026-11-23", "2026-11-23", "DRAFT").add("requestedTime", "2001-01-01T00:00:00.000Z").build());

        assertThat(recordedRequest().getRequestedTime()).isEqualTo(NOW_UTC);
    }

    @Test
    void crownCourtsGetAFinalListForTheNextWorkingDayEach() {
        final UUID first = randomUUID();
        final UUID second = randomUUID();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.courtrooms"), eq(Json.createObjectBuilder().add("oucodeL1Code", "C").build())))
                .thenReturn(Optional.of(crownCourts(first, second)));

        commands.publishCourtListsForCrownCourts(command, Json.createObjectBuilder().build());

        final ArgumentCaptor<PublishCourtListRequest> requests = ArgumentCaptor.forClass(PublishCourtListRequest.class);
        verify(viewstore, times(2)).recordRequest(requests.capture());
        assertThat(requests.getAllValues()).extracting(PublishCourtListRequest::getCourtCentreId).containsExactly(first, second);
        final LocalDate friday = LocalDate.of(2026, 11, 20);
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.getPublishCourtListType()).isEqualTo(FINAL);
            assertThat(request.getStartDate()).isEqualTo(friday);
            assertThat(request.getEndDate()).isEqualTo(friday);
            assertThat(request.isWeekCommencing()).isFalse();
        });
        verify(processor, times(2)).onPublishCourtListRequested(eq(command), any(), eq(false));
    }

    @Test
    void aFailingCrownCourtIsLoggedAndTheOthersStillGoAhead() {
        final UUID first = randomUUID();
        final UUID second = randomUUID();
        when(cppQueryClient.requestAsAdmin(anyString(), any(JsonObject.class))).thenReturn(Optional.of(crownCourts(first, second)));
        doThrow(new IllegalStateException("db")).when(viewstore).publishCourtListRequested(anyString(), anyString(), eq(first), any(), any());

        commands.publishCourtListsForCrownCourts(command, Json.createObjectBuilder().build());

        verify(viewstore).publishCourtListRequested(anyString(), anyString(), eq(second), any(), any());
        verify(processor, times(1)).onPublishCourtListRequested(eq(command), any(), anyBoolean());
    }

    @Test
    void crownCourtsFailWithoutAReferenceDataResponse() {
        when(cppQueryClient.requestAsAdmin(anyString(), any(JsonObject.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commands.publishCourtListsForCrownCourts(command, Json.createObjectBuilder().build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void producedTakesWeekCommencingFromTheRequest() {
        final UUID requestId = randomUUID();
        final UUID fileId = randomUUID();
        when(viewstore.requestWeekCommencing(requestId)).thenReturn(true);

        commands.recordCourtListProduced(command, Json.createObjectBuilder()
                .add("publishCourtListRequestId", requestId.toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListFileId", fileId.toString())
                .add("courtListFileName", "WarnedList.xml")
                .add("publishCourtListType", "WARN")
                .add("producedTime", "2026-11-19T09:00:00.000Z")
                .add("publishDate", "2026-11-23")
                .build());

        verify(viewstore).publishCourtListProduced(courtCentreId, WARN, ZonedDateTime.parse("2026-11-19T09:00:00.000Z"), fileId, "WarnedList.xml", true);
    }

    @Test
    void producedRejectsAnInvalidPublishDateAsTheJeeHandlerDid() {
        assertThatThrownBy(() -> commands.recordCourtListProduced(command, Json.createObjectBuilder()
                .add("publishCourtListRequestId", randomUUID().toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListFileId", randomUUID().toString())
                .add("courtListFileName", "x.xml")
                .add("publishCourtListType", "WARN")
                .add("producedTime", "2026-11-19T09:00:00.000Z")
                .add("publishDate", "not-a-date")
                .build())).isInstanceOf(RuntimeException.class);
        verify(viewstore, never()).publishCourtListProduced(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void requestExportHandsTheListToTheExportUnderItsDeterministicId() {
        final Metadata exportCommand = Metadata.builder().withId(randomUUID()).withName("listing.command.court-list-request-export").build();

        commands.courtListRequestExport(exportCommand, Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("publishCourtListType", "DRAFT")
                .add("startDate", "2026-11-23")
                .add("courtListJson", "{\"courtLists\":[]}")
                .build());

        final ArgumentCaptor<CourtListExportRequest> request = ArgumentCaptor.forClass(CourtListExportRequest.class);
        verify(processor).onCourtListExportRequested(eq(exportCommand), request.capture());
        assertThat(request.getValue()).isEqualTo(new CourtListExportRequest(courtCentreId,
                CourtListCommands.courtListId(courtCentreId, DRAFT, LocalDate.of(2026, 11, 23)), "{\"courtLists\":[]}", null, DRAFT, NOW_UTC,
                null, LocalDate.of(2026, 11, 23)));
        verify(viewstore, never()).recordRequest(any());
    }

    @Test
    void theCourtListIdIsTheUuidServiceOne() {
        final UUID centre = UUID.fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");

        assertThat(CourtListCommands.courtListId(centre, FINAL, LocalDate.of(2026, 11, 20)))
                .isEqualTo(UUID.nameUUIDFromBytes("b52f805c-2821-4904-a0e0-26f7fda6dd08/FINAL/2026-11-20".getBytes(UTF_8)))
                .isEqualTo(CourtListCommands.courtListId(centre, FINAL, LocalDate.of(2026, 11, 20)))
                .isNotEqualTo(CourtListCommands.courtListId(centre, DRAFT, LocalDate.of(2026, 11, 20)));
    }

    @Test
    void storeSavesTheListUnderItsCourtListIdLastUpdatedNow() {
        commands.storePublishedCourtList(courtCentreId, FINAL, LocalDate.of(2026, 11, 20), "{}");

        verify(viewstore).publishedCourtListStored(courtCentreId, FINAL, LocalDate.of(2026, 11, 20), "{}", NOW_UTC,
                CourtListCommands.courtListId(courtCentreId, FINAL, LocalDate.of(2026, 11, 20)));
    }

    @Test
    void exportOutcomesAreRecorded() {
        final LocalDate start = LocalDate.of(2026, 11, 20);
        commands.recordCourtListExportSuccessful(courtCentreId, FINAL, start, start, "DailyList.xml", NOW_UTC);
        commands.recordCourtListExportFailed(courtCentreId, FINAL, start, start, "boom", NOW_UTC);

        verify(viewstore).publishCourtListExportSuccessful(courtCentreId, FINAL, start, start, NOW_UTC, "DailyList.xml");
        verify(viewstore).publishCourtListExportFailed(courtCentreId, FINAL, start, start, NOW_UTC, "boom");
    }

    private jakarta.json.JsonObjectBuilder publishPayload(final String startDate, final String endDate, final String type) {
        return Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", startDate)
                .add("endDate", endDate)
                .add("publishCourtListType", type);
    }

    private PublishCourtListRequest recordedRequest() {
        final ArgumentCaptor<PublishCourtListRequest> request = ArgumentCaptor.forClass(PublishCourtListRequest.class);
        verify(viewstore).recordRequest(request.capture());
        return request.getValue();
    }

    private static JsonObject crownCourts(final UUID... ids) {
        final var units = Json.createArrayBuilder();
        for (final UUID id : ids) {
            units.add(Json.createObjectBuilder().add("id", id.toString()).add("defaultDurationHrs", "6:30").add("defaultStartTime", "10:00"));
        }
        return Json.createObjectBuilder().add("organisationunits", units).build();
    }
}
