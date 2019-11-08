package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;

import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class FileServiceClient {

    @Inject
    private FileStorer fileStorer;

    public UUID store(final CourtListMetadata metadata, final InputStream fileContentStream) throws FileServiceException {

        final JsonObject fileServiceMetadata = createObjectBuilder()
                .add("filename", metadata.getFilename())
                .add("documentUniqueId", metadata.getDocumentUniqueId())
                .build();

        return fileStorer.store(fileServiceMetadata, fileContentStream);
    }
}
