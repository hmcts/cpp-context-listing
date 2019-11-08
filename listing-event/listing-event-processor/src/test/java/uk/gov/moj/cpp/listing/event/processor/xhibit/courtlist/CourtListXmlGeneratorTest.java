package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;

import uk.gov.moj.cpp.listing.domain.xhibit.XhibitCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.transform.CourtServicesTransformer;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.transform.FirmListTransformer;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.GenerationFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

@RunWith(MockitoJUnitRunner.class)
public class CourtListXmlGeneratorTest {

    @InjectMocks
    private CourtListXmlGenerator courtListXmlGenerator;

    @Spy
    private FirmListTransformer firmListTransformer;

    @Spy
    private CourtServicesTransformer courtServicesTransformer;

    @Spy
    private XmlConverter xmlConverter;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void wireBeans() {
        firmListTransformer.setCourtServicesTransformer(courtServicesTransformer);
    }

    @Test
    public void shouldGenerateFirmListXml() throws Exception {

        final PublishCourtListRequestParameters requestParameters = withDefaults()
                .withXhibitCourtListType(XhibitCourtListType.FIRM)
                .build();

        final CourtListMetadata metadata = new CourtListMetadata("FILENAME", "UNIQUEID");

        final CourtListGenerationContext context = new CourtListGenerationContext(requestParameters, metadata);

        final String actualXml = courtListXmlGenerator.getXmlString(context);

        assertXmlEquals(actualXml, "xhibit/firmList.xml");
    }

    @Test
    public void shouldThrowExceptionIfXmlConversionFails() throws Exception {

        expectedException.expect(GenerationFailedException.class);
        expectedException.expectMessage("Court list XML generation for XHIBIT failed");

        final CourtListMetadata metadata = mock(CourtListMetadata.class);

        final CourtListGenerationContext context = new CourtListGenerationContext(withDefaults().build(), metadata);

        when(xmlConverter.convertToXml(any())).thenThrow(JAXBException.class);

        courtListXmlGenerator.getXmlString(context);
    }

    private void assertXmlEquals(final String actualXml, final String expectedXmlResourceName) throws IOException {

        System.out.println(actualXml);

        final String expectedXml = loadResourceFile(expectedXmlResourceName);

        final Diff xmlDiff = DiffBuilder.compare(expectedXml).withTest(actualXml).build();

        final Iterator<Difference> iter = xmlDiff.getDifferences().iterator();
        int size = 0;
        while (iter.hasNext()) {
            System.out.println(iter.next().toString());
            size++;
        }
        assertThat("XML differences", size, is(0));
    }

    private String loadResourceFile(final String resourceName) throws IOException {
        try (final InputStream configurationStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(configurationStream);
        }
    }
}
