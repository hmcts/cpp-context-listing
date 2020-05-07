package uk.gov.moj.cpp.listing.common.xhibit;

import static java.lang.String.format;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CommonXhibitReferenceDataService {

    private static final String UNMAPPED_COURT_ROOM_NAME = "Court -99";
    private static final String CREST_COURT_SITE_CODE = "crestCourtSiteCode";
    private static final String DEFAULT_CREST_COURT_SITE_CODE = "A";

    private XhibitReferenceDataValidator xhibitReferenceDataValidator = new XhibitReferenceDataValidator();

    @Inject
    private ReferenceDataCache referenceDataCache;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public List<UUID> getCourtCentreIdsForCrestId(final String crownCourtCrestId) {
        final List<CourtLocation> courtLocations = referenceDataCache.getCourtLocationsCache(crownCourtCrestId);
        courtLocations.forEach(courtLocation -> xhibitReferenceDataValidator.validate("crestCourtSiteId", courtLocation.getCrestCourtSiteId()));

        return courtLocations.stream()
                .map(courtLocation -> getOrganisationUnitIdByOuCode(courtLocation.getOuCode()))
                .distinct()
                .collect(Collectors.toList());
    }

    public String getDefaultCrestCourtSiteCode(final UUID courtCentreId) {

        return getCrestCourtSitesForCourtCentre(courtCentreId)
                .stream()
                .map(courtSite -> courtSite.getString(CREST_COURT_SITE_CODE))
                .sorted()
                .findFirst().orElse(DEFAULT_CREST_COURT_SITE_CODE);
    }

    public CourtLocation getCourtDetails(final UUID courtCentreId) {

        final CourtMapping court = referenceDataCache.getCourtMappingsMapCache(courtCentreId)
                .orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find court details with courtCentre %s", courtCentreId)))
                .stream()
                .findFirst()
                .orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find court details with courtCentre %s", courtCentreId)));

        return createCourtLocation(court);
    }

    public List<JsonObject> getCrestCourtSitesForCourtCentre(final UUID courtCentreId) {
        final List<JsonObject> courtMappingJsonObjectList = new ArrayList<>();

        final Optional<List<CourtMapping>> courtMappingList = referenceDataCache.getCourtMappingsMapCache(courtCentreId);

        courtMappingList.ifPresent(courtMappings -> courtMappings.forEach(courtMapping ->
                courtMappingJsonObjectList.add(objectToJsonObjectConverter.convert(courtMapping)))
        );

        return courtMappingJsonObjectList;
    }

    public Optional<CourtRoomMapping> getCourtRoom(final UUID courtCentreId, final UUID courtRoomUUID) {
        final JsonObject cpCourtRoom = getCpCourtRoom(courtCentreId, courtRoomUUID);

        final Integer courtRoomId = cpCourtRoom.getJsonNumber("courtroomId").intValue();

        final Optional<CourtRoomMappingsList> courtRoomMappingsListOptional = getCourtRoomMappingsList(courtCentreId);

        return courtRoomMappingsListOptional.isPresent() ? courtRoomMappingsListOptional.get().getCpXhibitCourtRoomMappings()
                .stream()
                .filter(courtRoomMapping -> courtRoomMapping.getCourtRoomId().equals(courtRoomId))
                .findFirst()
                : Optional.empty();
    }

    public HearingType getXhibitHearingType(final UUID cppHearingTypeId) {
        return referenceDataCache.getHearingTypeCache(cppHearingTypeId)
                .orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find hearing type %s", cppHearingTypeId)));
    }

    public int getCourtRoomNumber(final UUID courtCentreId, final UUID courtRoomId) {
        final CourtRoomMapping courtRoomMapping = getCourtRoomMappingBy(courtCentreId, courtRoomId);
        return courtRoomNumberForCourtRoom(courtRoomMapping.getCrestCourtRoomName());
    }

    public JsonObject getJudiciary(final UUID judiciaryId) {
        return objectToJsonObjectConverter.convert(getJudiciaryAsObject(judiciaryId));
    }

    private Judiciary getJudiciaryAsObject(final UUID judiciaryId) {

        return referenceDataCache.getJudiciariesMapCache(judiciaryId)
                .orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find judiciary with id %s", judiciaryId)));
    }

    public CourtRoomMapping getCourtRoomMappingBy(final UUID courtCentreId, final UUID courtRoomId) {
        return referenceDataCache.getCourtRoomMappingByCourtCentreAndCourtRoom(courtCentreId, courtRoomId)
                .orElse(new CourtRoomMapping(UNMAPPED_COURT_ROOM_NAME));
    }

    private UUID getOrganisationUnitIdByOuCode(final String ouCode) {
        final OrganisationUnit organisationUnit = referenceDataCache.getOrganisationUnitMapCache(ouCode)
                .orElseThrow(() -> new InvalidReferenceDataException(format("Cannot find organisation unit with code %s", ouCode)));

        return organisationUnit.getId();
    }

    private JsonObject getCpCourtRoom(final UUID courtCentreId, final UUID courtRoomUUID) {
        return referenceDataCache.getCpCourtRoomCache(courtCentreId)
                .stream()
                .filter(cpCourtRoom -> UUID.fromString(cpCourtRoom.getString("id")).equals(courtRoomUUID))
                .findFirst()
                .orElseThrow(() -> new InvalidReferenceDataException("Cannot find court room uuid " + courtRoomUUID));
    }

    public Optional<CourtRoomMappingsList> getCourtRoomMappingsList(final UUID courtCentreId) {

        return Optional.of(new CourtRoomMappingsList(referenceDataCache.getCourtRoomMappingsMapCache(courtCentreId).getCpXhibitCourtRoomMappings()));
    }

    private int courtRoomNumberForCourtRoom(final String courtRoomName) {
        final String[] splitName = courtRoomName.split(" ");

        return Integer.parseInt(splitName[splitName.length-1]);
    }

    private CourtLocation createCourtLocation(final CourtMapping courtMapping) {
        xhibitReferenceDataValidator.validate("crestCourtSiteId", courtMapping.getCrestCourtSiteId());

        return new CourtLocation(
                courtMapping.getOucode(),
                courtMapping.getCrestCourtId(),
                courtMapping.getCrestCourtSiteId(),
                courtMapping.getCrestCourtName(),
                courtMapping.getCrestCourtShortName(),
                courtMapping.getCrestCourtSiteName(),
                courtMapping.getCrestCourtSiteCode(),
                courtMapping.getCourtType());
    }

}
