package uk.gov.moj.cpp.listing.common.xhibit;

import uk.gov.moj.cpp.listing.domain.referencedata.CourtMapping;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMappingsList;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnitList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Map<String, List<CourtMapping>> courtMappingsMapByCrestCourtIdCache = new HashMap<>();

    private Map<UUID, HearingType> hearingTypesMapCache = new HashMap<>();

    private Map<UUID, List<JsonObject>> cpCourtRoomCache = new HashMap<>();

    private Map<String, OrganisationUnitList> organisationUnitMapCache = new HashMap<>();

    private Map<UUID, CourtRoomMappingsList> courtRoomMappingsMapCache = new HashMap<>();

    private Map<UUID, Judiciary> judiciariesMapCache = new HashMap<>();

    private Map<UUID, CourtMappingsList> courtMappingsMapCache = new HashMap<>();

    @PostConstruct
    public void initReferenceData() {
        initCourtMappings();
        initHearingTypes();
    }

    public CourtMappingsList getCourtMappingsMapCache(final UUID courtCentreId) {
        return courtMappingsMapCache.computeIfAbsent(courtCentreId, key -> referenceDataLoader.getXhibitCourtMappings(courtCentreId).get());
    }

    public Optional<Judiciary> getJudiciariesMapCache(final UUID judiciaryId) {
        return Optional.of(judiciariesMapCache.computeIfAbsent(judiciaryId, key -> referenceDataLoader.getJudiciary(judiciaryId).get()));
    }

    public CourtRoomMappingsList getCourtRoomMappingsMapCache(final UUID courtCentreId) {
        return courtRoomMappingsMapCache.computeIfAbsent(courtCentreId, key -> referenceDataLoader.getCourtRoomMappingsList(courtCentreId).get());
    }

    public OrganisationUnitList getOrganisationUnitMapCache(final String ouCode) {
        return organisationUnitMapCache.computeIfAbsent(ouCode, key -> referenceDataLoader.getOrganisationUnitListByOuCode(ouCode).get());
    }

    public List<JsonObject> getCpCourtRoomCache(final UUID courtCentreId) {
        return cpCourtRoomCache.computeIfAbsent(courtCentreId, key -> referenceDataLoader.getCpCourtRoom(courtCentreId));
    }

    public Optional<HearingType> getHearingTypeCache(final UUID hearingTypeId) {
        return Optional.of(hearingTypesMapCache.get(hearingTypeId));
    }

    public List<CourtMapping> getCourtMappingsCache(final String crestCourtId) {
        return courtMappingsMapByCrestCourtIdCache.get(crestCourtId);
    }

    private void initCourtMappings() {
        referenceDataLoader.getXhibitCourtMappings().ifPresent(courtMappingsList ->
            courtMappingsMapByCrestCourtIdCache = courtMappingsList.getCpXhibitCourtMappings()
                    .stream()
                    .collect(Collectors.groupingBy(CourtMapping::getCrestCourtId))
        );
    }

    private void initHearingTypes() {
        referenceDataLoader.getHearingTypesList().ifPresent(hearingTypesList ->
            hearingTypesMapCache = hearingTypesList.getHearingTypes().stream()
                    .collect(Collectors.toMap(HearingType::getId, Function.identity())
        ));
    }
}