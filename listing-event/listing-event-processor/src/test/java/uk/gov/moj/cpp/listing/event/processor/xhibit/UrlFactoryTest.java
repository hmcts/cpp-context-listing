package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class UrlFactoryTest {

    @InjectMocks
    private UrlFactory urlFactory;

    @Test
    public void shouldCreateAUrlFromTheUrlSpecString() throws Exception {

        final String spec = "http://www.google.com";

        final URL url = urlFactory.create(spec);

        assertThat(url.getProtocol(), is("http"));
        assertThat(url.getHost(), is("www.google.com"));
    }

    @Test
    public void shouldThrowARuntimeExceptionIfTheUrlIsMalformed() throws Exception {

        try {
            urlFactory.create("something silly");
            fail();
        } catch (final RuntimeException expected) {
            assertThat(expected.getMessage(), is("Failed to create url from 'something silly'"));
            assertThat(expected.getCause(), is(instanceOf(MalformedURLException.class)));
        }
    }

    @Test
    public void shouldCreateAUrlStringWithAppendedFilename() throws Exception {

        final URL url = new URL("http://www.google.com");

        assertThat(urlFactory.toUrl(url, "robots.txt"), is("http://www.google.com/robots.txt"));
    }

    @Test
    public void shouldAppendASlashWhenCreatingUrlFromFilenameIfRequired() throws Exception {

        final URL url = new URL("http://www.google.com/");

        assertThat(urlFactory.toUrl(url, "robots.txt"), is("http://www.google.com/robots.txt"));
    }
}
