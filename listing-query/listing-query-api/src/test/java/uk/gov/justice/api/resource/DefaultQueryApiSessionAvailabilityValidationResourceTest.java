package uk.gov.justice.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;

import javax.json.JsonObject;
import javax.json.Json;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultQueryApiSessionAvailabilityValidationResourceTest {

    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @InjectMocks
    private DefaultQueryApiSessionAvailabilityValidationResource resource;

    @Captor
    private ArgumentCaptor<JsonObject> payloadCaptor;

    @Test
    void shouldCallAdapterAndReturnValidateSessionAvailabilityResponse() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .add("duration", 30)
                .build();
        final JsonObject responseEntity = Json.createObjectBuilder().build();
        final Response adapterResponse = Response.status(Response.Status.OK).entity(responseEntity).build();

        when(courtSchedulerServiceAdapter.validateSessionAvailability(any(JsonObject.class))).thenReturn(adapterResponse);

        final Response result = resource.validateSessionAvailability(requestPayload);

        verify(courtSchedulerServiceAdapter).validateSessionAvailability(payloadCaptor.capture());
        final JsonObject payload = payloadCaptor.getValue();
        assertNotNull(payload.getJsonArray("courtScheduleIdList"));
        assertEquals(30, payload.getInt("duration"));

        assertNotNull(result);
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
    }

    @Test
    void shouldTransformSingletonArrayToConsecutiveDaysPayload() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .add("consecutiveDays", 2)
                .build();

        when(courtSchedulerServiceAdapter.validateSessionAvailability(any(JsonObject.class)))
                .thenReturn(Response.status(Response.Status.OK).entity(Json.createObjectBuilder().build()).build());

        resource.validateSessionAvailability(requestPayload);

        verify(courtSchedulerServiceAdapter).validateSessionAvailability(payloadCaptor.capture());
        final JsonObject payload = payloadCaptor.getValue();
        assertEquals("f8254db1-1683-483e-afb3-b87fde5a0a26", payload.getString("courtScheduleId"));
        assertEquals(2, payload.getInt("consecutiveDays"));
    }

    @Test
    void shouldRejectWhenConsecutiveDaysHasMultipleCourtScheduleIds() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26"))
                        .add(Json.createObjectBuilder().add("courtScheduleId", "9e4932f7-97b2-3010-b942-ddd2624e4dd8")))
                .add("consecutiveDays", 2)
                .build();

        final Response response = resource.validateSessionAvailability(requestPayload);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void shouldRejectWhenDurationAndConsecutiveDaysAreMissing() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .build();

        final Response response = resource.validateSessionAvailability(requestPayload);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
