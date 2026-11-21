package uk.gov.moj.cpp.listing.common.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Map;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisionalBookingServiceTest {

    private static final String BOOKING_IDS = "bookingIds";

    @Mock
    private SystemUserProvider systemUserProvider;

    private final ProvisionalBookingService service = new ProvisionalBookingService();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnTheSlotsCourtschedulerAnswers() throws Exception {
        courtschedulerAnswers(200, "{\"provisionalSlots\":[]}");

        final Response response = service.getSlots(Map.of(BOOKING_IDS, randomUUID().toString()));

        assertThat(response.getStatus(), is(200));
        assertThat(((JsonObject) response.getEntity()).getJsonArray("provisionalSlots").isEmpty(), is(true));
    }

    @Test
    void shouldAnswerServerErrorWhenCourtschedulerFails() throws Exception {
        courtschedulerAnswers(503, "unavailable");

        assertThat(service.getSlots(Map.of(BOOKING_IDS, randomUUID().toString())).getStatus(), is(500));
    }

    @Test
    void shouldAnswerServerErrorWhenCourtschedulerCannotBeReached() throws Exception {
        givenTheService("http://127.0.0.1:1");

        assertThat(service.getSlots(Map.of(BOOKING_IDS, randomUUID().toString())).getStatus(), is(500));
    }

    @Test
    void shouldRejectMissingParams() {
        assertThrows(DataValidationException.class, () -> service.getSlots(null));
    }

    private void courtschedulerAnswers(final int status, final String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/unconfirmedBooking", exchange -> {
            final byte[] bytes = body.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        givenTheService("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private void givenTheService(final String baseUri) throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(randomUUID()));
        setField("baseUri", baseUri);
        setField("systemUserProvider", systemUserProvider);
        setField("stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField("courtSchedulerHttpClient", new CourtSchedulerHttpClient(5000, 5000));
    }

    private void setField(final String name, final Object value) throws Exception {
        final Field field = ProvisionalBookingService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
