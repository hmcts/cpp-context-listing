package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.HearingStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlTestUtils;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class FirmListMapperTest extends BaseMapperTest {

    private static final Logger LOGGER = getLogger(FirmListMapperTest.class);

    @Mock
    private CourtServicesMapper courtServicesMapper;

    @Test
    public void generate() throws Exception {

        SittingStructure sittingStructure = new SittingStructure();
        sittingStructure.setSittingNote("TESTNOTE");

        when(courtServicesMapper.generateSittingStructure(any(JsonObject.class), eq(1))).thenReturn(sittingStructure);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        LOGGER.info("generatedXml:\n{}", generatedXml);

        XmlTestUtils.assertXmlEquals(generatedXml, "xhibit/mapper/expectedFirmListMapperTest.xml");
    }

    @Test
    public void generateFirmListWithNoHearings() throws Exception {

        SittingStructure sittingStructure = new SittingStructure();
        sittingStructure.setSittingNote("TESTNOTE");

        when(courtServicesMapper.generateSittingStructure(any(JsonObject.class), eq(1))).thenReturn(sittingStructure);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-no-hearings.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        LOGGER.info("generatedXml:\n{}", generatedXml);

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

        LOGGER.info("generatedXml:\n{}", generatedXml);

        XmlTestUtils.assertXmlEquals(generatedXml, "xhibit/mapper/expectedFirmListSortedSittingWithReserveListMapperTest.xml");
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
        when(courtServicesMapper.generateSittingStructureHearings(any(JsonObject.class))).thenReturn(hearings1);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-no-weekcommencing.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final JsonObject crestCourtSite = courtListsForPublishing.get(0).getJsonObject("crestCourtSite");

        when(courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite)).thenReturn(courtHouseStructure);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        LOGGER.info("generatedXml:\n{}", generatedXml);

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
