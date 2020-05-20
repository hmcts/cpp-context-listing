package uk.gov.moj.cpp.listing.common.azure;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProvisionalBookingServiceTest {

    @Mock
    private DefaultRotaslAzureService rotaslAzureService;

    @Mock
    private RotaslAzureConfig rotaslAzureConfig;

    @Mock
    private Response expectedResponse;

    @InjectMocks
    private ProvisionalBookingService provisionalBookingService;

    @Before
    public void setUp() {
        when(rotaslAzureConfig.getSubscriptionKey()).thenReturn("75e6ff1510914801b91d176bcbeef0dc");
    }

    @Test
    public void shouldBookSlots() {
        final Object payload = "sample";

        when(rotaslAzureService.post(any(), any(), eq(payload))).thenReturn(expectedResponse);

        final Response actualResponse = provisionalBookingService.bookSlots(payload);

        assertThat(actualResponse, notNullValue());
    }

    @Test
    public void shouldGetProvisionalSlots() {
        final List<String> bookingIdList = Arrays.asList(randomUUID().toString(), randomUUID().toString());
        final Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("bookingIds", String.join(",", bookingIdList));

        when(rotaslAzureService.get(any(), any(), eq(queryParameters))).thenReturn(expectedResponse);

        provisionalBookingService.getSlots(queryParameters);
    }
}
