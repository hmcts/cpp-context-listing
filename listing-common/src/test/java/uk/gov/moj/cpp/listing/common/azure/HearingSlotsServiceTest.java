package uk.gov.moj.cpp.listing.common.azure;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class HearingSlotsServiceTest {

    @Mock
    private Logger LOGGER;

    @Mock
    private DefaultRotaslAzureService rotaslAzureService;

    @Mock
    private RotaslAzureConfig rotaslAzureConfig;

    @Mock
    private Response expectedResponse;

    @InjectMocks
    private HearingSlotsService hearingSlotsService;

    @Before
    public void setUp() {
        when(rotaslAzureConfig.getSubscriptionKey()).thenReturn("75e6ff1510914801b91d176bcbeef0dc");
        when(LOGGER.isInfoEnabled()).thenReturn(true);
    }

    @Test
    public void shouldSearch() {
        final HashMap<String, String> objectObjectHashMap = new HashMap<>();

        when(rotaslAzureService.get(any(), any(), eq(objectObjectHashMap))).thenReturn(expectedResponse);

        final Response actualResponse = hearingSlotsService.search(objectObjectHashMap);

        assertThat(actualResponse, notNullValue());
    }

    @Test
    public void shouldUpdate() {
        JsonObject payload = Json.createObjectBuilder().build();


        when(rotaslAzureService.put(any(), any(), eq(payload))).thenReturn(expectedResponse);

        final Response actualResponse = hearingSlotsService.update(payload);

        assertThat(actualResponse, notNullValue());

    }


    @Test
    public void shouldDelete() {

        UUID hearingId = randomUUID();

        when(rotaslAzureService.delete(any(), any(), eq(hearingId))).thenReturn(expectedResponse);

        final Response actualResponse = hearingSlotsService.delete(hearingId);

        assertThat(actualResponse, notNullValue());

    }
}