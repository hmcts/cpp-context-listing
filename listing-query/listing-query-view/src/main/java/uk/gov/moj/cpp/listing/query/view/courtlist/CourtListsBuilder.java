package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Objects.nonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.common.xhibit.ThreadLocalCommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class CourtListsBuilder {

    private static final String CREST_COURT_SITE_CODE = "crestCourtSiteCode";
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListsBuilder.class);


    private final Map<String, List<FlatHearing>> crestCourtSiteCodeHearingsMap = new ConcurrentHashMap<>();
    private final Map<String, List<Sitting>> crestCourtSiteCodeSittingsMap = new ConcurrentHashMap<>();

    private final ThreadLocalCommonXhibitReferenceDataService threadLocalCommonXhibitReferenceDataService = new ThreadLocalCommonXhibitReferenceDataService();

    private CourtListsBuilder(final CommonXhibitReferenceDataService commonXhibitReferenceDataService) {
        this.threadLocalCommonXhibitReferenceDataService.set(commonXhibitReferenceDataService);
    }

    public static CourtListsBuilder forCourtCentre(final CommonXhibitReferenceDataService commonXhibitReferenceDataService) {
        return new CourtListsBuilder(commonXhibitReferenceDataService);
    }

    public CourtListsBuilder prepareEmptyCourtSiteHearings(final UUID courtCentreId) {
        threadLocalCommonXhibitReferenceDataService.get().getCrestCourtSitesForCrownCourtCentre(courtCentreId)
                .forEach(courtSite -> crestCourtSiteCodeHearingsMap.put(courtSite.getString(CREST_COURT_SITE_CODE),
                        new ArrayList<>()));

        return this;
    }

    public CourtListsBuilder assignHearingsToCourtSitesUsingCourtRoom(final UUID courtCentreId, final List<FlatHearing> flatHearings) {
        for (final FlatHearing flatHearing : flatHearings) {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("courtCentreId={}, courtRoomId={}, flatHearingId={}", courtCentreId, flatHearing.getCourtRoomId().map(UUID::toString).orElse("No Value"), nonNull(flatHearing.getCaseHearings()) && flatHearing.getCaseHearings().containsKey("id")?flatHearing.getCaseHearings().getString("id"):"No Value");
            }
            final String crestCourtSiteCode = getCrestCourtSiteCodeForCourtRoom(courtCentreId, flatHearing.getCourtRoomId());
            crestCourtSiteCodeHearingsMap.get(crestCourtSiteCode).add(flatHearing);
        }

        return this;
    }

    public CourtListsBuilder groupFlatHearingsIntoSittings(final LocalDate startDate, final String endDate) {
        for (final Map.Entry<String, List<FlatHearing>> entry : crestCourtSiteCodeHearingsMap.entrySet()) {
            final String crestCourtSiteCode = entry.getKey();
            final List<FlatHearing> courtSiteFlatHearings = entry.getValue();
            final List<Sitting> courtSiteSittings = SittingsPojoBuilder.assignFlatHearingsToSittings(courtSiteFlatHearings, startDate, endDate);
            crestCourtSiteCodeSittingsMap.put(crestCourtSiteCode, courtSiteSittings);
        }

        return this;
    }

    public JsonArray buildCourtListsArray(final UUID courtCentreId) {
        final JsonArrayBuilder courtListArray = JsonObjects.createArrayBuilder();

        for (final Map.Entry<String, List<Sitting>> entry : crestCourtSiteCodeSittingsMap.entrySet()) {
            final String crestCourtSiteCode = entry.getKey();
            final List<Sitting> sittings = entry.getValue();
            final JsonObject crestCourtSiteJson = getCrestCourtSiteJson(courtCentreId, crestCourtSiteCode);
            courtListArray.add(courtSiteCourtList(crestCourtSiteJson, sittings));
        }

        return courtListArray.build();
    }

    @PreDestroy
    public void unload() {
        threadLocalCommonXhibitReferenceDataService.unload();
    }

    private JsonObject getCrestCourtSiteJson(final UUID courtCentreId, final String crestCourtSiteCode) {
        return threadLocalCommonXhibitReferenceDataService.get().getCrestCourtSitesForCrownCourtCentre(courtCentreId)
                .stream().filter(courtSite -> crestCourtSiteCode.equals(courtSite.getString(CREST_COURT_SITE_CODE)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find site"));
    }

    private String getCrestCourtSiteCodeForCourtRoom(final UUID courtCentreId, final Optional<UUID> courtRoomUUID) {
        if (!courtRoomUUID.isPresent()) {
            return threadLocalCommonXhibitReferenceDataService.get().getDefaultCrestCourtSiteCode(courtCentreId);
        }
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("Thread: {} - Processing court room UUID: {} for court centre: {}",
                    Thread.currentThread().getName(), courtRoomUUID.get(), courtCentreId);
        }

        final Optional<CourtRoomMapping> courtRoomMapping = threadLocalCommonXhibitReferenceDataService.get()
                .getCourtRoom(courtCentreId, courtRoomUUID.get());

        return courtRoomMapping.isPresent() ? courtRoomMapping.get().getCrestCourtSiteCode()
                : threadLocalCommonXhibitReferenceDataService.get().getDefaultCrestCourtSiteCode(courtCentreId);
    }

    private JsonObject courtSiteCourtList(final JsonObject crestCourtSiteJson, final List<Sitting> sittings) {

        return JsonObjects.createObjectBuilder()
                .add("crestCourtSite", crestCourtSiteJson)
                .add("sittings", SittingsJsonGenerator.buildSittingsJson(sittings))
                .build();
    }

}