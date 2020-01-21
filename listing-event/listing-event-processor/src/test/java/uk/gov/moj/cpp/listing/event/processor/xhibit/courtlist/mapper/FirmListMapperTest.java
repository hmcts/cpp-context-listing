package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlTestUtils;

import java.util.List;

import javax.json.JsonObject;

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
}
