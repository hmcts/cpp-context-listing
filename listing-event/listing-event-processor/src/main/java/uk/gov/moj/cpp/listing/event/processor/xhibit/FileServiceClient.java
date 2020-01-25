package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

public class FileServiceClient {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private FileStorer fileStorer;

    @Inject
    private FileServiceStreamFactory fileServiceStreamFactory;

    public UUID store(final CourtListMetadata metadata, final String fileContent) throws FileServiceException {

        logger.info("store: metadata={}", metadata);

        final JsonObject fileServiceMetadata = createObjectBuilder()
                .add("filename", metadata.getFilename())
                .add("documentUniqueId", metadata.getDocumentUniqueId())
                .build();

        return fileStorer.store(fileServiceMetadata, fileServiceStreamFactory.buildStream(fileContent));
    }
}
