package uk.gov.moj.cpp.listing.common.xhibit;

import static java.lang.String.format;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;

@ApplicationScoped
public class XhibitService {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;
    @Inject
    private FileRetriever fileRetriever;

    @Inject
    private XhibitSessionFactory xhibitSessionFactory;

    @SuppressWarnings({"squid:S1166", "squid:S1162", "squid:S2139", "squid:S2629"})
    public void sendToXhibit(final UUID fileId, final String fileName) throws ExportFailedException {
        logger.info(format("Listing: Sending file '%s' to Xhibit", fileName));
        XhibitSession xhibitSession = null;
        try {
            xhibitSession = xhibitSessionFactory.createSession();
            final FileReference fileReference = retrieveFile(fileId);
            xhibitSession.exportFile(fileName, fileReference.getContentStream());
            logger.info(format("Listing: File '%s' successfully exported to Xhibit", fileName));
        } catch (final ExportFailedException e) {
            final String message = format("Failed to send to Xhibit,%s, for document with id: %s and name: %s", e.getMessage(), fileId, fileName);
            logger.error(message);
            throw new ExportFailedException(message, e.getCause());
        } finally {
            try {
                if (xhibitSession != null) {
                    xhibitSession.close();
                }
            } catch (IOException e) {
                logger.error("Issue with closing sessions", e.getCause());
            }
        }
    }

    @SuppressWarnings({"squid:S1166", "squid:S1162"})
    private FileReference retrieveFile(final UUID fileId) throws ExportFailedException {
        try {
            return fileRetriever.retrieve(fileId).orElseThrow(NotFoundException::new);
        } catch (final FileServiceException e) {
            final String message = format("Failed to retrieve file from File Service, %s, %s", e.getMessage(), fileId);
            logger.error(message);
            throw new ExportFailedException(message, e.getCause());
        }
    }

}
