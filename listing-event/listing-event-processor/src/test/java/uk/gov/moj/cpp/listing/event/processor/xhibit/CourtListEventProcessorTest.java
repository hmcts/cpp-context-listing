package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListXmlGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersParser;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.ExportFailedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class CourtListEventProcessorTest {
    private static final String PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED = "listing.event.publish-court-list-produced";
    @Spy
    @InjectMocks
    CourtListEventProcessor courtListEventProcessor;
    @Mock
    private Logger LOGGER;
    @Mock
    private XhibitService xhibitService;
    @Mock
    private PublishCourtListCommandSender publishCourtListCommandSender;
    @Mock
    private PublishCourtListRequestParametersParser publishCourtListRequestParametersParser;
    @Mock
    private CourtListMetadataGenerator courtListMetadataGenerator;
    @Mock
    private CourtListXmlGenerator courtListXmlGenerator;
    @Mock
    private FileServiceClient fileServiceClient;


    @Test
    public void shouldHandlePublishCourtListRequested() throws Exception {

        // Mocked values
        final JsonEnvelope tEnvelope = mock(JsonEnvelope.class);
        final UUID generatedDocumentId = randomUUID();
        final ByteArrayInputStream mockFileContentStream = mock(ByteArrayInputStream.class);
        final PublishCourtListRequestParameters parameters = mock(PublishCourtListRequestParameters.class);
        final CourtListMetadata courtListMetadata = new CourtListMetadata("TESTFILENAME", "UNIQUE_ID");

        when(publishCourtListRequestParametersParser.parse(tEnvelope)).thenReturn(parameters);
        when(courtListMetadataGenerator.generate(parameters)).thenReturn(courtListMetadata);
        when(courtListXmlGenerator.generateCourtListInputStream(tEnvelope, parameters, courtListMetadata)).thenReturn(mockFileContentStream);
        when(fileServiceClient.store(courtListMetadata, mockFileContentStream)).thenReturn(generatedDocumentId);

        // Tested method
        courtListEventProcessor.handlePublishCourtListRequested(tEnvelope);

        // Assertions
        verify(fileServiceClient).store(courtListMetadata, mockFileContentStream);
        verify(publishCourtListCommandSender).recordCourtListPublished(generatedDocumentId, courtListMetadata.getFilename());
    }

    @Test
    public void handleProducedCourtListWhenExportSuccessful() throws ExportFailedException {
        final UUID documentId = randomUUID();
        final String documentName = "documentName";
        final JsonObject payload = createObjectBuilder().add("documentId", documentId.toString())
                .add("documentName", documentName).build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED)
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, payload);
        courtListEventProcessor.handleProducedCourtList(tEnvelope);
        verify(xhibitService).sendToXhibit(documentId, documentName);
        verify(publishCourtListCommandSender).recordCourtListExportSuccessful(documentId, documentName);
    }

    @Test
    public void handleProducedCourtListWhenExportFailed() throws ExportFailedException {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED)

                .withUserId(randomUUID().toString()).build();
        final UUID documentId = randomUUID();
        final String documentName = "documentName";
        doThrow(new ExportFailedException2()).when(xhibitService).sendToXhibit(documentId, documentName);
        final JsonObject payload = createObjectBuilder().add("documentId", documentId.toString())
                .add("documentName", documentName)
                .build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, payload);
        courtListEventProcessor.handleProducedCourtList(tEnvelope);
        verify(xhibitService).sendToXhibit(documentId, documentName);
        verify(publishCourtListCommandSender).recordCourtListExportFailed(documentId, documentName, "Not reachable");
        verify(LOGGER).error(format("Export failed for %s %s %s", documentId, documentName, "Not reachable"));

    }

    private class ExportFailedException2 extends ExportFailedException {
        ExportFailedException2() {
            super("Not reachable", new Throwable());
        }
    }
}
