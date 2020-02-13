package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.DailyCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DailyListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.fromString;

public class DailyListMapper extends AbstractCourtListMapper {

    public DailyListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListsJson, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListsJson, courtServicesMapper);
    }

    @Override
    public JAXBElement<?> generate() {

        final DailyListStructure dailyListStructure = objectFactory.createDailyListStructure();

        dailyListStructure.setDocumentID(courtServicesMapper.generateDocumentID());
        dailyListStructure.setListHeader(courtServicesMapper.generateListHeader());
        dailyListStructure.setCrownCourt(courtServicesMapper.generateCrownCourtStructure(context.getParameters().getCourtCentreId()));
        dailyListStructure.setCourtLists(generateCourtLists());

        return objectFactory.createDailyList(dailyListStructure);
    }

    private DailyListStructure.CourtLists generateCourtLists() {

        final DailyListStructure.CourtLists courtLists = objectFactory.createDailyListStructureCourtLists();

        for (final JsonObject courtListJson : courtListsJson) {
            courtLists.getCourtList().add(generateDailyCourtListStructure(courtListJson));
        }

        return courtLists;
    }

    private DailyCourtListStructure generateDailyCourtListStructure(final JsonObject courtListJson) {

        final DailyCourtListStructure dailyCourtListStructure = objectFactory.createDailyCourtListStructure();

        dailyCourtListStructure.setCourtHouse(courtServicesMapper.generateCourtHouseStructure(
                courtListJson.getJsonObject("crestCourtSite")));

        dailyCourtListStructure.setSittings(generateSittings(courtListJson.getJsonArray("sittings").getValuesAs(JsonObject.class)));

        return dailyCourtListStructure;
    }

    private DailyCourtListStructure.Sittings generateSittings(final List<JsonObject> sittingsJson) {

        final DailyCourtListStructure.Sittings sittings = objectFactory.createDailyCourtListStructureSittings();
        final HashMap<UUID,Integer> currentSeqNumOfCourt = new HashMap<>();

        for (final JsonObject sittingJson : sittingsJson) {
            if (sittingJson.containsKey("courtRoomId")) {
                final UUID courtRoomId = fromString(sittingJson.getString("courtRoomId"));
                if (null == currentSeqNumOfCourt.get(courtRoomId)) {
                    currentSeqNumOfCourt.put(courtRoomId, 1);
                } else {
                    currentSeqNumOfCourt.put(courtRoomId, currentSeqNumOfCourt.get(courtRoomId) + 1);
                }
                sittings.getSitting().add(courtServicesMapper.generateSittingStructure(sittingJson, currentSeqNumOfCourt.get(courtRoomId)));
            }
        }
        sittings.getSitting().sort(Comparator.comparing(SittingStructure::getCourtRoomNumber));
        return sittings;
    }
}
