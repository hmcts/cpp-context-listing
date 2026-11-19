package uk.gov.moj.cpp.listing.query.document.generator;

import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;
import uk.gov.moj.cpp.listing.query.document.generator.exception.DocumentGenerationFailedException;

import java.net.URI;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Renders a court list PDF through systemdocgenerator. Replaces the
 * {@code system-documentgenerator-client} jar's {@code DefaultDocumentGeneratorClient} with the
 * same call: {@code POST .../systemdocgenerator-service/command/api/rest/systemdocgenerator/render},
 * {@code Content-Type: application/vnd.systemdocgenerator.render+json}, CJSCPPUID = the context
 * system user, body {@code {templateName, templatePayload, conversionFormat: "pdf"}}; the response
 * body is the document. Any non-200 or I/O failure is a {@link DocumentGenerationFailedException}.
 */
@Component
public class DocumentGeneratorClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorClient.class);

    // As deployed, and as the legacy ITs stub it (DocumentGeneratorStub); the client jar's template
    // (systemdocgenerator-command-api) was rewritten to the deployed context root at run time.
    static final String RENDER_PATH = "/systemdocgenerator-service/command/api/rest/systemdocgenerator/render";
    static final String RENDER_REQUEST_TYPE = "application/vnd.systemdocgenerator.render+json";

    private final RestClient restClient;
    private final ListingClientsProperties properties;

    public DocumentGeneratorClient(final RestClient cppRestClient, final ListingClientsProperties properties) {
        this.restClient = cppRestClient;
        this.properties = properties;
    }

    public byte[] generateDocument(final JsonObject documentPayload, final String templateName) {
        final UUID systemUserId;
        try {
            systemUserId = properties.requiredSystemUserId();
        } catch (final IllegalStateException e) {
            throw new DocumentGenerationFailedException("Could not find systemId ");
        }
        final JsonObject request = Json.createObjectBuilder()
                .add("templateName", templateName)
                .add("templatePayload", documentPayload)
                .add("conversionFormat", "pdf")
                .build();
        try {
            return restClient.post()
                    .uri(URI.create(properties.baseUrl() + RENDER_PATH))
                    .contentType(MediaType.parseMediaType(RENDER_REQUEST_TYPE))
                    .header("CJSCPPUID", systemUserId.toString())
                    .body(request.toString())
                    .exchange((req, response) -> {
                        final byte[] body = response.getBody().readAllBytes();
                        if (response.getStatusCode().value() != 200) {
                            throw new DocumentGenerationFailedException(new IllegalStateException(String.format(
                                    "Failed to generate document with identifier %s. Http status: %s", templateName, response.getStatusCode().value())));
                        }
                        return body;
                    });
        } catch (final RestClientException e) {
            LOGGER.error("Failed to generate document for template {} with document payload {} ", templateName, documentPayload);
            throw new DocumentGenerationFailedException(e);
        }
    }
}
