package uk.gov.moj.cpp.listing.command.hearing.api.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.common.service.ProvisionalBookingService;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import uk.gov.moj.cpp.listing.common.client.HttpResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Port of the JEE test (MBD refactor P2.3.2): the booking lookup tests. The JEE
 * {@code getCourtSchedulesById} tests (Apache HttpClient) are dropped with the method, which had no
 * caller.
 */
@ExtendWith(MockitoExtension.class)
class CourtSchedulerServiceTest {

    @Mock
    private ProvisionalBookingService provisionalBookingService;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private CourtSchedulerService courtSchedulerService;

    @Test
    public void shouldReturnTheSessionsCourtschedulerHoldsForTheBooking() {
        final JsonObject slot = Json.createObjectBuilder().add("courtScheduleId", UUID.randomUUID().toString()).build();
        final JsonObject body = Json.createObjectBuilder().add("provisionalSlots", Json.createArrayBuilder().add(slot)).build();
        final CourtSchedule session = new CourtSchedule();
        givenTheBookingLookupAnswers(body);
        when(jsonObjectToObjectConverter.convert(slot, CourtSchedule.class)).thenReturn(session);

        assertThat(courtSchedulerService.getCourtSchedulesByProvisionalBookingId("bk-1"), is(List.of(session)));
    }

    /** courtscheduler answers an unknown booking id with 200 and no slots: that is "no booking". */
    @Test
    public void shouldReturnNoSessionsWhenCourtschedulerHoldsNoneForTheBooking() {
        final JsonObject body = Json.createObjectBuilder().add("provisionalSlots", Json.createArrayBuilder()).build();
        givenTheBookingLookupAnswers(body);

        assertThat(courtSchedulerService.getCourtSchedulesByProvisionalBookingId("bk-1"), is(List.of()));
    }

    /**
     * ProvisionalBookingService reports an error status, a timeout or no connection as a 500. None
     * of those says the booking is gone, so the lookup must fail and be retried rather than answer
     * "no booking", which ends in [LISTING-LOST] for a hearing a retry would have listed.
     */
    @Test
    public void shouldFailRatherThanReportNoBookingWhenCourtschedulerDoesNotAnswer() {
        final HttpResult failed = mock(HttpResult.class);
        when(failed.getStatus()).thenReturn(500);
        when(provisionalBookingService.getSlots(any())).thenReturn(failed);

        org.junit.jupiter.api.Assertions.assertThrows(CourtSchedulerUnavailableException.class,
                () -> courtSchedulerService.getCourtSchedulesByProvisionalBookingId("bk-1"));
    }

    @Test
    public void shouldFailRatherThanReportNoBookingWhenThereIsNoResponseAtAll() {
        when(provisionalBookingService.getSlots(any())).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(CourtSchedulerUnavailableException.class,
                () -> courtSchedulerService.getCourtSchedulesByProvisionalBookingId("bk-1"));
    }

    private void givenTheBookingLookupAnswers(final JsonObject body) {
        final HttpResult response = mock(HttpResult.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getEntity()).thenReturn(body);
        when(provisionalBookingService.getSlots(any())).thenReturn(response);
        when(objectToJsonObjectConverter.convert(body)).thenReturn(body);
    }
}
