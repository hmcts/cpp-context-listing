package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

public class FirmListMapper extends AbstractCourtListMapper {
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String DUMMY_DATE = "DUMMY_DATE";
    private static final String SITTING_DATE = "sittingDate";
    private final FirmListStructure.ReserveList reserveList = objectFactory.createFirmListStructureReserveList();

    public FirmListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListsJson, final CourtServicesMapper courtServicesMapper) {
        super(context, courtListsJson, courtServicesMapper);
    }

    @Override
    public JAXBElement<?> generate() {

        final FirmListStructure firmListStructure = objectFactory.createFirmListStructure();

        firmListStructure.setDocumentID(courtServicesMapper.generateDocumentID());
        firmListStructure.setListHeader(courtServicesMapper.generateListHeader());
        firmListStructure.setCrownCourt(courtServicesMapper.generateCrownCourtStructure(context.getParameters().getCourtCentreId()));

        final FirmListStructure.CourtLists courtLists = generateCourtLists();
        firmListStructure.setCourtLists(courtLists);

        if (!reserveList.getHearing().isEmpty()) {
            firmListStructure.setReserveList(reserveList);
        }

        return objectFactory.createFirmList(firmListStructure);
    }

    private FirmListStructure.CourtLists generateCourtLists() {

        final FirmListStructure.CourtLists courtLists = objectFactory.createFirmListStructureCourtLists();

        for (final JsonObject courtListJson : courtListsJson) {
            final Map<String, FirmCourtListStructure> firmCourtListStructures = generateFirmCourtListStructure(courtListJson);
            firmCourtListStructures.forEach((key, value) -> courtLists.getCourtList().add(value));
        }
        return courtLists;
    }


    private Map<String, FirmCourtListStructure> generateFirmCourtListStructure(final JsonObject courtListJson) {

        final JsonObject crestCourtSite = courtListJson.getJsonObject("crestCourtSite");

        final List<JsonObject> sittings = courtListJson.getJsonArray("sittings").getValuesAs(JsonObject.class);

        final Map<String, FirmCourtListStructure> firmCourtListStructureSittingMap = buildFirmListStructure(sittings, crestCourtSite);

        return firmCourtListStructureSittingMap;
    }

    private Map<String, FirmCourtListStructure> buildFirmListStructure(final List<JsonObject> sittings, final JsonObject crestCourtSite) {
        final HashMap<String, FirmCourtListStructure> firmCourtListStructureSittingMap = new HashMap<>();
        final HashMap<UUID, Integer> currentSeqNumOfCourt = new HashMap<>();

        if (!sittings.isEmpty()) {
            sittings.forEach(sitting -> {
                final FirmCourtListStructure.Sittings sittingList = objectFactory.createFirmCourtListStructureSittings();

                final FirmCourtListStructure firmCourtListStructure = getFirmCourtListStructureWithCourtHouse(crestCourtSite);

                final String key = sitting.getString(SITTING_DATE, EMPTY);
                if (sitting.containsKey(COURT_ROOM_ID)) {
                    final UUID courtRoomId = fromString(sitting.getString(COURT_ROOM_ID));
                    if (null == currentSeqNumOfCourt.get(courtRoomId)) {
                        currentSeqNumOfCourt.put(courtRoomId, 1);
                    } else {
                        currentSeqNumOfCourt.put(courtRoomId, currentSeqNumOfCourt.get(courtRoomId) + 1);
                    }
                    sittingList.getSitting().add(courtServicesMapper.generateSittingStructure(sitting, currentSeqNumOfCourt.get(courtRoomId)));
                } else {
                    sittingList.getSitting().add(courtServicesMapper.generateSittingStructure(sitting, 1));
                }
                sittingList.getSitting().sort(Comparator.comparing(SittingStructure::getCourtRoomNumber));
                firmCourtListStructure.setSittings(sittingList);
                firmCourtListStructure.setSittingDate(LocalDate.parse(sitting.getString(SITTING_DATE)));

                buildFirmCourtListStructureAndReserveList(firmCourtListStructureSittingMap, sitting, sittingList, firmCourtListStructure, key);
            });
        } else {

            final FirmCourtListStructure firmCourtListStructure = getFirmCourtListStructureWithCourtHouse(crestCourtSite);
            final FirmCourtListStructure.Sittings firmCourtListStructureSittings = objectFactory.createFirmCourtListStructureSittings();
            firmCourtListStructure.setSittings(firmCourtListStructureSittings);

            firmCourtListStructureSittingMap.put(DUMMY_DATE, firmCourtListStructure);
        }

        return new TreeMap<>(firmCourtListStructureSittingMap);
    }

    private FirmCourtListStructure getFirmCourtListStructureWithCourtHouse(final JsonObject crestCourtSite) {
        final FirmCourtListStructure firmCourtListStructure = objectFactory.createFirmCourtListStructure();

        final CourtHouseStructure courtHouse = courtServicesMapper.generateCourtHouseStructure(
                crestCourtSite);
        firmCourtListStructure.setCourtHouse(courtHouse);
        return firmCourtListStructure;
    }

    private void buildFirmCourtListStructureAndReserveList(final HashMap<String, FirmCourtListStructure> firmCourtListStructureSittingMap, final JsonObject sitting, final FirmCourtListStructure.Sittings sittingList, final FirmCourtListStructure firmCourtListStructure, final String key) {

        if (sitting.getBoolean("weekCommencing")) {
            final SittingStructure.Hearings hearings = courtServicesMapper.generateSittingStructureHearings(sitting);
            hearings.getHearing().forEach(hearingStructure -> reserveList.getHearing().add(hearingStructure));
        } else {
            if (firmCourtListStructureSittingMap.containsKey(key)) {
                firmCourtListStructureSittingMap.get(key).getSittings().getSitting().addAll(sittingList.getSitting());

            } else {
                firmCourtListStructureSittingMap.put(key, firmCourtListStructure);
            }
        }
    }
}
