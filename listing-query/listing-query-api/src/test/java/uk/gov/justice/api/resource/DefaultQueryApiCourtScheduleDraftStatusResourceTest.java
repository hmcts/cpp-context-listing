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
class DefaultQueryApiCourtScheduleDraftStatusResourceTest {

    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @InjectMocks
    private DefaultQueryApiCourtScheduleDraftStatusResource resource;

    @Captor
    private ArgumentCaptor<JsonObject> payloadCaptor;

    @Test
    void shouldForwardPayloadToAdapterAndReturnAnyDraftTrue() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "f8254db1-1683-483e-afb3-b87fde5a0a26")))
                .build();
        final JsonObject adapterBody = Json.createObjectBuilder().add("anyDraft", true).build();

        when(courtSchedulerServiceAdapter.getCourtScheduleDraftStatus(requestPayload)).thenReturn(adapterBody);

        final Response result = resource.checkDraftStatus(requestPayload);

        verify(courtSchedulerServiceAdapter).getCourtScheduleDraftStatus(payloadCaptor.capture());
        assertEquals(requestPayload, payloadCaptor.getValue());
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(adapterBody, result.getEntity());
    }

    @Test
    void shouldReturnAnyDraftFalseWhenAdapterReportsAllNonDraft() {
        final JsonObject requestPayload = Json.createObjectBuilder()
                .add("courtScheduleIdList", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("courtScheduleId", "9e4932f7-97b2-3010-b942-ddd2624e4dd8")))
                .build();
        final JsonObject adapterBody = Json.createObjectBuilder().add("anyDraft", false).build();

        when(courtSchedulerServiceAdapter.getCourtScheduleDraftStatus(requestPayload)).thenReturn(adapterBody);

        final Response result = resource.checkDraftStatus(requestPayload);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(adapterBody, result.getEntity());
    }
}
