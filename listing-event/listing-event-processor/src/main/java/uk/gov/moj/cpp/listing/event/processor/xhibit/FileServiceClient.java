package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

public class FileServiceClient {

    @Inject
    private FileStorer fileStorer;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    public UUID store(final String filename, final InputStream fileContentStream) throws FileServiceException {
        final JsonObject fileServiceMetadata = createObjectBuilder()
                .add("filename", filename)
                .build();

        final UUID fileId = fileStorer.store(fileServiceMetadata, fileContentStream);

        try {
            fileContentStream.close();
        } catch (IOException e) {
            logger.error("Closing stream", e);
        }

        return fileId;
    }
}
