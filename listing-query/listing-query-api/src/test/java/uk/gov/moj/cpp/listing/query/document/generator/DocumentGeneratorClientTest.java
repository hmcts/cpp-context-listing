package uk.gov.moj.cpp.listing.query.document.generator;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.listing.query.document.generator.exception.DocumentGenerationFailedException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentGeneratorClientTest {

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;
    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient documentGeneratorClient;

    @Mock
    private JsonObject documentPayload;

    @InjectMocks
    private DocumentGeneratorClient client;

    @Test
    public void shouldGenerateDocument() throws IOException {
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(JsonObject.class), any(String.class), any(UUID.class))).thenReturn(new byte[10]);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));
        byte[] bytes = client.generateDocument(documentPayload, STRING.next());
        Assertions.assertNotNull(bytes);
    }

    @Test
    public void shouldGenerateDocumentThrowException() throws IOException {
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(JsonObject.class), any(String.class), any(UUID.class))).thenThrow(new IOException());
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));
        assertThrows(DocumentGenerationFailedException.class, () -> client.generateDocument(documentPayload, STRING.next()));
    }

}
