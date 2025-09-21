package uk.gov.moj.cpp.listing.common.xhibit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

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

@ApplicationScoped
public class ReferenceDataCache {

    @Inject
    private ReferenceDataLoader referenceDataLoader;

    private final Map<String, List<CourtLocation>> crownCourtLocationListMapCache = new ConcurrentHashMap<>();

    private final Map<String, List<CourtMapping>> crownCourtMappingListMapCache = new ConcurrentHashMap<>();

    private final Map<String, List<CourtMapping>> magsCourtMappingListMapCache = new ConcurrentHashMap<>();

    private final Map<UUID, HearingType> hearingTypesMapCache = new ConcurrentHashMap<>();

    private final Map<UUID, List<JsonObject>> cpCourtRoomCache = new ConcurrentHashMap<>();

    private final Map<String, OrganisationUnit> organisationUnitMapCache = new ConcurrentHashMap<>();

    private final Map<UUID, OrganisationUnit> organisationUnitMapByIdCache = new ConcurrentHashMap<>();

    private final Map<UUID, CourtRoomMappingsList> courtRoomMappingsMapCache = new ConcurrentHashMap<>();

    private final Map<CourtCentreRoomKey, Optional<CourtRoomMapping>> courtRoomMappingByCourtCentreRoomMap = new ConcurrentHashMap<>();

    private final Map<UUID, Judiciary> judiciariesMapCache = new ConcurrentHashMap<>();

    private final Map<String, HearingType> hearingTypesCodesMapCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initReferenceData() {
        synchronized (this) {
            this.initCrownCourtMappingsList();
            this.initAllHearingTypes();
            this.initOrganisationUnitList();
        }
    }

    public Optional<List<CourtMapping>> getCrownCourtMappingsMapCache(final UUID courtCentreId) {
        this.ensureOrganisationUnitsInitialized();
        this.ensureCrownCourtMappingsInitialized();
        OrganisationUnit organisationUnit = this.organisationUnitMapByIdCache.get(courtCentreId);
        return organisationUnit == null
                ? Optional.empty()
                : Optional.ofNullable(this.crownCourtMappingListMapCache.get(organisationUnit.getOucode()));
    }

    public Optional<List<CourtMapping>> getMagsCourtMappingsMapCache(final UUID courtCentreId) {
        this.ensureOrganisationUnitsInitialized();
        OrganisationUnit organisationUnit = this.organisationUnitMapByIdCache.get(courtCentreId);
        if (organisationUnit == null) {
            return Optional.empty();
        }

        String oucode = organisationUnit.getOucode();

        // Ensure safe lazy load
        return Optional.ofNullable(
                this.magsCourtMappingListMapCache.computeIfAbsent(oucode, key ->
                        this.referenceDataLoader.getXhibitMagsCourtMappings(oucode)
                                .map(m -> m.getCpXhibitCourtMappings())
                                .orElse(null))
        );
    }

    public Optional<Judiciary> getJudiciariesMapCache(final UUID judiciaryId) {
        return Optional.ofNullable(
                this.judiciariesMapCache.computeIfAbsent(judiciaryId,
                        key -> this.referenceDataLoader.getJudiciary(judiciaryId).orElse(null)));
    }

    public CourtRoomMappingsList getCourtRoomMappingsMapCache(final UUID courtCentreId) {
        return this.courtRoomMappingsMapCache.computeIfAbsent(courtCentreId,
                key -> this.referenceDataLoader.getCourtRoomMappingsList(courtCentreId).orElse(null));
    }

    public Optional<CourtRoomMapping> getCourtRoomMappingByCourtCentreAndCourtRoom(final UUID courtCentreId, final UUID courtRoomId) {
        return this.courtRoomMappingByCourtCentreRoomMap.computeIfAbsent(new CourtCentreRoomKey(courtCentreId, courtRoomId), key -> {
            return this.referenceDataLoader.getCourtRoomMappingsList(courtCentreId)
                    .orElseThrow(() -> new InvalidReferenceDataException("Invalid object courtRoomMappings"))
                    .getCpXhibitCourtRoomMappings()
                    .stream()
                    .filter(c -> courtRoomId.equals(c.getCourtRoomUUID()))
                    .findFirst();
        });
    }

