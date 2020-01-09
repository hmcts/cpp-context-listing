package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.ZonedDateTime.parse;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.DRAFT;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FINAL;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlTestUtils.assertXmlEquals;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;

import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;

@RunWith(Parameterized.class)
public class CourtListFileGeneratorTest {

    private static final Logger LOGGER = getLogger(CourtListFileGeneratorTest.class);
    private static final String DAILY_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-daily-list.json";
    private static final String WEEK_COMMENCING_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-week-commencing-list.json";
    private static final String RESTRICTED_DAILY_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-restricted-daily-list.json";

    @Parameterized.Parameter(0)
    public PublishCourtListType publishCourtListType;

    @Parameterized.Parameter(1)
    public String courtListJsonFile;

    @Parameterized.Parameter(2)
    public String expectedXmlFile;
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
    @Spy
    private MapperFactory mapperFactory;

    @Parameterized.Parameters(name = "{index}: Test with PublishCourtListType={0}, expectedXmlFile is:{2} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {WARN, WEEK_COMMENCING_COURT_LIST_JSON_FILE, "xhibit/expectedWarnedList.xml"},
                {DRAFT, DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedDraftList.xml"},
                {FINAL, DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedFinalList.xml"},
                {FIRM, WEEK_COMMENCING_COURT_LIST_JSON_FILE, "xhibit/expectedFirmList.xml"},
                {FINAL, RESTRICTED_DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedRestrictedFinalList.xml"}};
        return asList(data);
    }

    @Before
    public void initialiseLogger() {
        BasicConfigurator.configure();
    }

    @Before
    public void wireBeans() {
        xmlUtils.setLogger(logger);
        xmlUtils.postConstruct();
        mapperFactory.setXhibitReferenceDataService(xhibitReferenceDataService);
    }

    @Before
    public void mockDataSources() {

        MockitoAnnotations.initMocks(this);

        final CourtLocation courtLocation = new CourtLocation("000",
                                                              "001",
                                                              "MOCK_CROWN_COURTNAME",
                                                              "MOCK",
                                                              "MOCKCOURTNAME",
                                                              "MOCKSITECODE",
                                                              "CROWN_COURT");

        when(xhibitReferenceDataService.getCourtDetails(any(), any())).thenReturn(courtLocation);
        when(xhibitReferenceDataService.getCourtLocationsForCourt(any(), any())).thenReturn(asList(courtLocation));

        final JsonObject judiciary = givenPayload("/xhibit/mock-data/referencedata.query.judiciaries.json");
        when(xhibitReferenceDataService.getJudiciary(any(), any())).thenReturn(judiciary);

        final JsonObject hearingType = Json.createObjectBuilder()
                .add("hearingCode", "XXX")   // TODO SCSL-85
                .add("hearingDescription", "XHIBIT_HEARING_DESCRIPTION")
                .build();
        when(xhibitReferenceDataService.getXhibitHearingType(any(), any())).thenReturn(hearingType);

        final JsonObject courtListData = givenPayload(courtListJsonFile);
        when(listingService.getPublishedCourtListForCourtCentre(any(), any())).thenReturn(courtListData);
    }

    @Test
    public void shouldGenerateXml() throws Exception {

        final PublishCourtListRequestParameters requestParameters = withDefaults()
                .publishCourtListType(publishCourtListType)
                .build();

        final CourtListMetadata metadata = new CourtListMetadata(publishCourtListType.name() + "-FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        final String generatedXml = courtListFileGenerator.generateXml(envelope, requestParameters, metadata);

        LOGGER.info("generatedXml:\n{}", generatedXml);

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + publishCourtListType.getSchemaName());

        assertXmlEquals(generatedXml, expectedXmlFile);
    }

}
