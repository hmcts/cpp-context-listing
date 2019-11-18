package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.WarnedListStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.util.List;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

public class WarnedListMapper extends AbstractCourtListMapper {

    public WarnedListMapper(final CourtListGenerationContext context, final JsonObject courtListForPublishing, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListForPublishing, courtServicesMapper);
    }

    @Override
    public JAXBElement<?> generate() {

        final WarnedListStructure warnedListStructure = objectFactory.createWarnedListStructure();

        warnedListStructure.setDocumentID(courtServicesMapper.generateDocumentID());
        warnedListStructure.setListHeader(courtServicesMapper.generateListHeader());
        warnedListStructure.setCrownCourt(courtServicesMapper.generateCourtHouseStructure(context.getParameters().getCourtCentreId()));
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

        final List<JsonObject> hearings = courtListForPublishing.getJsonArray("hearings").getValuesAs(JsonObject.class);

        for (final JsonObject hearing : hearings) {

            if (hasFixedDate(hearing)) {
                courtList.getWithFixedDate().add(generateWithFixedDate(hearing));
            } else {
                courtList.getWithoutFixedDate().add(generateWithoutFixedDate(hearing));
            }
        }

        return courtList;
    }

    private boolean hasFixedDate(final JsonObject hearing) {

        return (hearing.containsKey("startDate"));
    }

    private WarnedListStructure.CourtLists.CourtList.WithoutFixedDate generateWithoutFixedDate(final JsonObject hearing) {

        final WarnedListStructure.CourtLists.CourtList.WithoutFixedDate withoutFixedDate = objectFactory.createWarnedListStructureCourtListsCourtListWithoutFixedDate();

        withoutFixedDate.getFixture().add(courtServicesMapper.generateFixtureStructure(hearing));
        withoutFixedDate.setHearingType(courtServicesMapper.getHearingTypeForHearing(hearing));

        return withoutFixedDate;
    }

    private WarnedListStructure.CourtLists.CourtList.WithFixedDate generateWithFixedDate(final JsonObject hearing) {

        final WarnedListStructure.CourtLists.CourtList.WithFixedDate withFixedDate = objectFactory.createWarnedListStructureCourtListsCourtListWithFixedDate();

        withFixedDate.getFixture().add(courtServicesMapper.generateFixtureStructure(hearing));
        withFixedDate.setHearingType(courtServicesMapper.getHearingTypeForHearing(hearing));

        return withFixedDate;
    }
}
