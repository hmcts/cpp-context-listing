package uk.gov.moj.cpp.listing.common.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
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

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

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
    private static final String START_TIME = "2026-05-01T09:00:00.000Z";
    private static final String DAY_1_TIME = "2026-07-01T09:00:00.000Z";
    private static final String DAY_2_TIME = "2026-07-02T09:00:00.000Z";

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
                adapter.moveHearingToPastDate(hearingId, courtCentreId, UUID.randomUUID(), START_TIME, null, 30);

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
                hearingId, UUID.randomUUID(), UUID.randomUUID(), DAY_1_TIME, DAY_2_TIME, 30);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).sessionDate(), is(LocalDate.parse("2026-07-01")));
        assertThat(result.get(1).sessionDate(), is(LocalDate.parse("2026-07-02")));
    }

    @Test
    void shouldOmitDurationInRequestWhenNotSupplied() {
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
                hearingId, UUID.randomUUID(), UUID.randomUUID(), START_TIME, null, null);

        assertThat(result.get(0).durationInMinutes(), is(nullValue()));
    }

    @Test
    void shouldThrowWith422AndErrorCodeWhenFutureDate() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "FUTURE_DATE_NOT_ALLOWED")
                .add("message", "must not be after today")
                .build();
        when(response.getStatus()).thenReturn(422);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(any(), any())).thenReturn(response);

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        "2999-01-01T09:00:00.000Z", null, 30));

        assertThat(ex.getHttpStatus(), is(422));
        assertThat(ex.getErrorCode(), is("FUTURE_DATE_NOT_ALLOWED"));
    }

    @Test
    void shouldThrowWith422NoSessionFoundWhenCourtschedulerReturns422() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_SESSION_FOUND")
                .add("message", "No session available")
                .build();
        when(response.getStatus()).thenReturn(422);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(any(), any())).thenReturn(response);

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        START_TIME, null, 30));

        assertThat(ex.getHttpStatus(), is(422));
        assertThat(ex.getErrorCode(), is("NO_SESSION_FOUND"));
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
                        START_TIME, null, 30));

        assertThat(ex.getHttpStatus(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(ex.getErrorCode(), is("NO_SESSION_FOUND"));
    }

    @Test
    void shouldSendNewFieldsAndOmitHearingIdInRequestBody() {
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

        adapter.moveHearingToPastDate(hearingId, courtCentreId, courtRoomId, DAY_1_TIME, DAY_2_TIME, 30);

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
        assertThat(request.getInt("durationInMinutes"), is(30));
    }
}
