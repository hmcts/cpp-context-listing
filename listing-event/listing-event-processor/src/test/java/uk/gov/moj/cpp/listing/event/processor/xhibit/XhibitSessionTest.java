package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.ExportFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.github.sardine.Sardine;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class XhibitSessionTest {

    @Mock
    private Sardine client;

    @Mock
    private Logger logger;

    @Mock
    private InputStream fileContents;

    private XhibitSession xhibitSession;

    private XhibitSession xhibitSessionSpy;

    private XhibitSession xhibitSessionWithTrailingSlash;

    @Before
    public void createClassAndMocks() throws Exception {
        xhibitSession = new XhibitSession(new URL("http://outbound.com"), client);
        xhibitSessionSpy = spy(xhibitSession);
        xhibitSessionWithTrailingSlash = new XhibitSession(new URL("http://outbound.com"), client);
    }


    @Test
    public void shouldPutFileToOutboundUrlOnArchive() throws Exception {

        final String filename = "filename";
        xhibitSession.exportFile(filename, fileContents);

        verify(client).put("http://outbound.com/filename", fileContents);
    }

    @Test
    public void shouldHandleTrailingSlashWhenPuttingFileToOutboundUrlOnArchive() throws Exception {

        final String filename = "filename";
        xhibitSession.exportFile(filename, fileContents);

        verify(client).put("http://outbound.com/filename", fileContents);
    }

    @Test
    public void shouldThrowAExportToLibraFailedExceptionIfExportingFileThrowsAnHttpResponseException() throws Exception {

        final HttpResponseException httpResponseException = new HttpResponseException(404, "Ooops");
        final String filename = "filename";

        final String expectedMessage = "CPF01: Failed to put file 'filename' " +
                "to 'http://outbound.com/filename'. " +
                "Response status: 404";

        doThrow(httpResponseException).when(client).put("http://outbound.com/filename", fileContents);
        setField(xhibitSessionSpy,"logger",logger);
        try {
            xhibitSessionSpy.exportFile(filename, fileContents);
            fail();
        } catch (final ExportFailedException expected) {
            assertThat(expected.getCause(), is(httpResponseException));
            assertThat(expected.getMessage(), is(expectedMessage));
        }

        verify(logger).error(expectedMessage, httpResponseException.getMessage());
    }

    @Test
    public void shouldThrowAExportToLibraFailedExceptionIfExportingFileThrowsAnIOException() throws Exception {

        final IOException ioException = new IOException("Ooops");
        final String filename = "filename";

        final String expectedMessage = "CPF01: Failed to put file 'filename' to 'http://outbound.com/filename'";
        setField(this.xhibitSessionSpy,"logger",logger);

        doThrow(ioException).when(client).put("http://outbound.com/filename", fileContents);

        try {
            xhibitSessionSpy.exportFile(filename, fileContents);
            fail();
        } catch (final ExportFailedException expected) {
            assertThat(expected.getCause(), is(ioException));
            assertThat(expected.getMessage(), is(expectedMessage));
        }

        verify(logger).error(expectedMessage, ioException.getMessage());
    }

    @Test
    public void shouldCloseTheClient() throws Exception {

        xhibitSession.close();

        verify(client).shutdown();
    }
}