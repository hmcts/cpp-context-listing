package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.time.ZonedDateTime.parse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.HearingStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlTestUtils;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class FirmListMapperTest {

    @Mock
    private CourtServicesMapper courtServicesMapper;

    @Spy
    private XmlUtils xmlUtils;

    @Mock
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @Mock
    protected JsonEnvelope envelope;
    protected PublishCourtListRequestParameters requestParameters;
    protected CourtListMetadata metadata;
    protected CourtListGenerationContext context;
    @Mock
    private Logger logger;

    @BeforeEach
    public void wireBeans() {
        xmlUtils.setLogger(logger);
        xmlUtils.postConstruct();
    }

    @BeforeEach
    public void mockDataSources() {

        MockitoAnnotations.initMocks(this);

        final CourtLocation crownCourtLocation = new CourtLocation(
                "OUCODE",
                "000",
                "001",
                "MOCK_CROWN_COURTNAME",
                "MOCK",
                "MOCKCOURTNAME",
                "MOCKSITECODE",
                "CROWN_COURT");

        final CourtLocation magsCourtLocation = new CourtLocation(
                "OUCODE",
                "000",
                "001",
                "MOCK_MAGISTRATES_COURTNAME",
                "MOCK",
                "MOCKCOURTNAME",
                "000",
                "MAGISTRATES_COURT");

        requestParameters = withDefaults()
                .build();

        metadata = new CourtListMetadata(requestParameters.getPublishCourtListType().name() + "-FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        context = new CourtListGenerationContext(envelope, requestParameters, metadata);
    }

    @Test
    public void generate() throws Exception {

        SittingStructure sittingStructure = new SittingStructure();
        sittingStructure.setSittingNote("TESTNOTE");

        when(courtServicesMapper.generateSittingStructure(any(JsonObject.class), eq(1))).thenReturn(sittingStructure);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        XmlTestUtils.assertXmlEquals(generatedXml, "xhibit/mapper/expectedFirmListMapperTest.xml");
    }


    @Test
    public void generateFirmListWithNoHearings() throws Exception {

        SittingStructure sittingStructure = new SittingStructure();
        sittingStructure.setSittingNote("TESTNOTE");

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-no-hearings.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        XmlTestUtils.assertXmlEquals(generatedXml, "xhibit/mapper/expectedFirmListNoHearingsMapperTest.xml");
    }

    @Test
    public void generateFirmListWithReserveList() throws Exception {

        SittingStructure sittingStructure = new SittingStructure();
        sittingStructure.setSittingNote("TESTNOTE");

        SittingStructure.Hearings hearings = mock(SittingStructure.Hearings.class);

        SittingStructure.Hearings hearings1 = new SittingStructure.Hearings();
        hearings1.getHearing().add(getHearingStructure(1));

        CourtHouseStructure courtHouseStructure = mock(CourtHouseStructure.class);


        when(courtServicesMapper.generateSittingStructure(any(JsonObject.class), eq(1))).thenReturn(sittingStructure);
        when(courtServicesMapper.generateSittingStructureHearings(any(JsonObject.class))).thenReturn(hearings1);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final JsonObject crestCourtSite = courtListsForPublishing.get(0).getJsonObject("crestCourtSite");

        when(courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite)).thenReturn(courtHouseStructure);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        XmlTestUtils.assertXmlEquals(generatedXml, "xhibit/mapper/expectedFirmListSortedSittingWithReserveListMapperTest.xml");
    }

    @Test
    public void generateFirmListWithReserveListWithSameHearingList() throws Exception {

        SittingStructure sittingStructure = new SittingStructure();
        sittingStructure.setSittingNote("TESTNOTE");

        SittingStructure.Hearings hearings = mock(SittingStructure.Hearings.class);

        SittingStructure.Hearings hearings1 = new SittingStructure.Hearings();
        hearings1.getHearing().add(getHearingStructure(1));

        CourtHouseStructure courtHouseStructure = new CourtHouseStructure();
        CourtHouseStructure.CourtHouseCode courtHouseCode = new CourtHouseStructure.CourtHouseCode();
        courtHouseStructure.setCourtHouseCode(courtHouseCode);
        courtHouseCode.setValue("443");

        CourtHouseStructure courtHouseStructure2 = new CourtHouseStructure();
        CourtHouseStructure.CourtHouseCode courtHouseCode2 = new CourtHouseStructure.CourtHouseCode();
        courtHouseStructure2.setCourtHouseCode(courtHouseCode2);
        courtHouseCode2.setValue("443");

        CourtHouseStructure courtHouseStructure3 = new CourtHouseStructure();
        CourtHouseStructure.CourtHouseCode courtHouseCode3 = new CourtHouseStructure.CourtHouseCode();
        courtHouseStructure3.setCourtHouseCode(courtHouseCode3);
        courtHouseCode3.setValue("6444");

        CourtHouseStructure courtHouseStructure4 = new CourtHouseStructure();
        CourtHouseStructure.CourtHouseCode courtHouseCode4 = new CourtHouseStructure.CourtHouseCode();
        courtHouseStructure4.setCourtHouseCode(courtHouseCode4);
        courtHouseCode4.setValue("1445");

        when(courtServicesMapper.generateSittingStructure(any(JsonObject.class), eq(1))).thenReturn(sittingStructure);
        when(courtServicesMapper.generateSittingStructureHearings(any(JsonObject.class))).thenReturn(hearings1);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-listWithSameSittingDate.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final JsonObject crestCourtSite = courtListsForPublishing.get(0).getJsonObject("crestCourtSite");
        final JsonObject crestCourtSite1 = courtListsForPublishing.get(1).getJsonObject("crestCourtSite");
        final JsonObject crestCourtSite2 = courtListsForPublishing.get(2).getJsonObject("crestCourtSite");
        final JsonObject crestCourtSite3 = courtListsForPublishing.get(3).getJsonObject("crestCourtSite");

        when(courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite)).thenReturn(courtHouseStructure);
        when(courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite1)).thenReturn(courtHouseStructure2);
        when(courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite2)).thenReturn(courtHouseStructure3);
        when(courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite3)).thenReturn(courtHouseStructure4);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        XmlTestUtils.assertXmlEquals(generatedXml, "xhibit/mapper/expectedFirmListSortedSittingWithReserveListMapperWithSameSittingDateTest.xml");
    }

    @Test
    public void generateFirmListWithNoReserveList() throws Exception {

        SittingStructure sittingStructure = new SittingStructure();
        sittingStructure.setSittingNote("TESTNOTE");

        SittingStructure.Hearings hearings = mock(SittingStructure.Hearings.class);

        SittingStructure.Hearings hearings1 = new SittingStructure.Hearings();
        hearings1.getHearing().add(getHearingStructure(1));

        CourtHouseStructure courtHouseStructure = mock(CourtHouseStructure.class);


        when(courtServicesMapper.generateSittingStructure(any(JsonObject.class), eq(1))).thenReturn(sittingStructure);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-no-weekcommencing.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final JsonObject crestCourtSite = courtListsForPublishing.get(0).getJsonObject("crestCourtSite");

        when(courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite)).thenReturn(courtHouseStructure);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        XmlTestUtils.assertXmlEquals(generatedXml, "xhibit/mapper/expectedFirmListSortedSittingWithNoReserveListMapperTest.xml");
    }

    private JsonObjectBuilder crestCourtSite() {
        return Json.createObjectBuilder()
                .add("crestCourtSiteId", "002")
                .add("crestCourtSiteName", "MOCKCOURTNAME2")
                .add("courtType", "CROWN_COURT");
    }

    private HearingStructure getHearingStructure(final int sequenceNumber){
        final HearingStructure hearingStructure = new HearingStructure();
        hearingStructure.setHearingSequenceNumber(sequenceNumber);
        hearingStructure.setRespondent("RESPONDENT1");
        hearingStructure.setCaseNumber("CASENUMBER1");

        return hearingStructure;
    }
}
