package uk.gov.moj.cpp.listing.common.service;

import static java.util.List.of;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.courtroomchange.ChangeCourtRoomForMultidayException;
import uk.gov.moj.cpp.listing.common.courtroomchange.ChangedDaySession;
import uk.gov.moj.cpp.listing.common.courtroomchange.RequestedChangeDay;

import java.time.LocalDate;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class CourtSchedulerServiceAdapterChangeCourtRoomForMultidayTest {

    @InjectMocks
    private CourtSchedulerServiceAdapter adapter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    @Test
    void shouldParseAllocatedSchedulesOn200() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.parse("2026-05-01");
        final List<RequestedChangeDay> days = of(new RequestedChangeDay(sessionDate, UUID.randomUUID(), 360));

        final JsonObject body = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("source", "CHANGE_COURT_ROOM_FOR_MULTIDAY")
                .add("allocatedSchedules", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("courtRoomId", "9d324f4f-6c3b-451f-ac1e-f459db781153")
                                .add("sessionDate", "2026-05-01")
                                .add("sessionStartTime", "2026-05-01T09:00:00Z")
                                .add("durationInMinutes", 360)))
                .build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.changeCourtRoomForMultidayHearing(eq(hearingId), any())).thenReturn(response);

        final List<ChangedDaySession> result = adapter.changeCourtRoomForMultidayHearing(hearingId, days);

        assertThat(result, hasSize(1));
        final ChangedDaySession session = result.get(0);
        assertThat(session.courtScheduleId(), is(courtScheduleId));
        assertThat(session.courtRoomId(), is("9d324f4f-6c3b-451f-ac1e-f459db781153"));
        assertThat(session.sessionDate(), is(sessionDate));
        assertThat(session.sessionStartTime(), is("2026-05-01T09:00:00Z"));
        assertThat(session.durationInMinutes(), is(360));
    }

    @Test
    void shouldBuildRequestBodyFromRequestedChangeDays() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.parse("2026-05-01");
        final List<RequestedChangeDay> days = of(new RequestedChangeDay(sessionDate, courtScheduleId, 360));

        final JsonObject body = createObjectBuilder().add("allocatedSchedules", createArrayBuilder()).build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.changeCourtRoomForMultidayHearing(eq(hearingId), any())).thenReturn(response);

        adapter.changeCourtRoomForMultidayHearing(hearingId, days);

        final ArgumentCaptor<JsonObject> requestCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(hearingSlotsService).changeCourtRoomForMultidayHearing(eq(hearingId), requestCaptor.capture());
        final JsonObject request = requestCaptor.getValue();
        final JsonObject day = request.getJsonArray("days").getJsonObject(0);
        assertThat(day.getString("sessionDate"), is("2026-05-01"));
        assertThat(day.getString("courtScheduleId"), is(courtScheduleId.toString()));
        assertThat(day.getInt("durationInMinutes"), is(360));
    }

    @Test
    void shouldThrowWith422AndErrorCodeWhenNoAllocationOnDate() {
        final JsonObject body = createObjectBuilder()
                .add("errorCode", "NO_ALLOCATION_ON_DATE")
                .add("message", "No allocation for the requested date")
                .build();
        when(response.getStatus()).thenReturn(422);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.changeCourtRoomForMultidayHearing(any(), any())).thenReturn(response);

        final ChangeCourtRoomForMultidayException ex = assertThrows(ChangeCourtRoomForMultidayException.class,
                () -> adapter.changeCourtRoomForMultidayHearing(UUID.randomUUID(),
                        of(new RequestedChangeDay(LocalDate.parse("2026-05-01"), UUID.randomUUID(), 360))));

        assertThat(ex.getHttpStatus(), is(422));
        assertThat(ex.getErrorCode(), is("NO_ALLOCATION_ON_DATE"));
    }

    @Test
    void shouldNormaliseLegacy404ToA422NoSessionFound() {
        final JsonObject body = createObjectBuilder().build();
        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(body);
        when(hearingSlotsService.changeCourtRoomForMultidayHearing(any(), any())).thenReturn(response);

        final ChangeCourtRoomForMultidayException ex = assertThrows(ChangeCourtRoomForMultidayException.class,
                () -> adapter.changeCourtRoomForMultidayHearing(UUID.randomUUID(),
                        of(new RequestedChangeDay(LocalDate.parse("2026-05-01"), UUID.randomUUID(), 360))));

        assertThat(ex.getHttpStatus(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(ex.getErrorCode(), is("NO_SESSION_FOUND"));
    }
}
