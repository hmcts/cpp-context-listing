package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.ZonedDateTime.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

@RunWith(MockitoJUnitRunner.class)
public class CourtListFileGeneratorTest {

    private static final Logger LOGGER = getLogger(CourtListFileGeneratorTest.class);

    @InjectMocks
    private CourtListFileGenerator courtListFileGenerator;

    @Spy
    private XmlUtils xmlUtils;

    @Mock
    private XhibitReferenceDataService xhibitReferenceDataService;

    @Mock
    private ListingService listingService;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Logger logger;

    @Before
    public void initialiseLogger() {
        BasicConfigurator.configure();
    }

    @Before
    public void wireBeans() {
        xmlUtils.setLogger(logger);
        xmlUtils.postConstruct();
    }

    @Before
    public void mockDataSources() {
        final CourtLocation courtLocation = new CourtLocation("000", "MOCKCOURTNAME",
                "MOCK", "MOCKSITECODE", "CROWN_COURT");
        when(xhibitReferenceDataService.getCourtDetails(any(), any())).thenReturn(courtLocation);

        final JsonObject judge = givenPayload("/xhibit/mock-data/referencedata.query.get.judge.json");
        when(xhibitReferenceDataService.getJudge(any(), any())).thenReturn(judge);

        final JsonObject judiciary = givenPayload("/xhibit/mock-data/referencedata.query.judiciaries.json");
        when(xhibitReferenceDataService.getJudiciary(any(), any())).thenReturn(judiciary);

        final List<JsonObject> hearings = givenPayload("/xhibit/mock-data/listing.query.search.hearings.json")
                .getJsonArray("hearings").getValuesAs(JsonObject.class);
        when(listingService.getHearingsForPublishing(any(), any())).thenReturn(hearings);
    }

    @Test
    public void shouldGenerateFirmListXml() throws Exception {

        final PublishCourtListRequestParameters requestParameters = withDefaults()
                .publishCourtListType(PublishCourtListType.FIRM)
                .build();

        final CourtListMetadata metadata = new CourtListMetadata("FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        final String generatedXml = courtListFileGenerator.generateXml(envelope, requestParameters, metadata);

        LOGGER.info("generatedXml:\n{}", generatedXml);

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + PublishCourtListType.FIRM.getSchemaName());

        assertXmlEquals(generatedXml, "xhibit/expectedFirmList.xml");
    }

    private void assertXmlEquals(final String actualXml, final String expectedXmlResourceName) throws IOException {

        final String expectedXml = loadResourceFile(expectedXmlResourceName);

        final Diff xmlDiff = DiffBuilder.compare(expectedXml).withTest(actualXml).build();

        final Iterator<Difference> iter = xmlDiff.getDifferences().iterator();
        int size = 0;
        while (iter.hasNext()) {
            LOGGER.info(iter.next().toString());
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
