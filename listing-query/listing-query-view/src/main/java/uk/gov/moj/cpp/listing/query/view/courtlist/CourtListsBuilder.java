package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.common.xhibit.CourtRoomNumbers;
import uk.gov.moj.cpp.listing.common.xhibit.ThreadLocalCommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
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
import javax.json.JsonString;

public class CourtListsBuilder {

    private static final String CREST_COURT_SITE_CODE = "crestCourtSiteCode";
    private static final String CREST_COURT_SITE_NAME = "crestCourtSiteName";
    private static final String CREST_COURT_NAME = "crestCourtName";
    private static final String MAIN_CREST_COURT_SITE_CODE = "A";
    private static final int UNMAPPED_COURT_ROOM_SORT_KEY = CourtRoomNumbers.sortKey(CourtRoomNumbers.UNMAPPED_COURT_ROOM_NUMBER);
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListsBuilder.class);

    /**
     * Ordering of a crown court centre's CREST court sites:
     * 1. the main site first - CREST site code "A", or, failing that, the site whose CREST site name matches
     *    the CREST court name;
     * 2. the remaining sites by CREST site code (A, B, C...), as the XHIBIT feed lists them.
     */
    static final Comparator<JsonObject> COURT_SITE_ORDER =
            Comparator.<JsonObject, Integer>comparing(CourtListsBuilder::courtSiteRank)
                    .thenComparing(courtSite -> stringOrEmpty(courtSite, CREST_COURT_SITE_CODE));

    /**
     * Ordering of hearings within a sitting (courtroom): by start time, then by the listing sequence for that
     * hearing day. Hearings without a start time or sequence go last; the sort is stable for ties.
     */
    static final Comparator<Hearing> HEARING_ORDER =
            comparing(Hearing::getStartTime, nullsLast(naturalOrder()))
                    .thenComparing(Hearing::getSequence, nullsLast(naturalOrder()));


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
                LOGGER.info("courtCentreId={}, courtRoomId={}, flatHearingId={}", courtCentreId, !flatHearing.getCourtRoomId().isEmpty()?flatHearing.getCourtRoomId().get():"No Value", nonNull(flatHearing.getCaseHearings()) && flatHearing.getCaseHearings().containsKey("id")?flatHearing.getCaseHearings().getString("id"):"No Value");
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

        final List<JsonObject> orderedCourtSites = crestCourtSiteCodeSittingsMap.keySet().stream()
                .map(crestCourtSiteCode -> getCrestCourtSiteJson(courtCentreId, crestCourtSiteCode))
                .sorted(COURT_SITE_ORDER)
                .collect(toList());

        for (final JsonObject crestCourtSiteJson : orderedCourtSites) {
            final List<Sitting> sittings = crestCourtSiteCodeSittingsMap.get(crestCourtSiteJson.getString(CREST_COURT_SITE_CODE));
            enrichSittingsWithCourtRoomNames(courtCentreId, sittings);
            sortSittingsByCourtRoom(courtCentreId, sittings);
            sittings.forEach(sitting -> sitting.getHearings().sort(HEARING_ORDER));
            courtListArray.add(courtSiteCourtList(crestCourtSiteJson, sittings));
        }

        return courtListArray.build();
    }

    /**
     * Orders sittings within a court site by sitting date, then by CREST courtroom number (from the
     * cp-xhibit courtroom mapping, e.g. "Court 3" -> 3), then by courtroom display name. Courtrooms without a
     * CREST mapping go after the mapped ones. The sort is stable, so sittings in the same courtroom (e.g. different
     * judges) keep their existing relative order.
     */
    private void sortSittingsByCourtRoom(final UUID courtCentreId, final List<Sitting> sittings) {
        final Map<Sitting, Integer> courtRoomNumbers = new IdentityHashMap<>();
        sittings.forEach(sitting -> courtRoomNumbers.put(sitting, crestCourtRoomNumber(courtCentreId, sitting)));

        sittings.sort(comparing((Sitting sitting) -> sitting.getSittingKey().getSittingDate(), nullsLast(naturalOrder()))
                .thenComparing(courtRoomNumbers::get)
                .thenComparing(Sitting::getCourtRoomName, nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    private int crestCourtRoomNumber(final UUID courtCentreId, final Sitting sitting) {
        final Optional<UUID> courtRoomId = sitting.getSittingKey().getCourtRoomId();
        if (courtRoomId.isEmpty()) {
            return UNMAPPED_COURT_ROOM_SORT_KEY;
        }
        try {
            return threadLocalCommonXhibitReferenceDataService.get().getCourtRoom(courtCentreId, courtRoomId.get())
                    .map(CourtRoomMapping::getCrestCourtRoomName)
                    .flatMap(CourtRoomNumbers::trailingNumber)
                    .orElse(UNMAPPED_COURT_ROOM_SORT_KEY);
        } catch (final RuntimeException e) {
            LOGGER.warn("Cannot resolve CREST court room number for courtCentreId={}, courtRoomId={}", courtCentreId, courtRoomId.get());
            return UNMAPPED_COURT_ROOM_SORT_KEY;
        }
    }

    private static int courtSiteRank(final JsonObject courtSite) {
        if (MAIN_CREST_COURT_SITE_CODE.equalsIgnoreCase(stringOrEmpty(courtSite, CREST_COURT_SITE_CODE))) {
            return 0;
        }
        final String siteName = stringOrEmpty(courtSite, CREST_COURT_SITE_NAME);
        return !siteName.isEmpty() && siteName.equalsIgnoreCase(stringOrEmpty(courtSite, CREST_COURT_NAME)) ? 1 : 2;
    }

    private static String stringOrEmpty(final JsonObject json, final String key) {
        return json.get(key) instanceof JsonString ? json.getString(key) : "";
    }

    private void enrichSittingsWithCourtRoomNames(final UUID courtCentreId, final List<Sitting> sittings) {
        for (final Sitting sitting : sittings) {
            sitting.getSittingKey().getCourtRoomId().ifPresent(courtRoomId -> {
                threadLocalCommonXhibitReferenceDataService.get()
                        .getCourtRoomDisplayName(courtCentreId, courtRoomId)
                        .ifPresent(sitting::setCourtRoomName);
                threadLocalCommonXhibitReferenceDataService.get()
                        .getWelshCourtRoomDisplayName(courtCentreId, courtRoomId)
                        .ifPresent(sitting::setWelshCourtRoomName);
            });
        }
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