    public Optional<OrganisationUnit> getOrganisationUnitMapCache(final String ouCode) {
        return Optional.ofNullable(
                this.organisationUnitMapCache.computeIfAbsent(ouCode,
                        key -> this.referenceDataLoader.getOrganisationUnitByOuCode(ouCode).orElse(null)));
    }

    public List<JsonObject> getCpCourtRoomCache(final UUID courtCentreId) {
        return this.cpCourtRoomCache.computeIfAbsent(courtCentreId,
                key -> this.referenceDataLoader.getCpCourtRoom(courtCentreId));
    }

    public Optional<HearingType> getHearingTypeCache(final UUID hearingTypeId) {
        this.ensureHearingTypesInitialized();
        return Optional.ofNullable(this.hearingTypesMapCache.get(hearingTypeId));
    }

    public Optional<HearingType> getHearingTypeCodeCache(final String hearingCode) {
        this.ensureHearingTypesInitialized();
        return Optional.ofNullable(this.hearingTypesCodesMapCache.get(hearingCode));
    }

    public List<CourtLocation> getCrownCourtLocationsCache(final String crestCourtId) {
        this.ensureCrownCourtMappingsInitialized();
        return this.crownCourtLocationListMapCache.get(crestCourtId);
    }

    private synchronized void ensureHearingTypesInitialized() {
        if (this.hearingTypesMapCache.isEmpty() || this.hearingTypesCodesMapCache.isEmpty()) {
            this.initAllHearingTypes();
        }
    }

    private synchronized void ensureCrownCourtMappingsInitialized() {
        if (this.crownCourtLocationListMapCache.isEmpty() || this.crownCourtMappingListMapCache.isEmpty()) {
            this.initCrownCourtMappingsList();
        }
    }

    private void initCrownCourtMappingsList() {
        this.referenceDataLoader.getXhibitCrownCourtMappings().ifPresent(courtMappingsList -> {
            courtMappingsList.getCpXhibitCourtMappings().forEach(courtMapping -> {
                this.crownCourtLocationListMapCache
                        .computeIfAbsent(courtMapping.getCrestCourtId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                        .add(this.createCourtLocation(courtMapping));

                this.crownCourtMappingListMapCache
                        .computeIfAbsent(courtMapping.getOucode(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                        .add(courtMapping);
            });
        });
    }

    public void initAllHearingTypes() {
        this.referenceDataLoader.getAllHearingTypesList().ifPresent(this::getHearingTypes);
    }

    public void initOrganisationUnitList() {
        this.referenceDataLoader.getOrganisationUnitList().ifPresent(this::getOrganisationUnits);
    }

    public CourtLocation createCourtLocation(final CourtMapping courtMapping) {
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
        hearingTypesList.getHearingTypes().forEach(hearingType -> {
            this.hearingTypesMapCache.putIfAbsent(hearingType.getId(), hearingType);
            this.hearingTypesCodesMapCache.putIfAbsent(hearingType.getHearingCode(), hearingType);
        });
    }

    private void getOrganisationUnits(OrganisationUnitList organisationUnitList) {
        organisationUnitList.getOrganisationunits().forEach(unit -> {
            this.organisationUnitMapByIdCache.putIfAbsent(unit.getId(), unit);
            this.organisationUnitMapCache.putIfAbsent(unit.getOucode(), unit);
        });
    }

    private synchronized void ensureOrganisationUnitsInitialized() {
        if (this.organisationUnitMapByIdCache.isEmpty() || this.organisationUnitMapCache.isEmpty()) {
            this.initOrganisationUnitList();
        }
    }
}
