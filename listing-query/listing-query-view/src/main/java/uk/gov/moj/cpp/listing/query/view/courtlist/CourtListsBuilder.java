package uk.gov.moj.cpp.listing.query.view.courtlist;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class CourtListsBuilder {

    private static final String CREST_COURT_SITE_CODE = "crestCourtSiteCode";

    private final Map<String, List<FlatHearing>> crestCourtSiteCodeHearingsMap = new HashMap<>();
    private final Map<String, List<Sitting>> crestCourtSiteCodeSittingsMap = new HashMap<>();

    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;
    private UUID courtCentreId;

    private CourtListsBuilder(final UUID courtCentreId, final CommonXhibitReferenceDataService commonXhibitReferenceDataService) {
        this.courtCentreId = courtCentreId;
        this.commonXhibitReferenceDataService = commonXhibitReferenceDataService;
    }

    public static CourtListsBuilder forCourtCentre(final UUID courtCentreId,
                                                   final CommonXhibitReferenceDataService commonXhibitReferenceDataService) {

        return new CourtListsBuilder(courtCentreId, commonXhibitReferenceDataService);
    }

    public CourtListsBuilder prepareEmptyCourtSiteHearings() {

        commonXhibitReferenceDataService.getCrestCourtSitesForCourtCentre(courtCentreId)
                .forEach(courtSite -> crestCourtSiteCodeHearingsMap.put(courtSite.getString(CREST_COURT_SITE_CODE),
                        new ArrayList<>()));

        return this;
    }

    public CourtListsBuilder assignHearingsToCourtSitesUsingCourtRoom(final List<FlatHearing> flatHearings) {

        for (final FlatHearing flatHearing : flatHearings) {

            final String crestCourtSiteCode = getCrestCourtSiteCodeForCourtRoom(flatHearing.getCourtRoomId());

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

    public JsonArray buildCourtListsArray() {

        final JsonArrayBuilder courtListArray = Json.createArrayBuilder();

        for (final Map.Entry<String, List<Sitting>> entry : crestCourtSiteCodeSittingsMap.entrySet()) {

            final String crestCourtSiteCode = entry.getKey();
            final List<Sitting> sittings = entry.getValue();

            final JsonObject crestCourtSiteJson = getCrestCourtSiteJson(crestCourtSiteCode);

            courtListArray.add(courtSiteCourtList(crestCourtSiteJson, sittings));
        }

        return courtListArray.build();
    }

    private JsonObject getCrestCourtSiteJson(final String crestCourtSiteCode) {

        return commonXhibitReferenceDataService.getCrestCourtSitesForCourtCentre(courtCentreId)
                .stream().filter(courtSite -> crestCourtSiteCode.equals(courtSite.getString(CREST_COURT_SITE_CODE)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find site"));
    }

    private JsonObject courtSiteCourtList(final JsonObject crestCourtSiteJson, final List<Sitting> sittings) {

        return Json.createObjectBuilder()
                .add("crestCourtSite", crestCourtSiteJson)
                .add("sittings", SittingsJsonGenerator.buildSittingsJson(sittings))
                .build();
    }

    private String getCrestCourtSiteCodeForCourtRoom(final Optional<UUID> courtRoomUUID) {

        if (!courtRoomUUID.isPresent()) {
            return commonXhibitReferenceDataService.getDefaultCrestCourtSiteCode(courtCentreId);
        }

        final Optional<CourtRoomMapping> courtRoomMapping = commonXhibitReferenceDataService.getCourtRoom(courtCentreId,
                courtRoomUUID.get());

        return courtRoomMapping.isPresent() ? courtRoomMapping.get().getCrestCourtSiteCode()
                : commonXhibitReferenceDataService.getDefaultCrestCourtSiteCode(courtCentreId);
    }
}
