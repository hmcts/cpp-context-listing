package uk.gov.justice.api.resource;

import static javax.json.Json.createArrayBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.listing.event.utils.FileUtil;
import uk.gov.moj.cpp.platform.data.utils.rest.service.RestClientService;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultQueryApiUpdateHearingSlotsResourceTest {

    @Mock
    private RestClientService restClientService;

    @Mock
    private Response response;

    @InjectMocks
    private DefaultQueryApiUpdateHearingSlotsResource resource;

    @Test
    public void updateHearingSlots() {
        setField(resource, "rotaSLEndpoint", "rotasl.service.api.endpoint");
        setField(resource, "updateAvailableHearingSlotsSubscriptionKey", "93411ae22b514c12ade724e880c135dc");
        when(restClientService.get(any(String.class), any(Map.class), any(Map.class))).thenReturn(response);
        when(response.readEntity(JsonObject.class)).thenReturn(createJsonObject());

        resource.updateHearingSlots(
                getSlotDetail());

        verify(restClientService).get(any(String.class), any(Map.class), any(Map.class));
        verify(restClientService).newResponseFrom(response, JsonObject.class);
    }

    private JsonObject createJsonObject() {
        return Json.createObjectBuilder()
                .add("results", 1)
                .add("pageCount", 1)
                .add("hearingSlots", createArrayBuilder().add("one").add("two").build())
                .build();
    }

    private String getSlotDetail(){
       return FileUtil.givenPayload("/test-data/listing.update.available.hearing.slots.json").toString();
    }
}