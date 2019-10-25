package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.ExportFailedException;

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

    @Mock
    private Logger LOGGER;

    @Mock
    private XhibitService xhibitService;

    @Mock
    private PublishCourtListCommandSender publishCourtListCommandSender;

    @Spy
    @InjectMocks
    CourtListEventProcessor courtListEventProcessor;

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
        verify(LOGGER).error(format("Export failed for %s %s %s",documentId, documentName, "Not reachable"));

    }

    private class ExportFailedException2 extends ExportFailedException {
        ExportFailedException2() {
            super("Not reachable", new Throwable());
        }
    }
}