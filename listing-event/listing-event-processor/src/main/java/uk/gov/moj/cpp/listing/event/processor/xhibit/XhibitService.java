package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.ExportFailedException;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;

public class XhibitService {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;
    @Inject
    private FileRetriever fileRetriever;

    @Inject
    private XhibitSessionFactory xhibitSessionFactory;

    @SuppressWarnings({"squid:S1166", "squid:S1162", "squid:S2139", "squid:S2629"})
    public void sendToXhibit(final UUID documentId, final String documentName) throws ExportFailedException {
        logger.info(format("Listing: Sending file '%s' to Xhibit", documentName));
        XhibitSession xhibitSession = null;
        try {
            xhibitSession = xhibitSessionFactory.createSession();
            final FileReference fileReference = retrieveFile(documentId);
            xhibitSession.exportFile(documentName, fileReference.getContentStream());
            logger.info(format("Listing: File '%s' successfully exported to Xhibit", documentName));
        } catch (final ExportFailedException e) {
            final String message = format("Failed to send to Xhibit,%s, for document with id: %s and name: %s", e.getMessage(), documentId, documentName);
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
    private FileReference retrieveFile(final UUID documentId) throws ExportFailedException {
        try {
            return fileRetriever.retrieve(documentId).orElseThrow(NotFoundException::new);
        } catch (final FileServiceException e) {
            final String message = format("Failed to retrieve file from File Service, %s, %s", e.getMessage(), documentId);
            logger.error(message);
            throw new ExportFailedException(message, e.getCause());
        }
    }

}
