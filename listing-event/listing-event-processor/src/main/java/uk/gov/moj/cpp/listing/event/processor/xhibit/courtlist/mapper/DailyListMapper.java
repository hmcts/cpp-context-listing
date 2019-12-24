package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.DailyCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DailyListStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

public class DailyListMapper extends AbstractCourtListMapper {

    public DailyListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListForPublishing, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListForPublishing, courtServicesMapper);
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

        courtLists.getCourtList().add(generateDailyCourtListStructure());

        return courtLists;
    }

    private DailyCourtListStructure generateDailyCourtListStructure() {

        final DailyCourtListStructure dailyCourtListStructure = objectFactory.createDailyCourtListStructure();

        dailyCourtListStructure.setCourtHouse(courtServicesMapper.generateCourtHouseStructure(
                context.getParameters().getCourtCentreId()));

        dailyCourtListStructure.setSittings(generateSittings());

        return dailyCourtListStructure;
    }

    private DailyCourtListStructure.Sittings generateSittings() {

        final DailyCourtListStructure.Sittings sittings = objectFactory.createDailyCourtListStructureSittings();

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
