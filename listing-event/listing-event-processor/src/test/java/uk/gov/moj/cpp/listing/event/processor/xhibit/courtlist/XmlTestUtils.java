package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

public class XmlTestUtils {

    public static void assertXmlEquals(final String actualXml, final String expectedXmlResourceName) throws IOException {
        assertXmlEquals(actualXml, expectedXmlResourceName, Collections.emptyMap());
    }

    public static void assertXmlEquals(final String actualXml, final String expectedXmlResourceName,
                                       final Map<String, String> replaceables) throws IOException {

        String expectedXml = loadResourceFile(expectedXmlResourceName);
        Iterator<Map.Entry<String, String>> iterator = replaceables.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            expectedXml = expectedXml.replaceAll(next.getKey(), next.getValue());
        }

        final Diff xmlDiff = DiffBuilder.compare(expectedXml).withTest(actualXml).build();

        final Iterator<Difference> iter = xmlDiff.getDifferences().iterator();
        int size = 0;
        while (iter.hasNext()) {
            iter.next();
            size++;
        }
        assertThat("XML differences", size, is(0));
    }

    public static String loadResourceFile(final String resourceName) throws IOException {
        try (final InputStream configurationStream = XmlTestUtils.class.getClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(configurationStream);
        }
    }
}
