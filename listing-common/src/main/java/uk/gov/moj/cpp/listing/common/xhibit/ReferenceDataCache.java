package uk.gov.moj.cpp.listing.common.xhibit;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;
import uk.gov.moj.cpp.listing.common.xhibit.model.CourtCentreRoomKey;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
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

    private Map<String, List<CourtLocation>> courtLocationListMapCache = new HashMap<>();

    private Map<String, List<CourtMapping>> courtMappingListMapCache = new HashMap<>();

    private Map<UUID, HearingType> hearingTypesMapCache = new HashMap<>();

    private Map<UUID, List<JsonObject>> cpCourtRoomCache = new HashMap<>();

    private Map<String, OrganisationUnit> organisationUnitMapCache = new HashMap<>();

    private Map<UUID, OrganisationUnit> organisationUnitMapByIdCache = new HashMap<>();

    private Map<UUID, CourtRoomMappingsList> courtRoomMappingsMapCache = new HashMap<>();

    private Map<CourtCentreRoomKey, Optional<CourtRoomMapping>> courtRoomMappingByCourtCentreRoomMap = new HashMap<>();

    private Map<UUID, Judiciary> judiciariesMapCache = new HashMap<>();

    @PostConstruct
    public void initReferenceData() {
        initCourtMappingsList();
        initHearingTypes();
        initOrganisationUnitList();
    }

    public Optional<List<CourtMapping>> getCourtMappingsMapCache(final UUID courtCentreId) {
        final OrganisationUnit organisationUnit = organisationUnitMapByIdCache.get(courtCentreId);
        if (Objects.isNull(organisationUnit)) {
            return empty();
        }
        return ofNullable(courtMappingListMapCache.get(organisationUnit.getOucode()));
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

    public List<CourtLocation> getCourtLocationsCache(final String crestCourtId) {
        if (courtLocationListMapCache.isEmpty()) {
            initCourtMappingsList();
        }
        return courtLocationListMapCache.get(crestCourtId);
    }

    private void initCourtMappingsList() {
        referenceDataLoader.getXhibitCourtMappings().ifPresent(courtMappingsList -> {
                courtLocationListMapCache = courtMappingsList.getCpXhibitCourtMappings()
                    .stream()
                    .map(this::createCourtLocation)
                    .collect(Collectors.groupingBy(CourtLocation::getCrestCourtId));

                courtMappingListMapCache = courtMappingsList.getCpXhibitCourtMappings()
                        .stream()
                        .collect(Collectors.groupingBy(CourtMapping::getOucode));
            }

        );
    }

    private void initHearingTypes() {
        referenceDataLoader.getHearingTypesList().ifPresent(hearingTypesList ->
            hearingTypesMapCache = hearingTypesList.getHearingTypes().stream()
                    .collect(Collectors.toMap(HearingType::getId, Function.identity())
        ));
    }

    private void initOrganisationUnitList() {
        referenceDataLoader.getOrganisationUnitList().ifPresent(organisationUnitList ->
            organisationUnitMapByIdCache = organisationUnitList.getOrganisationunits().stream()
                    .collect(Collectors.toMap(OrganisationUnit::getId, Function.identity()))
        );
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
}