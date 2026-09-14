package uk.gov.moj.cpp.listing.common.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateResult;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * courtRoomId/startInstant/endInstant mirror main's contract (the review artifact this
 * reconciles) - courtCentreId/startDate-only calls no longer exist on the adapter.
 */
@ExtendWith(MockitoExtension.class)
class CourtSchedulerServiceAdapterMoveHearingToPastDateTest {

    @InjectMocks
    private CourtSchedulerServiceAdapter adapter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    private static final ZonedDateTime START_INSTANT = ZonedDateTime.parse("2026-05-01T09:00:00Z");
    private static final ZonedDateTime END_INSTANT = ZonedDateTime.parse("2026-05-01T17:00:00Z");

    @Test
    void shouldParseSlotDetailsOn200() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final LocalDate startDate = LocalDate.parse("2026-05-01");

        final JsonObject body = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("courtScheduleId", courtScheduleId.toString())
                .add("courtRoomId", "9d324f4f-6c3b-451f-ac1e-f459db781153")
                .add("sessionDate", "2026-05-01")
                .add("sessionStartTime", "2026-05-01T09:00:00Z")
                .add("sessionEndTime", "2026-05-01T17:00:00Z")
                .add("durationInMinutes", 30)
                .add("source", "MOVE_TO_PAST_DATE")
                .build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        final MoveHearingToPastDateResult result = adapter.moveHearingToPastDate(
                hearingId, courtCentreId, courtRoomId, START_INSTANT, END_INSTANT, 30, "MAGISTRATES");

