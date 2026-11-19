package uk.gov.moj.cpp.listing.query.document.generator;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.moj.cpp.listing.common.client.CppRestClientConfig;
import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;
import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import uk.gov.moj.cpp.listing.query.document.generator.exception.DocumentGenerationFailedException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Replaces the legacy test of the systemdocgenerator client wrapper: the call is now made with
 * Spring RestClient, so it is checked against a local HTTP server (path, media type, CJSCPPUID,
 * body) instead of mocking the client jar.
 */
class DocumentGeneratorClientTest {

    private static final UUID SYSTEM_USER = randomUUID();

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> userId = new AtomicReference<>();
    private int status = 200;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(DocumentGeneratorClient.RENDER_PATH, exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            userId.set(exchange.getRequestHeaders().getFirst("CJSCPPUID"));
            final byte[] pdf = "%PDF".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, pdf.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(pdf);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private DocumentGeneratorClient client(final UUID systemUser) {
        final ListingClientsProperties properties = new ListingClientsProperties(
                "http://localhost:" + server.getAddress().getPort(), null, systemUser, 5, 5);
        return new DocumentGeneratorClient(new CppRestClientConfig().cppRestClient(properties), properties);
    }

    @Test
    void shouldGenerateDocument() {
        final JsonObject payload = JsonObjects.createObjectBuilder().add("a", "b").build();

        final byte[] bytes = client(SYSTEM_USER).generateDocument(payload, "PublicCourtList");

        assertThat(new String(bytes, StandardCharsets.UTF_8), is("%PDF"));
        assertThat(contentType.get(), is("application/vnd.systemdocgenerator.render+json"));
        assertThat(userId.get(), is(SYSTEM_USER.toString()));
        final JsonObject sent = Json.createReader(new StringReader(requestBody.get())).readObject();
        assertThat(sent.getString("templateName"), is("PublicCourtList"));
        assertThat(sent.getString("conversionFormat"), is("pdf"));
        assertThat(sent.getJsonObject("templatePayload"), is(payload));
    }

    @Test
    void shouldGenerateDocumentThrowException() {
        status = 500;
        final JsonObject payload = JsonObjects.createObjectBuilder().build();

        assertThrows(DocumentGenerationFailedException.class, () -> client(SYSTEM_USER).generateDocument(payload, "x"));
    }

    @Test
    void shouldFailWithoutSystemUser() {
        final JsonObject payload = JsonObjects.createObjectBuilder().build();

        assertThrows(DocumentGenerationFailedException.class, () -> client(null).generateDocument(payload, "x"));
    }
}
