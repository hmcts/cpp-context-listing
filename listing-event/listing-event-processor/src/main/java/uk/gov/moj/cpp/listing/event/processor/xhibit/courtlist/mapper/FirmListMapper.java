package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

public class FirmListMapper extends AbstractCourtListMapper {

    public FirmListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListForPublishing, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListForPublishing, courtServicesMapper);
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

        courtLists.getCourtList().add(generateFirmCourtListStructure());

        return courtLists;
    }

    private FirmCourtListStructure generateFirmCourtListStructure() {

        final FirmCourtListStructure firmCourtListStructure = objectFactory.createFirmCourtListStructure();

        firmCourtListStructure.setCourtHouse(courtServicesMapper.generateCourtHouseStructure(
                context.getParameters().getCourtCentreId()));

        firmCourtListStructure.setSittings(generateSittings());
        firmCourtListStructure.setSittingDate(context.getParameters().getStartDate());

        return firmCourtListStructure;
    }

    private FirmCourtListStructure.Sittings generateSittings() {

        final FirmCourtListStructure.Sittings sittings = objectFactory.createFirmCourtListStructureSittings();

        final List<JsonObject> sittingsJson = new ArrayList<>();
        courtListForPublishing.forEach(courtForPublishing ->
            sittingsJson.addAll(courtForPublishing.getJsonObject("courtList").getJsonArray("sittings").getValuesAs(JsonObject.class))
        );

        int sittingSequenceNumber = 1;
        for (final JsonObject sittingJson : sittingsJson) {
            sittings.getSitting().add(courtServicesMapper.generateSittingStructure(sittingJson, sittingSequenceNumber++));
        }

        return sittings;
    }
}
