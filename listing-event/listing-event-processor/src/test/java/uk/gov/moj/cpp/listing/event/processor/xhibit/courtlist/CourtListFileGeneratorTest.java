package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.ZonedDateTime.parse;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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
    private static final String DAILY_COURT_LIST_SUMMER_TIME_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-daily-list-summer-time.json";
    private static final String WEEK_COMMENCING_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-week-commencing-list.json";
    private static final String RESTRICTED_DAILY_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-restricted-daily-list.json";
    private static final String COURT_LIST_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-with-fixed-date-corresponding-hearing-types-list.json";
    private static final String COURT_LIST_2_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist2-with-fixed-date-corresponding-hearing-types-list.json";
    private static final String COURT_LIST_WITH_CASE_HIDDEN_INPUT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-case-hidden.json";


    @Parameterized.Parameter(0)
    public PublishCourtListType publishCourtListType;
    @Parameterized.Parameter(1)
    public String courtListJsonFile;
    @Parameterized.Parameter(2)
    public String expectedXmlFile;
    private UUID courtCentreId1 = fromString("f34a5dba-8c4b-4ec8-8b9a-6af405c00ebf");
    private UUID courtCentreId2 = fromString("f46ddec0-928e-4236-9d1b-142715e8b570");
    private LocalDate startDate = LocalDate.parse("2019-11-04");

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

    private JsonObject courtListJson;

    @Parameterized.Parameters(name = "{index}: Test with PublishCourtListType={0}, expectedXmlFile is:{2} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {WARN, WEEK_COMMENCING_COURT_LIST_JSON_FILE, "xhibit/expectedWarnedList.xml"},
                {DRAFT, DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedDraftList.xml"},
                {DRAFT, DAILY_COURT_LIST_SUMMER_TIME_JSON_FILE, "xhibit/expectedDraftListSummerTime.xml"},
                {FINAL, DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedFinalList.xml"},
                {FIRM, WEEK_COMMENCING_COURT_LIST_JSON_FILE, "xhibit/expectedFirmList.xml"},
                {FINAL, RESTRICTED_DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedRestrictedFinalList.xml"},
                {WARN, COURT_LIST_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE, "xhibit/expectedWarnedListWithDifferentHearingTypesInCase.xml"},
                {WARN, COURT_LIST_2_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE, "xhibit/expectedWarnedListWithMultipleHearingTypesInCase.xml"},
                {WARN, COURT_LIST_WITH_CASE_HIDDEN_INPUT_LIST_JSON_FILE, "xhibit/expectedWarnedListWithCaseHidden.xml"}

        };
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

        final String crestCourtId = "000";
        final CourtLocation courtLocation1 = createCourtLocation(crestCourtId, "1");
        final CourtLocation courtLocation2 = createCourtLocation(crestCourtId, "2");

        final List<UUID> courtCentreIds = Arrays.asList(courtCentreId1, courtCentreId2);

        when(xhibitReferenceDataService.getCourtDetails(courtCentreId1)).thenReturn(courtLocation1);
        when(xhibitReferenceDataService.getCourtDetails(courtCentreId2)).thenReturn(courtLocation2);
        when(xhibitReferenceDataService.getCourtCentreIdsForCrestId(envelope, crestCourtId)).thenReturn(courtCentreIds);
        when(xhibitReferenceDataService.getCourtRoomNumber(courtCentreId1,UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(10);
        when(xhibitReferenceDataService.getCourtRoomNumber(courtCentreId1,UUID.fromString("6508af42-e4d4-396d-a752-d676ebd38f6d"))).thenReturn(20);
        when(xhibitReferenceDataService.getCourtRoomNumber(courtCentreId1,UUID.fromString("64b0f4cf-2dde-310b-b7da-cab57b285b6f"))).thenReturn(4);
        when(xhibitReferenceDataService.getCourtRoomNumber(courtCentreId1,UUID.fromString("28813316-35dc-30b7-a94f-07aeec664d9f"))).thenReturn(3);
        when(xhibitReferenceDataService.getCourtRoomNumber(courtCentreId1,UUID.fromString("1f9630dc-e4ba-3378-8880-2369883394b2"))).thenReturn(1);

        final JsonObject judiciary = givenPayload("/xhibit/mock-data/referencedata.query.judiciaries.json");
        when(xhibitReferenceDataService.getJudiciary(any(), any())).thenReturn(judiciary);

        final JsonObject hearingType = Json.createObjectBuilder()
                .add("exhibitHearingCode", "TRL")
                .add("exhibitHearingDescription", "XHIBIT_HEARING_DESCRIPTION-TRL")
                .build();

        final JsonObject hearingType1 = Json.createObjectBuilder()
                .add("exhibitHearingCode", "PTP")
                .add("exhibitHearingDescription", "XHIBIT_HEARING_DESCRIPTION-PTP")
                .build();

        final JsonObject hearingType2 = Json.createObjectBuilder()
                .add("exhibitHearingCode", "SBT")
                .add("exhibitHearingDescription", "XHIBIT_HEARING_DESCRIPTION-PTP")
                .build();

        final UUID hearingTypeId = UUID.fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4");
        when(xhibitReferenceDataService.getXhibitHearingType(any(), any())).thenReturn(hearingType);

        when(xhibitReferenceDataService.getXhibitHearingType(any(), eq(hearingTypeId))).thenReturn(hearingType);

        final UUID hearingTypeId1 = UUID.fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced");

        when(xhibitReferenceDataService.getXhibitHearingType(any(), eq(hearingTypeId1))).thenReturn(hearingType1);

        final UUID hearingTypeId2 = UUID.fromString("c6b0c2bf-3f98-46ed-ab7e-56efaf9ecceb");

        when(xhibitReferenceDataService.getXhibitHearingType(any(), eq(hearingTypeId2))).thenReturn(hearingType2);

        courtListJson = givenPayload(courtListJsonFile);

        when(listingService.getPublishedCourtListForCourtCentre(
                envelope,
                courtCentreId2,
                publishCourtListType,
                startDate)).thenReturn(emptyCourtList(courtCentreId2));
    }

    private CourtLocation createCourtLocation(final String crestCourtId, final String nameSuffix) {
        return new CourtLocation(
                "OUCODE",
                crestCourtId,
                "00" + nameSuffix,
                "MOCK_CROWN_COURTNAME",
                "MOCK",
                "MOCKCOURTNAME" + nameSuffix,
                "MOCKSITECODE" + nameSuffix,
                "CROWN_COURT");
    }

    private JsonObject emptyCourtList(final UUID courtCentreId) {
        return Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("crestCourtSite", crestCourtSite())
                                        .add("sittings", Json.createArrayBuilder().build())
                                        .build()
                        ).build()
                ).build();
    }

    private JsonObjectBuilder crestCourtSite() {
        return Json.createObjectBuilder()
                .add("crestCourtSiteId", "002")
                .add("crestCourtSiteName", "MOCKCOURTNAME2")
                .add("courtType", "CROWN_COURT");
    }

    @Test
    public void shouldGenerateXml() throws Exception {

        final PublishCourtListRequestParameters requestParameters = withDefaults()
                .withCourtCentreId(courtCentreId1)
                .publishCourtListType(publishCourtListType)
                .withStartDate(startDate)
                .build();

        final CourtListMetadata metadata = new CourtListMetadata(publishCourtListType.name() + "-FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        final String generatedXml = courtListFileGenerator.generateXml(envelope, requestParameters, metadata, courtListJson);

        LOGGER.info("generatedXml:\n{}", generatedXml);

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + publishCourtListType.getSchemaName());

        assertXmlEquals(generatedXml, expectedXmlFile);
    }

}
