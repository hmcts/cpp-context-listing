package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.WarnedListStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarnedListMapper extends AbstractCourtListMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarnedListMapper.class);
    private static final String HEARINGS = "hearings";
    private static final String HEARING_TYPE = "hearingType";

    public WarnedListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListsJson, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListsJson, courtServicesMapper);
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

        for (final JsonObject courtListJson : courtListsJson) {
            courtLists.getCourtList().add(generateWarnedCourtListStructure(courtListJson));
        }

        return courtLists;
    }

    private WarnedListStructure.CourtLists.CourtList generateWarnedCourtListStructure(final JsonObject courtListJson) {

        final WarnedListStructure.CourtLists.CourtList courtList = objectFactory.createWarnedListStructureCourtListsCourtList();

        courtList.setCourtHouse(courtServicesMapper.generateCourtHouseStructure(
                courtListJson.getJsonObject("crestCourtSite")));

        for (final JsonObject sittingJson : courtListJson.getJsonArray("sittings").getValuesAs(JsonObject.class)) {

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
        withoutFixedDate.setHearingType(getXhibitHearingType(sittingJson));

        return withoutFixedDate;
    }

    private WarnedListStructure.CourtLists.CourtList.WithFixedDate generateWithFixedDate(final JsonObject sittingJson) {

        final WarnedListStructure.CourtLists.CourtList.WithFixedDate withFixedDate = objectFactory
                .createWarnedListStructureCourtListsCourtListWithFixedDate();

        withFixedDate.getFixture().add(courtServicesMapper.generateFixtureStructure(sittingJson));
        withFixedDate.setHearingType(getXhibitHearingType(sittingJson));

        return withFixedDate;
    }

    protected String getXhibitHearingType(final JsonObject sittingJson) {
        final Set<UUID> hearingTypeUuids = sittingJson.getJsonArray(HEARINGS)
                .stream()
                .map(JsonObject.class::cast)
                .map(hearing -> hearing.getJsonObject(HEARING_TYPE))
                .map(hearing -> hearing.getString("id"))
                .map(UUID::fromString)
                .collect(Collectors.toSet());

        if (hearingTypeUuids.isEmpty()) {
            LOGGER.warn("Expecting 1 hearingTye, got nothing");
            return null;
        }

        if (hearingTypeUuids.size() > 1) {
            LOGGER.warn("Expecting 1 hearingTye, got {} ", hearingTypeUuids);
        }

        final UUID hearingUUID = hearingTypeUuids.stream().findFirst().orElse(null);

        return courtServicesMapper.getHearingTypeForHearing(hearingUUID);
    }
}
