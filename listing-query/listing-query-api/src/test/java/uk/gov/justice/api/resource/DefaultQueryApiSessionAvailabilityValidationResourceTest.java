package uk.gov.justice.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;

import javax.json.Json;
import javax.json.JsonObject;
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
    void shouldForwardPayloadToAdapterAndReturnResponse() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .add("duration", 30)
                .build();
        final Response adapterResponse = Response.status(Response.Status.OK).entity(Json.createObjectBuilder().build()).build();

        when(courtSchedulerServiceAdapter.validateSessionAvailability(requestPayload)).thenReturn(adapterResponse);

        final Response result = resource.validateSessionAvailability(requestPayload);

        verify(courtSchedulerServiceAdapter).validateSessionAvailability(payloadCaptor.capture());
        assertEquals(requestPayload, payloadCaptor.getValue());
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
    }

    @Test
    void shouldReturnAdapterErrorResponseUnchanged() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .build();
        final JsonObject errorBody = Json.createObjectBuilder()
                .add("validationResult", Json.createObjectBuilder()
                        .add("status", "FAILURE")
                        .add("validationError", "duration is required"))
                .build();
        final Response adapterResponse = Response.status(Response.Status.BAD_REQUEST).entity(errorBody).build();

        when(courtSchedulerServiceAdapter.validateSessionAvailability(requestPayload)).thenReturn(adapterResponse);

        final Response result = resource.validateSessionAvailability(requestPayload);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        assertEquals(errorBody, result.getEntity());
    }
}
