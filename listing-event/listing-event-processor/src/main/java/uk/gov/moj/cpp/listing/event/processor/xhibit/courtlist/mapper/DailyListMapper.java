package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.DailyCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DailyListStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

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

        final UUID courtCentreId = UUID.fromString(courtListJson.getString("courtCentreId"));
        dailyCourtListStructure.setCourtHouse(courtServicesMapper.generateCourtHouseStructure(
                courtCentreId));

        dailyCourtListStructure.setSittings(generateSittings(courtListJson.getJsonArray("sittings").getValuesAs(JsonObject.class)));

        return dailyCourtListStructure;
    }

    private DailyCourtListStructure.Sittings generateSittings(final List<JsonObject> sittingsJson) {

        final DailyCourtListStructure.Sittings sittings = objectFactory.createDailyCourtListStructureSittings();

        int sittingSequenceNumber = 1;
        for (final JsonObject sittingJson : sittingsJson) {
            sittings.getSitting().add(courtServicesMapper.generateSittingStructure(sittingJson, sittingSequenceNumber++));
        }

        return sittings;
    }
}
