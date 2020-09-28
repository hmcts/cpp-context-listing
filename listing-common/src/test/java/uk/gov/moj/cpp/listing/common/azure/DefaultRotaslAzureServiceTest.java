package uk.gov.moj.cpp.listing.common.azure;

import static java.net.URI.*;
import static java.util.UUID.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.platform.data.utils.rest.service.RestClientService;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRotaslAzureServiceTest {

    private static final String ENDPOINT = "endpoint";
    private static final String SUBSCRIPTION = "subscription";
    private static final String SERVICE_URI = "serviceUri";
    public static final String URI_STRING = "uriString";

    @Mock
    private RestClientService restService;

    @Mock
    private RotaslAzureConfig rotaslAzureConfig;

    @InjectMocks
    private DefaultRotaslAzureService target;

    @Before
    public void setUp() {
        when(rotaslAzureConfig.getServiceUri()).thenReturn(SERVICE_URI);
        when(restService.post(any(String.class), any(Map.class), anyObject())).thenReturn(null);
        when(restService.delete(any(String.class), any(Map.class), any(Map.class))).thenReturn(null);
    }

    @Test
    public void shouldReturnOKWhenGetInvoked() {
        when(restService.get(any(String.class), any(Map.class), any(Map.class))).thenReturn(ok().build());
        when(restService.newResponseFrom(any(Response.class), any(Class.class))).thenReturn(ok().build());
        final Response response = target.get(ENDPOINT, SUBSCRIPTION, Collections.emptyMap());

        verify(restService).get(any(String.class), any(Map.class), any(Map.class));
        assertThat(response.getStatus(), is(HttpStatus.SC_OK));
    }

    @Test
    public void shouldReturnOKWhenPutInvoked() {
        when(restService.put(any(String.class), any(Map.class), anyObject())).thenReturn(ok().build());
        when(restService.newResponseFrom(any(Response.class), any(Class.class))).thenReturn(ok().build());
        final Response response = target.put(ENDPOINT, SUBSCRIPTION, new Object());

        verify(restService).put(any(String.class), any(Map.class), anyObject());
        assertThat(response.getStatus(), is(HttpStatus.SC_OK));
    }

    @Test
    public void shouldReturnWhenPostInvoked() {
        URI uri = create(URI_STRING);
        when(restService.post(any(String.class), any(Map.class), any(Map.class))).thenReturn(created(uri).build());
        when(restService.newResponseFrom(any(Response.class), any(Class.class))).thenReturn(created(uri).build());
        final Response response = target.post(ENDPOINT, SUBSCRIPTION, Collections.emptyMap());

        verify(restService).post(any(String.class), any(Map.class), any(Map.class));
        assertThat(response.getStatus(), is(HttpStatus.SC_CREATED));
    }

    @Test
    public void shouldReturnWhenDeleteInvoked() {
        when(restService.delete(any(String.class), any(Map.class))).thenReturn(accepted().build());
        when(restService.newResponseFrom(any(Response.class), any(Class.class))).thenReturn(accepted().build());
        final Response response = target.delete(ENDPOINT, SUBSCRIPTION, randomUUID());

        verify(restService).delete(any(String.class), any(Map.class));
        assertThat(response.getStatus(), is(HttpStatus.SC_ACCEPTED));
    }
}