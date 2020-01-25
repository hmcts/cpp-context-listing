package uk.gov.moj.cpp.listing.common.xhibit;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;

import java.io.InputStream;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class XhibitServiceTest {

    @Mock
    private Logger logger;

    @Mock
    private FileRetriever fileRetriever;

    @Mock
    private FileReference fileReference;

    @Mock
    private InputStream content;

    @Mock
    private XhibitSession ftpsSession;

    @Mock
    private XhibitSessionFactory xhibitSessionFactory;

    @Mock
    private Exception1 exception;

    @InjectMocks
    @Spy
    private DefaultXhibitService xhibitService;

    @Test
    public void shouldSendExportFileToXhibit() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final String documentName = "Xhibit_000001_20161212121212.xml";

        when(fileRetriever.retrieve(documentId)).thenReturn(of(fileReference));
        when(fileReference.getContentStream()).thenReturn(content);
        when(xhibitSessionFactory.createSession()).thenReturn(ftpsSession);

        xhibitService.sendToXhibit(documentId, documentName);

        final InOrder inOrder = inOrder(ftpsSession, logger, fileRetriever, xhibitSessionFactory);

        inOrder.verify(fileRetriever).retrieve(documentId);
        inOrder.verify(logger).info("Listing: Sending file 'Xhibit_000001_20161212121212.xml' to Xhibit");
        inOrder.verify(xhibitSessionFactory).createSession();
        inOrder.verify(ftpsSession).exportFile(documentName, content);
        inOrder.verify(logger).info("Listing: File 'Xhibit_000001_20161212121212.xml' successfully exported to Xhibit");
    }

    @Test
    public void shouldFailExportFileToXhibitWhenFileNotRetrievable() throws Exception {

        final UUID documentId = UUID.randomUUID();
        final String documentName = "Xhibit_000001_20161212121212.xml";
        when(xhibitSessionFactory.createSession()).thenReturn(ftpsSession);
        when(fileRetriever.retrieve(documentId)).thenThrow(Exception1.class);
        final InOrder inOrder = inOrder(logger);
        try {
            xhibitService.sendToXhibit(documentId, documentName);
            fail();
        } catch (final ExportFailedException e) {
            inOrder.verify(logger).error(format("Failed to retrieve file from File Service, %s, %s", "File not found", documentId));
        }
    }

    @Test
    public void shouldFailExportFileToXhibitWhenExhibitNotReachable() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final String documentName = "Xhibit_000001_20161212121212.xml";
        when(fileRetriever.retrieve(documentId)).thenReturn(of(fileReference));
        when(fileReference.getContentStream()).thenReturn(content);
        when(xhibitSessionFactory.createSession()).thenReturn(ftpsSession);
        doThrow(Exception2.class).when(ftpsSession).exportFile(documentName, content);
        final InOrder inOrder = inOrder(logger);

        try {
            xhibitService.sendToXhibit(documentId, documentName);
            fail();
        } catch (final ExportFailedException e) {
            final String errorMessage = format("Failed to send to Xhibit,%s, for document with id: %s and name: %s", "Not reachable", documentId, documentName);
            assertThat(e.getMessage(), is(errorMessage));
            inOrder.verify(logger).info("Listing: Sending file 'Xhibit_000001_20161212121212.xml' to Xhibit");
            inOrder.verify(logger).error(errorMessage);
        }
    }

    @Test
    public void shouldSendExportFileToXhibitWithInputStream() throws Exception {

        final String documentName = "Xhibit_000001_20181212121211.xml";

        when(fileReference.getContentStream()).thenReturn(content);
        when(xhibitSessionFactory.createSession()).thenReturn(ftpsSession);

        xhibitService.sendToXhibit(content, documentName);

        final InOrder inOrder = inOrder(ftpsSession, logger, fileRetriever, xhibitSessionFactory);

        inOrder.verify(xhibitSessionFactory).createSession();
        inOrder.verify(ftpsSession).exportFile(documentName, content);
        inOrder.verify(logger).info("Listing: File '" + documentName + "' successfully exported to Xhibit");

    }

    @Test
    public void shouldFailExportFileToXhibitWithInputStreamWhenExhibitNotReachable() throws Exception {

        final String documentName = "Xhibit_000001_20161212121212.xml";

        when(fileReference.getContentStream()).thenReturn(content);
        when(xhibitSessionFactory.createSession()).thenReturn(ftpsSession);
        doThrow(Exception2.class).when(ftpsSession).exportFile(documentName, content);
        final InOrder inOrder = inOrder(logger);

        try {
            xhibitService.sendToXhibit(content, documentName);
            fail();
        } catch (final ExportFailedException e) {
            final String errorMessage = format("Failed to send to Xhibit,%s, for document with name: %s", "Not reachable", documentName);
            assertThat(e.getMessage(), is(errorMessage));
            inOrder.verify(logger).info("Listing: Sending file '" + documentName + "' to Xhibit");
            inOrder.verify(logger).error(errorMessage);
        }
    }

    private class Exception1 extends FileServiceException {

        public Exception1(final String message,
                          final Throwable cause) {
            super(message, cause);
        }

        @Override
        public String getMessage() {
            return "File not found";
        }
    }

    private class Exception2 extends ExportFailedException {

        public Exception2(final String message,
                          final Throwable cause) {
            super(message, cause);
        }

        @Override
        public String getMessage() {
            return "Not reachable";
        }
    }
}