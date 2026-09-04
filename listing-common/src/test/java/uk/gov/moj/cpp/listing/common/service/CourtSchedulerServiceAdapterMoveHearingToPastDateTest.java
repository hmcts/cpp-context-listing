package uk.gov.moj.cpp.listing.common.service;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtSchedulerServiceAdapterMoveHearingToPastDateTest {

    // Absolute UTC instants sent as the move request's startTime/endTime (no separate date/local-time).
    // courtscheduler derives the per-day times and the duration from these, so the adapter no longer
    // sends a durationInMinutes.
    private static final String START_TIME = "2026-05-01T09:00:00.000Z";
    private static final String DAY_1_TIME = "2026-07-01T09:00:00.000Z";
    private static final String DAY_2_TIME = "2026-07-02T17:00:00.000Z";

    @InjectMocks
    private CourtSchedulerServiceAdapter adapter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    private static JsonObjectBuilder slot(final String courtScheduleId, final String sessionDate) {
        return createObjectBuilder()
                .add("courtScheduleId", courtScheduleId)
                .add("courtRoomId", "9d324f4f-6c3b-451f-ac1e-f459db781153")
                .add("sessionDate", sessionDate)
                .add("sessionStartTime", sessionDate + "T09:00:00Z")
                .add("sessionEndTime", sessionDate + "T12:00:00Z")
                .add("durationInMinutes", 30)
                .add("source", "MOVE_TO_PAST_DATE");
    }

    @Test
    void shouldParseSingleBookedSlotOn200() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();

        final JsonObject body = createObjectBuilder()
                .add("bookedSlots", createArrayBuilder().add(slot(courtScheduleId.toString(), "2026-05-01")))
                .build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        final List<MoveHearingToPastDateResult> result =
                adapter.moveHearingToPastDate(hearingId, courtCentreId, UUID.randomUUID(), START_TIME, START_TIME);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).courtScheduleId(), is(courtScheduleId));
        assertThat(result.get(0).courtRoomId(), is("9d324f4f-6c3b-451f-ac1e-f459db781153"));
        assertThat(result.get(0).sessionDate(), is(LocalDate.parse("2026-05-01")));
        assertThat(result.get(0).sessionStartTime(), is("2026-05-01T09:00:00Z"));
        assertThat(result.get(0).durationInMinutes(), is(30));
    }

    @Test
    void shouldParseMultipleBookedSlotsForAMultiDayMove() {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject body = createObjectBuilder()
                .add("bookedSlots", createArrayBuilder()
                        .add(slot(UUID.randomUUID().toString(), "2026-07-01"))
                        .add(slot(UUID.randomUUID().toString(), "2026-07-02")))
                .build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        final List<MoveHearingToPastDateResult> result = adapter.moveHearingToPastDate(
                hearingId, UUID.randomUUID(), UUID.randomUUID(), DAY_1_TIME, DAY_2_TIME);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).sessionDate(), is(LocalDate.parse("2026-07-01")));
        assertThat(result.get(1).sessionDate(), is(LocalDate.parse("2026-07-02")));
    }

    @Test
    void shouldParseSlotWithNoDurationInResponse() {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject body = createObjectBuilder()
                .add("bookedSlots", createArrayBuilder().add(createObjectBuilder()
                        .add("courtScheduleId", UUID.randomUUID().toString())
                        .add("sessionDate", "2026-05-01")))
                .build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        final List<MoveHearingToPastDateResult> result = adapter.moveHearingToPastDate(
                hearingId, UUID.randomUUID(), UUID.randomUUID(), START_TIME, START_TIME);

        assertThat(result.get(0).durationInMinutes(), is(nullValue()));
    }

    @Test
    void shouldReplaceCourtschedulerDiagnosticMessageWithFixedCopyOn422NoSessionFound() {
        // courtscheduler propagates its internal diagnostic; the adapter must replace it with the
        // fixed user-facing copy so the UI always sees one message for NO_SESSION_FOUND
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_SESSION_FOUND")
                .add("message", "No session available at courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26 on 2026-06-15")
                .build();
        when(response.getStatus()).thenReturn(422);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(any(), any())).thenReturn(response);

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        START_TIME, START_TIME));

        assertThat(ex.getHttpStatus(), is(422));
        assertThat(ex.getErrorCode(), is("NO_SESSION_FOUND"));
        assertThat(ex.getResponseBody().getString("message"), is(CourtSchedulerServiceAdapter.NO_SESSION_FOUND_MESSAGE));
    }

    @Test
    void shouldNormaliseLegacy404ToA422NoSessionFound() {
        final JsonObject body = createObjectBuilder().build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(any(), any())).thenReturn(response);

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        START_TIME, START_TIME));

        assertThat(ex.getHttpStatus(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(ex.getErrorCode(), is("NO_SESSION_FOUND"));
        assertThat(ex.getResponseBody().getString("message"), is(CourtSchedulerServiceAdapter.NO_SESSION_FOUND_MESSAGE));
    }

    @Test
    void shouldSendNewFieldsOmitHearingIdAndOmitDurationInRequestBody() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final JsonObject body = createObjectBuilder()
                .add("bookedSlots", createArrayBuilder().add(slot(UUID.randomUUID().toString(), "2026-07-01")))
                .build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        adapter.moveHearingToPastDate(hearingId, courtCentreId, courtRoomId, DAY_1_TIME, DAY_2_TIME);

        final ArgumentCaptor<JsonObject> requestCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(hearingSlotsService).moveHearingToPastDate(eq(hearingId), requestCaptor.capture());
        final JsonObject request = requestCaptor.getValue();
        assertThat(request.containsKey("hearingId"), is(false));
        assertThat(request.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(request.getString("courtRoomId"), is(courtRoomId.toString()));
        assertThat(request.getString("jurisdiction"), is("MAGISTRATES"));
        assertThat(request.getString("startTime"), is(DAY_1_TIME));
        assertThat(request.getString("endTime"), is(DAY_2_TIME));
        assertThat(request.containsKey("startDate"), is(false));
        assertThat(request.containsKey("hearingStartTime"), is(false));
        // courtscheduler derives the duration itself, so the adapter must not send one
        assertThat(request.containsKey("durationInMinutes"), is(false));
    }
}
