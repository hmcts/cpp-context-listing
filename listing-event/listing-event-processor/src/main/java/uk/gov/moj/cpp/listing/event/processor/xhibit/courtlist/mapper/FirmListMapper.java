package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.fromString;

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
        final HashMap<UUID,Integer> currentSeqNumOfCourt = new HashMap<>();

        for (final JsonObject sittingJson : sittingsJson) {
            if (sittingJson.containsKey("courtRoomId")) {
                final UUID courtRoomId = fromString(sittingJson.getString("courtRoomId"));
                if(null == currentSeqNumOfCourt.get(courtRoomId)) {
                    currentSeqNumOfCourt.put(courtRoomId, 1);
                } else {
                    currentSeqNumOfCourt.put(courtRoomId, currentSeqNumOfCourt.get(courtRoomId) + 1);
                }
                sittings.getSitting().add(courtServicesMapper.generateSittingStructure(sittingJson, currentSeqNumOfCourt.get(courtRoomId)));
            } else {
                sittings.getSitting().add(courtServicesMapper.generateSittingStructure(sittingJson, 1));
            }
        }
        sittings.getSitting().sort(Comparator.comparing(SittingStructure::getCourtRoomNumber));
        return sittings;
    }
}
