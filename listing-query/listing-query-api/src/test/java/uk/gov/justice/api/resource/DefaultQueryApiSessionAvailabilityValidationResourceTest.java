package uk.gov.justice.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;

import java.util.Map;

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

    private static final String VALIDATE_RESPONSE_JSON = "listing.validate.session.availability.json";

    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @InjectMocks
    private DefaultQueryApiSessionAvailabilityValidationResource resource;

    @Captor
    private ArgumentCaptor<Map<String, String>> paramsCaptor;

    @Test
    void shouldCallAdapterAndReturnValidateSessionAvailabilityResponse() {
        final JsonObject responseEntity = new uk.gov.justice.services.common.converter.StringToJsonObjectConverter()
                .convert(FileUtil.getPayload(VALIDATE_RESPONSE_JSON));
        final Response adapterResponse = Response.status(Response.Status.OK).entity(responseEntity).build();

        when(courtSchedulerServiceAdapter.validateSessionAvailability(any(Map.class))).thenReturn(adapterResponse);

        final Response result = resource.validateSessionAvailability(
                "ADULT",
                "2017-10-11",
                "2020-10-11",
                null,
                "Z01KR05",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "20",
                "1",
                null,
                null,
                null,
                null);

        verify(courtSchedulerServiceAdapter).validateSessionAvailability(paramsCaptor.capture());
        final Map<String, String> params = paramsCaptor.getValue();
        assertEquals("ADULT", params.get("panel"));
        assertEquals("2017-10-11", params.get("sessionStartDate"));
        assertEquals("2020-10-11", params.get("sessionEndDate"));
        assertEquals("20", params.get("pageSize"));
        assertEquals("1", params.get("pageNumber"));
        assertEquals("ALL", params.get("status"));

        assertNotNull(result);
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
    }

    @Test
    void shouldPassConsecutiveDaysStatusAndIsWeekCommencingToAdapter() {
        final JsonObject responseEntity = new uk.gov.justice.services.common.converter.StringToJsonObjectConverter()
                .convert(FileUtil.getPayload(VALIDATE_RESPONSE_JSON));
        when(courtSchedulerServiceAdapter.validateSessionAvailability(any(Map.class)))
                .thenReturn(Response.status(Response.Status.OK).entity(responseEntity).build());

        resource.validateSessionAvailability(
                "ADULT",
                "2017-10-11",
                "2020-10-11",
                null,
                "Z01KR05",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "20",
                "1",
                30,
                "FINAL",
                2,
                false);

        verify(courtSchedulerServiceAdapter).validateSessionAvailability(paramsCaptor.capture());
        final Map<String, String> params = paramsCaptor.getValue();
        assertEquals("FINAL", params.get("status"));
        assertEquals("2", params.get("consecutiveDays"));
        assertEquals("false", params.get("isWeekCommencing"));
        assertEquals("30", params.get("duration"));
    }
}
