package uk.gov.moj.cpp.listing.common.xhibit;

import static java.lang.String.*;
import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;

import com.github.sardine.Sardine;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;

public class XhibitSession implements AutoCloseable {
    public static final String SENDING_EXPORT_FILE_ERROR = "CPF01";

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    private final Sardine client;
    private final URL outbound;

    public XhibitSession(final URL outbound, final Sardine client) {
        this.outbound = outbound;
        this.client = client;
    }

    @Override
    public void close() throws IOException {
        client.shutdown();
    }

    @SuppressWarnings({"squid:S1166", "squid:S1162", "squid:S2139"})
    public void exportFile(final String filename, final InputStream content) throws ExportFailedException {

        final String outboundUrl = new UrlFactory().toUrl(outbound, filename);
        try {
            client.put(outboundUrl, content);
        } catch (final HttpResponseException e) {
            final String errorMessage = format("%s: Failed to put file '%s' to '%s'. Response status: %s",
                    SENDING_EXPORT_FILE_ERROR, filename, outboundUrl, e.getStatusCode());
            logger.error(errorMessage, e.getMessage());
            throw new ExportFailedException(errorMessage, e);
        } catch (final IOException e) {
            final String errorMessage = format("%s: Failed to put file '%s' to '%s'",
                    SENDING_EXPORT_FILE_ERROR, filename, outboundUrl);
            logger.error(errorMessage, e.getMessage());
            throw new ExportFailedException(errorMessage, e);
        }
    }


    public Sardine getClient() {
        return client;
    }

    public URL getOutbound() {
        return outbound;
    }

}
