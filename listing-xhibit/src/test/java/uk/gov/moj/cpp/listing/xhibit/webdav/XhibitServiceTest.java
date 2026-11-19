package uk.gov.moj.cpp.listing.xhibit.webdav;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The WebDAV PUT to XHIBIT: the request Sardine sent (method, URL, preemptive basic auth, body) and
 * the JEE error messages, against a local HTTP server.
 */
class XhibitServiceTest {

    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final AtomicInteger status = new AtomicInteger(201);
    private XhibitWebDavProperties properties;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        properties = new XhibitWebDavProperties();
        properties.setOutboundUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/xhibit-gateway/send-to-xhibit");
        properties.setUser("listing");
        properties.setPassword("secret");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void putsTheFileUnderTheOutboundUrlWithPreemptiveBasicAuth() throws Exception {
        new XhibitService(properties).sendToXhibit("<cs:DailyList/>".getBytes(UTF_8), "DailyList_421_20261119101530.xml");

        assertThat(requests).containsExactly("PUT /xhibit-gateway/send-to-xhibit/DailyList_421_20261119101530.xml Basic "
                + Base64.getEncoder().encodeToString("listing:secret".getBytes(UTF_8)) + " <cs:DailyList/>");
    }

    @Test
    void aNon2xxAnswerIsAnExportFailureWithTheJeeMessage() {
        status.set(404);
        final String url = properties.getOutboundUrl() + "/FirmList.xml";

        assertThatThrownBy(() -> new XhibitService(properties).sendToXhibit("<x/>".getBytes(UTF_8), "FirmList.xml"))
                .isInstanceOf(ExportFailedException.class)
                .hasMessage("Failed to send to Xhibit,CPF01: Failed to put file 'FirmList.xml' to '" + url
                        + "'. Response status: 404, for document with name: FirmList.xml");
    }

    @Test
    void anUnreachableServerIsAnExportFailure() {
        server.stop(0);

        assertThatThrownBy(() -> new XhibitService(properties).sendToXhibit("<x/>".getBytes(UTF_8), "WarnedList.xml"))
                .isInstanceOf(ExportFailedException.class)
                .hasMessageStartingWith("Failed to send to Xhibit,CPF01: Failed to put file 'WarnedList.xml' to '");
    }

    @Test
    void theFileNameIsAppendedWithASingleSlash() {
        assertThat(XhibitService.toUrl("http://h/x/", "a.xml")).isEqualTo("http://h/x/a.xml");
        assertThat(XhibitService.toUrl("http://h/x", "a.xml")).isEqualTo("http://h/x/a.xml");
    }

    private void handle(final HttpExchange exchange) throws IOException {
        final String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                + exchange.getRequestHeaders().getFirst("Authorization") + " " + body);
        exchange.sendResponseHeaders(status.get(), -1);
        exchange.close();
    }
}
