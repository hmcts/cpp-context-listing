package uk.gov.moj.cpp.listing.common.xhibit;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;
import uk.gov.moj.cpp.listing.common.xhibit.model.CourtCentreRoomKey;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingTypesList;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnitList;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

@ApplicationScoped
public class ReferenceDataCache {

    @Inject
    private ReferenceDataLoader referenceDataLoader;

    private Map<String, List<CourtLocation>> crownCourtLocationListMapCache = new HashMap<>();

    private Map<String, List<CourtMapping>> crownCourtMappingListMapCache = new HashMap<>();

    private Map<String, List<CourtMapping>> magsCourtMappingListMapCache = new HashMap<>();

    private Map<UUID, HearingType> hearingTypesMapCache = new HashMap<>();

    private Map<UUID, List<JsonObject>> cpCourtRoomCache = new HashMap<>();

    private Map<String, OrganisationUnit> organisationUnitMapCache = new HashMap<>();

    private Map<UUID, OrganisationUnit> organisationUnitMapByIdCache = new HashMap<>();

    private Map<UUID, CourtRoomMappingsList> courtRoomMappingsMapCache = new HashMap<>();

    private Map<CourtCentreRoomKey, Optional<CourtRoomMapping>> courtRoomMappingByCourtCentreRoomMap = new HashMap<>();

    private Map<UUID, Judiciary> judiciariesMapCache = new HashMap<>();

    private Map<String, HearingType> hearingTypesCodesMapCache = new HashMap<>();

    @PostConstruct
    public void initReferenceData() {
        initCrownCourtMappingsList();
        initHearingTypes();
        initOrganisationUnitList();
    }

    public Optional<List<CourtMapping>> getCrownCourtMappingsMapCache(final UUID courtCentreId) {
        final OrganisationUnit organisationUnit = organisationUnitMapByIdCache.get(courtCentreId);
        if (Objects.isNull(organisationUnit)) {
            return empty();
        }
        return ofNullable(crownCourtMappingListMapCache.get(organisationUnit.getOucode()));
    }

    public Optional<List<CourtMapping>> getMagsCourtMappingsMapCache(final UUID courtCentreId) {
        final OrganisationUnit organisationUnit = organisationUnitMapByIdCache.get(courtCentreId);
        if (Objects.isNull(organisationUnit)) {
            return empty();
        }

        final String oucode = organisationUnit.getOucode();

        if (!magsCourtMappingListMapCache.containsKey(oucode)) {
            referenceDataLoader.getXhibitMagsCourtMappings(oucode)
                    .ifPresent(courtMappingsList -> magsCourtMappingListMapCache.put(oucode, courtMappingsList.getCpXhibitCourtMappings()));
        }

        return ofNullable(magsCourtMappingListMapCache.get(organisationUnit.getOucode()));
    }

    public Optional<Judiciary> getJudiciariesMapCache(final UUID judiciaryId) {
        return Optional.of(judiciariesMapCache.computeIfAbsent(judiciaryId, key -> referenceDataLoader.getJudiciary(judiciaryId).get()));
    }

    public CourtRoomMappingsList getCourtRoomMappingsMapCache(final UUID courtCentreId) {
        return courtRoomMappingsMapCache.computeIfAbsent(courtCentreId, key -> referenceDataLoader.getCourtRoomMappingsList(courtCentreId).get());
    }

    public Optional<CourtRoomMapping> getCourtRoomMappingByCourtCentreAndCourtRoom(final UUID courtCentreId, final UUID courtRoomId) {
        return courtRoomMappingByCourtCentreRoomMap.computeIfAbsent(new CourtCentreRoomKey(courtCentreId, courtRoomId),
                key ->
                        referenceDataLoader.getCourtRoomMappingsList(courtCentreId)
                                .orElseThrow(() -> new InvalidReferenceDataException("Invalid object courtRoomMappings"))
                                .getCpXhibitCourtRoomMappings()
                                .stream()
                                .filter(courtRoomMappings -> Objects.nonNull(courtRoomMappings.getCourtRoomUUID()))
                                .filter(courtRoomMappings -> courtRoomMappings.getCourtRoomUUID().equals(courtRoomId))
                                .findFirst()
        );
    }

    public Optional<OrganisationUnit> getOrganisationUnitMapCache(final String ouCode) {
        return Optional.of(organisationUnitMapCache.computeIfAbsent(ouCode, key -> referenceDataLoader.getOrganisationUnitByOuCode(ouCode).get()));
    }

    public List<JsonObject> getCpCourtRoomCache(final UUID courtCentreId) {
        return cpCourtRoomCache.computeIfAbsent(courtCentreId, key -> referenceDataLoader.getCpCourtRoom(courtCentreId));
    }

    public Optional<HearingType> getHearingTypeCache(final UUID hearingTypeId) {
        if (hearingTypesMapCache.isEmpty()) {
            initHearingTypes();
        }
        return Optional.of(hearingTypesMapCache.get(hearingTypeId));
    }

    public Optional<HearingType> getHearingTypeCodeCache(final String hearingCode) {
        if (hearingTypesCodesMapCache.isEmpty()) {
            initHearingTypes();
        }
        return Optional.of(hearingTypesCodesMapCache.get(hearingCode));
    }

    public List<CourtLocation> getCrownCourtLocationsCache(final String crestCourtId) {
        if (crownCourtLocationListMapCache.isEmpty()) {
            initCrownCourtMappingsList();
        }
        return crownCourtLocationListMapCache.get(crestCourtId);
    }

    private void initCrownCourtMappingsList() {
        referenceDataLoader.getXhibitCrownCourtMappings().ifPresent(courtMappingsList -> {
                    crownCourtLocationListMapCache = courtMappingsList.getCpXhibitCourtMappings()
                            .stream()
                            .map(this::createCourtLocation)
                            .collect(Collectors.groupingBy(CourtLocation::getCrestCourtId));

                    crownCourtMappingListMapCache = courtMappingsList.getCpXhibitCourtMappings()
                            .stream()
                            .collect(Collectors.groupingBy(CourtMapping::getOucode));
                }
        );
    }

    protected void initHearingTypes() {
        referenceDataLoader.getHearingTypesList().ifPresent(this::getHearingTypes);
    }

    protected void initOrganisationUnitList() {
        referenceDataLoader.getOrganisationUnitList().ifPresent(this::getOrganisationUnits);
    }

    private CourtLocation createCourtLocation(final CourtMapping courtMapping) {

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

    private void getHearingTypes(HearingTypesList hearingTypesList) {
        hearingTypesMapCache = hearingTypesList.getHearingTypes().stream()
                .collect(Collectors.toMap(HearingType::getId, Function.identity(), (firstHearingTypeId, secondHearingTypeId) -> firstHearingTypeId)
                );
        hearingTypesCodesMapCache = hearingTypesList.getHearingTypes().stream()
                .collect(Collectors.toMap(HearingType::getHearingCode, Function.identity(), (firstHearingTypeId, secondHearingTypeId) -> firstHearingTypeId)
                );
    }

    private void getOrganisationUnits(OrganisationUnitList organisationUnitList) {
        organisationUnitMapByIdCache = organisationUnitList.getOrganisationunits().stream()
                .collect(Collectors.toMap(OrganisationUnit::getId, Function.identity(), (firstOrganisationUnitId, secondOrganisationUnitId) -> firstOrganisationUnitId));
    }
}