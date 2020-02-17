package uk.gov.justice.api.resource;

import static javax.json.Json.createArrayBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;

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
public class DefaultQueryApiHearingSlotsResourceTest {
    @Mock
    private HearingSlotsService hearingSlotsService;

    @Mock
    private Response response;

    @InjectMocks
    private DefaultQueryApiHearingSlotsResource resource;

    @Test
    public void searchHearingSlots() {
        when(hearingSlotsService.search(any(Map.class))).thenReturn(response);
        when(response.readEntity(JsonObject.class)).thenReturn(createJsonObject());

        resource.getHearingSlots("ADULT",
                "2017-10-11",
                "2020-10-11",
                "BAOOUS",
                "BAOOUS",
                "001c067d-eaca-4ce5-ad90-a366ef3e4bb6",
                "1234",
                "BYS",
                "AM",
                "20",
                "1");

        verify(hearingSlotsService).search(any(Map.class));
    }

    private JsonObject createJsonObject() {
        return Json.createObjectBuilder()
                .add("results", 12)
                .add("pageCount", 1)
                .add("hearingSlots", createArrayBuilder().add("one").add("two").build())
                .build();
    }
}