        assertThat(result.courtScheduleId(), is(courtScheduleId));
        assertThat(result.courtRoomId(), is("9d324f4f-6c3b-451f-ac1e-f459db781153"));
        assertThat(result.sessionDate(), is(startDate));
        assertThat(result.sessionStartTime(), is("2026-05-01T09:00:00Z"));
        assertThat(result.sessionEndTime(), is("2026-05-01T17:00:00Z"));
        assertThat(result.durationInMinutes(), is(30));
        assertThat(result.lastSessionDate(), is(startDate));
        assertThat("flat body is a single session", result.sessions().size(), is(1));
    }

    /** courtscheduler's real wire shape: the booked day(s) nested under "sessions", first = start day. */
    @Test
    void shouldParseFirstNestedSessionOn200() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID firstScheduleId = UUID.randomUUID();
        final UUID secondScheduleId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate startDate = LocalDate.parse("2026-05-04");

        final JsonObject body = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("source", "MOVE_TO_PAST_DATE")
                .add("sessions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("courtScheduleId", firstScheduleId.toString())
                                .add("courtRoomId", "9d324f4f-6c3b-451f-ac1e-f459db781153")
                                .add("sessionDate", "2026-05-04")
                                .add("sessionStartTime", "2026-05-04T10:00:00.000+00:00")
                                .add("sessionEndTime", "2026-05-04T17:00:00.000+00:00"))
                        .add(createObjectBuilder()
                                .add("courtScheduleId", secondScheduleId.toString())
                                .add("courtRoomId", "9d324f4f-6c3b-451f-ac1e-f459db781153")
                                .add("courtHouseId", courtHouseId.toString())
                                .add("sessionDate", "2026-05-05")
                                .add("isDraft", false)))
                .build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        final MoveHearingToPastDateResult result = adapter.moveHearingToPastDate(
                hearingId, courtCentreId, courtRoomId, START_INSTANT, ZonedDateTime.parse("2026-05-05T17:00:00Z"), 720, "CROWN");

        assertThat(result.courtScheduleId(), is(firstScheduleId));
        assertThat(result.courtRoomId(), is("9d324f4f-6c3b-451f-ac1e-f459db781153"));
        assertThat(result.sessionDate(), is(startDate));
        assertThat(result.sessionStartTime(), is("2026-05-04T10:00:00.000+00:00"));
        assertThat(result.sessionEndTime(), is("2026-05-04T17:00:00.000+00:00"));
        assertThat(result.durationInMinutes(), is(nullValue()));
        assertThat("end date follows the LAST booked session", result.lastSessionDate(), is(LocalDate.parse("2026-05-05")));
        assertThat("every booked session is returned, in order", result.sessions().size(), is(2));
        assertThat(result.sessions().get(0).courtScheduleId(), is(firstScheduleId));
        assertThat(result.sessions().get(1).courtScheduleId(), is(secondScheduleId));
        assertThat(result.sessions().get(1).sessionDate(), is(LocalDate.parse("2026-05-05")));
        assertThat(result.sessions().get(1).courtCentreId(), is(courtHouseId));
        assertThat(result.sessions().get(1).isDraft(), is(false));
        assertThat("absent on the wire -> unknown, not false", result.sessions().get(0).isDraft(), is(nullValue()));
    }

    @Test
    void shouldSendSuppliedJurisdictionInRequest() {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject body = createObjectBuilder().add("courtScheduleId", UUID.randomUUID().toString())
                .add("sessionDate", "2026-05-01").build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        adapter.moveHearingToPastDate(hearingId, UUID.randomUUID(), UUID.randomUUID(), START_INSTANT, END_INSTANT, 30, "CROWN");

        final ArgumentCaptor<JsonObject> requestCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(hearingSlotsService).moveHearingToPastDate(eq(hearingId), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getString("jurisdiction"), is("CROWN"));
    }

    @Test
    void shouldOmitDurationInRequestWhenNotSupplied() {
        final UUID hearingId = UUID.randomUUID();
        final JsonObject body = createObjectBuilder().add("courtScheduleId", UUID.randomUUID().toString())
                .add("sessionDate", "2026-05-01").build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        final MoveHearingToPastDateResult result = adapter.moveHearingToPastDate(
                hearingId, UUID.randomUUID(), UUID.randomUUID(), START_INSTANT, END_INSTANT, null, "MAGISTRATES");

        assertThat(result.durationInMinutes(), is(nullValue()));
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

        final ZonedDateTime future = ZonedDateTime.parse("2999-01-01T09:00:00Z");
        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), future, future, 30, "MAGISTRATES"));

        assertThat(ex.getHttpStatus(), is(422));
        assertThat(ex.getErrorCode(), is("FUTURE_DATE_NOT_ALLOWED"));
    }

    /** A genuine 422 NO_SESSION_FOUND from courtscheduler is normalised to the fixed user-facing message (ported from main). */
    @Test
    void shouldThrowWith422NoSessionFoundWhenCourtschedulerReturns422() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_SESSION_FOUND")
                .add("message", "No session available at courtCentreId=... on 2026-05-01")
                .build();
        when(response.getStatus()).thenReturn(422);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(any(), any())).thenReturn(response);

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), START_INSTANT, END_INSTANT, 30, "MAGISTRATES"));

        assertThat(ex.getHttpStatus(), is(422));
        assertThat(ex.getErrorCode(), is("NO_SESSION_FOUND"));
        assertThat("courtscheduler's own diagnostic message is replaced with the fixed copy",
                ex.getResponseBody().getString("message"),
                is("No suitable sessions are available for the selected date. Please select another date."));
    }

    @Test
    void shouldNormaliseLegacy404ToA422NoSessionFound() {
        final JsonObject body = createObjectBuilder().build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(any(), any())).thenReturn(response);

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), START_INSTANT, END_INSTANT, 30, "MAGISTRATES"));

        assertThat(ex.getHttpStatus(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(ex.getErrorCode(), is("NO_SESSION_FOUND"));
        assertThat(ex.getResponseBody().getString("message"),
                is("No suitable sessions are available for the selected date. Please select another date."));
    }

    @Test
    void shouldNotSendHearingIdInRequestBody() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final JsonObject body = createObjectBuilder().add("courtScheduleId", UUID.randomUUID().toString())
                .add("sessionDate", "2026-05-01").build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(eq(hearingId), any())).thenReturn(response);

        adapter.moveHearingToPastDate(hearingId, courtCentreId, courtRoomId, START_INSTANT, END_INSTANT, 30, "MAGISTRATES");

        final ArgumentCaptor<JsonObject> requestCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(hearingSlotsService).moveHearingToPastDate(eq(hearingId), requestCaptor.capture());
        final JsonObject request = requestCaptor.getValue();
        assertThat(request.containsKey("hearingId"), is(false));
        assertThat(request.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(request.getString("courtRoomId"), is(courtRoomId.toString()));
        assertThat(request.getString("jurisdiction"), is("MAGISTRATES"));
        assertThat(request.getString("startTime"), is(START_INSTANT.toString()));
        assertThat(request.getString("endTime"), is(END_INSTANT.toString()));
        assertThat(request.getInt("durationInMinutes"), is(30));
    }
}
