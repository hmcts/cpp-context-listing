package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;
import java.util.Comparator;
import java.util.List;

public class FirmListMapper extends AbstractCourtListMapper {

    public FirmListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListsJson, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListsJson, courtServicesMapper);
    }

    @Override
    public JAXBElement<?> generate() {

        final FirmListStructure firmListStructure = objectFactory.createFirmListStructure();

        firmListStructure.setDocumentID(courtServicesMapper.generateDocumentID());
        firmListStructure.setListHeader(courtServicesMapper.generateListHeader());
        firmListStructure.setCrownCourt(courtServicesMapper.generateCrownCourtStructure(context.getParameters().getCourtCentreId()));
        firmListStructure.setCourtLists(generateCourtLists());

        return objectFactory.createFirmList(firmListStructure);
    }

    private FirmListStructure.CourtLists generateCourtLists() {

        final FirmListStructure.CourtLists courtLists = objectFactory.createFirmListStructureCourtLists();

        for (final JsonObject courtListJson : courtListsJson) {
            courtLists.getCourtList().add(generateFirmCourtListStructure(courtListJson));
        }

        return courtLists;
    }

    private FirmCourtListStructure generateFirmCourtListStructure(final JsonObject courtListJson) {

        final FirmCourtListStructure firmCourtListStructure = objectFactory.createFirmCourtListStructure();

        firmCourtListStructure.setCourtHouse(courtServicesMapper.generateCourtHouseStructure(
                courtListJson.getJsonObject("crestCourtSite")));

        firmCourtListStructure.setSittings(generateSittings(courtListJson.getJsonArray("sittings").getValuesAs(JsonObject.class)));
        firmCourtListStructure.setSittingDate(context.getParameters().getStartDate());

        return firmCourtListStructure;
    }

    private FirmCourtListStructure.Sittings generateSittings(final List<JsonObject> sittingsJson) {

        final FirmCourtListStructure.Sittings sittings = objectFactory.createFirmCourtListStructureSittings();
        int sittingSequenceNumber = 1;
        for (final JsonObject sittingJson : sittingsJson) {
            sittings.getSitting().add(courtServicesMapper.generateSittingStructure(sittingJson, sittingSequenceNumber++));
        }
        sittings.getSitting().sort(Comparator.comparing(SittingStructure::getCourtRoomNumber));
        return sittings;
    }
}
