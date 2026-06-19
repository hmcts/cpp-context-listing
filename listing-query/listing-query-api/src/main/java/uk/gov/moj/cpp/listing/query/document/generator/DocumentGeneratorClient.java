package uk.gov.moj.cpp.listing.query.document.generator;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.listing.query.document.generator.exception.DocumentGenerationFailedException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DocumentGeneratorClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorClient.class);

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private SystemUserProvider systemUserProvider;

    public byte[] generateDocument(final JsonObject documentPayload, final String templateName){
        final UUID systemUserId = systemUserProvider.getContextSystemUserId().orElseThrow(() -> new DocumentGenerationFailedException("Could not find systemId "));
        try {
            return  documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(documentPayload, templateName, systemUserId);
        } catch (IOException e) {
            LOGGER.error("Failed to generate document for template {} with document payload {} ",templateName, documentPayload);
            throw new DocumentGenerationFailedException(e);
        }
    }
}
