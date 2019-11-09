package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.ZonedDateTime.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate.CourtServicesGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate.FirmListGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

@RunWith(MockitoJUnitRunner.class)
public class CourtListFileGeneratorTest {

    @InjectMocks
    private CourtListFileGenerator courtListFileGenerator;

    @Spy
    private FirmListGenerator firmListGenerator;

    @Spy
    private CourtServicesGenerator courtServicesGenerator;

    @Spy
    private XmlUtils xmlUtils;

    @Mock
    private XhibitReferenceDataService xhibitReferenceDataService;

    @Before
    public void wireBeans() {
        firmListGenerator.setCourtServicesGenerator(courtServicesGenerator);
        courtServicesGenerator.setXhibitReferenceDataService(xhibitReferenceDataService);
    }

    @Before
    public void mockDataSources() {
        final CourtLocation courtLocation = new CourtLocation("000", "MOCKCOURTNAME", "MOCK", "MOCKSITECODE");

        when(xhibitReferenceDataService.getCourtDetails(any(), any())).thenReturn(courtLocation);
    }

    @Test
    public void shouldGenerateFirmListXml() throws Exception {

        final PublishCourtListRequestParameters requestParameters = withDefaults()
                .publishCourtListType(PublishCourtListType.FIRM)
                .build();

        final CourtListMetadata metadata = new CourtListMetadata("FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        final CourtListGenerationContext context = new CourtListGenerationContext(mock(JsonEnvelope.class), requestParameters, metadata);

        final String actualXml = courtListFileGenerator.getXmlString(context);

        assertXmlEquals(actualXml, "xhibit/firmList.xml");
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
