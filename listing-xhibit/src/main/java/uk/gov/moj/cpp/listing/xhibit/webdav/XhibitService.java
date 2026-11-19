package uk.gov.moj.cpp.listing.xhibit.webdav;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sends a court-list XML file to XHIBIT by WebDAV {@code PUT} (port of the JEE
 * {@code DefaultXhibitService} + {@code XhibitSession} + {@code XhibitSessionFactory} +
 * {@code UrlFactory}, listing-common).
 *
 * <p>The JEE code used Sardine 5.7 ({@code SardineFactory.begin(user, password)},
 * {@code enablePreemptiveAuthentication}, {@code put(url, stream)}). For a single file upload Sardine
 * sends one HTTP/1.1 {@code PUT <outboundUrl>/<filename>} with a preemptive
 * {@code Authorization: Basic} header and {@code Expect: 100-continue}, and treats any status
 * outside 2xx as a failure. This class sends the same request with the JDK {@code HttpClient}
 * (ADR 015 implementation notes): Sardine 5.7 needs the {@code javax.xml.bind} API on the classpath
 * (§12 forbids {@code javax.*}) and Apache HttpClient 4. The error messages are the JEE ones, so a
 * failed export records the same {@code errorMessage}.
 */
@Component
@EnableConfigurationProperties(XhibitWebDavProperties.class)
public class XhibitService {

    public static final String SENDING_EXPORT_FILE_ERROR = "CPF01";

    private static final Logger LOGGER = LoggerFactory.getLogger(XhibitService.class);

    private final XhibitWebDavProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public XhibitService(final XhibitWebDavProperties properties) {
        this(properties, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build());
    }

    XhibitService(final XhibitWebDavProperties properties, final HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public void sendToXhibit(final byte[] content, final String fileName) throws ExportFailedException {
        LOGGER.info(format("Listing: Sending file '%s' to Xhibit", fileName));
        try {
            exportFile(fileName, content);
            LOGGER.info(format("Listing: File '%s' successfully exported to Xhibit", fileName));
        } catch (final ExportFailedException e) {
            final String message = format("Failed to send to Xhibit,%s, for document with name: %s", e.getMessage(), fileName);
            LOGGER.error(message);
            throw new ExportFailedException(message, e.getCause());
        }
    }

    private void exportFile(final String filename, final byte[] content) throws ExportFailedException {
        final String outboundUrl = toUrl(properties.getOutboundUrl(), filename);
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(outboundUrl))
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                    .expectContinue(true)
                    .header("Authorization", basicAuthorization())
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
        } catch (final IllegalArgumentException e) {
            throw new ExportFailedException(format("%s: Failed to put file '%s' to '%s'", SENDING_EXPORT_FILE_ERROR, filename, outboundUrl), e);
        }
        try {
            final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            final int status = response.statusCode();
            if (status < 200 || status >= 300) {
                final String errorMessage = format("%s: Failed to put file '%s' to '%s'. Response status: %s",
                        SENDING_EXPORT_FILE_ERROR, filename, outboundUrl, status);
                LOGGER.error(errorMessage);
                throw new ExportFailedException(errorMessage, new IOException("Unexpected response (" + status + ")"));
            }
        } catch (final IOException e) {
            final String errorMessage = format("%s: Failed to put file '%s' to '%s'", SENDING_EXPORT_FILE_ERROR, filename, outboundUrl);
            LOGGER.error(errorMessage, e);
            throw new ExportFailedException(errorMessage, e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            final String errorMessage = format("%s: Failed to put file '%s' to '%s'", SENDING_EXPORT_FILE_ERROR, filename, outboundUrl);
            throw new ExportFailedException(errorMessage, e);
        }
    }

    private String basicAuthorization() {
        final String credentials = properties.getUser() + ":" + properties.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(UTF_8));
    }

    /** {@code UrlFactory.toUrl}: the outbound URL, a '/' if it has none, then the file name. */
    static String toUrl(final String baseUrl, final String filename) {
        final StringBuilder newUrl = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/")) {
            newUrl.append("/");
        }
        return newUrl.append(filename).toString();
    }
}
