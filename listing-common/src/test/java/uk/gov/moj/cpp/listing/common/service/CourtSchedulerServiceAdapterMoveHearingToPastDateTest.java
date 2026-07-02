package uk.gov.moj.cpp.listing.common.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateResult;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtSchedulerServiceAdapterMoveHearingToPastDateTest {

    @InjectMocks
    private CourtSchedulerServiceAdapter adapter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    @Test
    void shouldParseSlotDetailsOn200() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
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

        final MoveHearingToPastDateResult result = adapter.moveHearingToPastDate(hearingId, courtCentreId, startDate, 30);

        assertThat(result.courtScheduleId(), is(courtScheduleId));
        assertThat(result.courtRoomId(), is("9d324f4f-6c3b-451f-ac1e-f459db781153"));
        assertThat(result.sessionDate(), is(startDate));
        assertThat(result.sessionStartTime(), is("2026-05-01T09:00:00Z"));
        assertThat(result.sessionEndTime(), is("2026-05-01T17:00:00Z"));
        assertThat(result.durationInMinutes(), is(30));
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

        final MoveHearingToPastDateResult result = adapter.moveHearingToPastDate(hearingId, UUID.randomUUID(), LocalDate.parse("2026-05-01"), null);

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

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), LocalDate.parse("2999-01-01"), 30));

        assertThat(ex.getHttpStatus(), is(422));
        assertThat(ex.getErrorCode(), is("FUTURE_DATE_NOT_ALLOWED"));
    }

    @Test
    void shouldThrowWith404WhenNoSession() {
        final JsonObject body = createObjectBuilder().build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.moveHearingToPastDate(any(), any())).thenReturn(response);

        final MoveHearingToPastDateException ex = assertThrows(MoveHearingToPastDateException.class,
                () -> adapter.moveHearingToPastDate(UUID.randomUUID(), UUID.randomUUID(), LocalDate.parse("2026-05-01"), 30));

        assertThat(ex.getHttpStatus(), is(HttpStatus.SC_NOT_FOUND));
    }
}
