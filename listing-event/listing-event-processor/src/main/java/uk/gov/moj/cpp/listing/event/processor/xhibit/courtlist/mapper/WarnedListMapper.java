package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.WarnedListStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

public class WarnedListMapper extends AbstractCourtListMapper {

    public WarnedListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListForPublishing, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListForPublishing, courtServicesMapper);
    }

    @Override
    public JAXBElement<?> generate() {

        final WarnedListStructure warnedListStructure = objectFactory.createWarnedListStructure();

        warnedListStructure.setDocumentID(courtServicesMapper.generateDocumentID());
        warnedListStructure.setListHeader(courtServicesMapper.generateListHeader());
        warnedListStructure.setCrownCourt(courtServicesMapper.generateCrownCourtStructure(context.getParameters().getCourtCentreId()));
        warnedListStructure.setCourtLists(generateCourtLists());

        return objectFactory.createWarnedList(warnedListStructure);
    }

    private WarnedListStructure.CourtLists generateCourtLists() {

        final WarnedListStructure.CourtLists courtLists = objectFactory.createWarnedListStructureCourtLists();

        courtLists.getCourtList().add(generateWarnedCourtListStructure());

        return courtLists;
    }

    private WarnedListStructure.CourtLists.CourtList generateWarnedCourtListStructure() {

        final WarnedListStructure.CourtLists.CourtList courtList = objectFactory.createWarnedListStructureCourtListsCourtList();

        courtList.setCourtHouse(courtServicesMapper.generateCourtHouseStructure(
                context.getParameters().getCourtCentreId()));

        final List<JsonObject> sittingsJson = new ArrayList<>();
        courtListForPublishing.forEach(courtForPublishing ->
            sittingsJson.addAll(courtForPublishing.getJsonObject("courtList").getJsonArray("sittings").getValuesAs(JsonObject.class))
        );


        for (final JsonObject sittingJson : sittingsJson) {

            if (sittingJson.getBoolean("weekCommencing")) {
                courtList.getWithoutFixedDate().add(generateWithoutFixedDate(sittingJson));
            } else {
                courtList.getWithFixedDate().add(generateWithFixedDate(sittingJson));
            }
        }

        return courtList;
    }

    private WarnedListStructure.CourtLists.CourtList.WithoutFixedDate generateWithoutFixedDate(final JsonObject sittingJson) {

        final WarnedListStructure.CourtLists.CourtList.WithoutFixedDate withoutFixedDate = objectFactory
                .createWarnedListStructureCourtListsCourtListWithoutFixedDate();

        withoutFixedDate.getFixture().add(courtServicesMapper.generateFixtureStructure(sittingJson));
        withoutFixedDate.setHearingType("HRG"); // For Hearing

        return withoutFixedDate;
    }

    private WarnedListStructure.CourtLists.CourtList.WithFixedDate generateWithFixedDate(final JsonObject sittingJson) {

        final WarnedListStructure.CourtLists.CourtList.WithFixedDate withFixedDate = objectFactory
                .createWarnedListStructureCourtListsCourtListWithFixedDate();

        withFixedDate.getFixture().add(courtServicesMapper.generateFixtureStructure(sittingJson));
        withFixedDate.setHearingType("HRG"); // For Hearing

        return withFixedDate;
    }
}
