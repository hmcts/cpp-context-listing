package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import uk.gov.justice.services.messaging.JsonObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CourtListsBuilderTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.of(2024, 1, 15);
    private static final String END_DATE = "";

    private CommonXhibitReferenceDataService referenceDataService;

    @BeforeEach
    void setUp() {
        referenceDataService = mock(CommonXhibitReferenceDataService.class);
    }

    @Test
    void shouldCreateBuilderInstanceViaFactoryMethod() {
        final CourtListsBuilder builder = CourtListsBuilder.forCourtCentre(referenceDataService);

        assertThat(builder, is(notNullValue()));
    }

    @Test
    void shouldReturnBuilderFromPrepareEmptyCourtSiteHearingsForFluentChaining() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A"), courtSite("B")));

        final CourtListsBuilder result = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID);

        assertThat(result, is(notNullValue()));
    }

    @Test
    void shouldReturnBuilderFromAssignHearingsForFluentChaining() {
        final UUID roomId = randomUUID();
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomId))
                .thenReturn(of(courtRoomMapping("A")));

        final CourtListsBuilder result = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(of(roomId))));

        assertThat(result, is(notNullValue()));
    }

    @Test
    void shouldReturnBuilderFromGroupFlatHearingsForFluentChaining() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final CourtListsBuilder result = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(empty())))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE);

        assertThat(result, is(notNullValue()));
    }

    @Test
    void shouldUseDefaultCourtSiteWhenHearingHasNoCourtRoom() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(empty())))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtLists.size(), is(1));
        assertThat(courtLists.getJsonObject(0).getJsonArray("sittings").size(), is(1));
    }

    @Test
    void shouldAssignHearingToMappedCourtSiteWhenCourtRoomIsPresent() {
        final UUID roomId = randomUUID();
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomId))
                .thenReturn(of(courtRoomMapping("A")));

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(of(roomId))))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtLists.size(), is(1));
        assertThat(courtLists.getJsonObject(0).getJsonArray("sittings").size(), is(1));
    }

    @Test
    void shouldFallBackToDefaultCourtSiteWhenCourtRoomMappingIsAbsent() {
        final UUID roomId = randomUUID();
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomId)).thenReturn(empty());
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(of(roomId))))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtLists.size(), is(1));
        assertThat(courtLists.getJsonObject(0).getJsonArray("sittings").size(), is(1));
    }

    @Test
    void shouldProduceOneCourtListEntryPerCourtSite() {
        final UUID roomA = randomUUID();
        final UUID roomB = randomUUID();
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A"), courtSite("B")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomA))
                .thenReturn(of(courtRoomMapping("A")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomB))
                .thenReturn(of(courtRoomMapping("B")));

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID,
                        asList(flatHearing(of(roomA)), flatHearing(of(roomB))))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtLists.size(), is(2));
    }

    @Test
    void shouldIncludeCrestCourtSiteAndSittingsInEachCourtListEntry() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(empty())))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonObject entry = courtLists.getJsonObject(0);
        assertThat(entry.containsKey("crestCourtSite"), is(true));
        assertThat(entry.containsKey("sittings"), is(true));
    }

    @Test
    void shouldPopulateCrestCourtSiteDataFromReferenceDataService() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(empty())))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonObject crestCourtSite = courtLists.getJsonObject(0).getJsonObject("crestCourtSite");
        assertThat(crestCourtSite.getString("crestCourtSiteCode"), is("A"));
        assertThat(crestCourtSite.getString("crestCourtSiteName"), is("Site A"));
    }

    @Test
    void shouldProduceEmptyCourtListsArrayWhenNoCourtSites() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(emptyList());

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, emptyList())
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtLists.size(), is(0));
    }

    @Test
    void shouldProduceCourtListEntryWithNoSittingsWhenNoHearingsAssigned() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, emptyList())
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtLists.size(), is(1));
        assertThat(courtLists.getJsonObject(0).getJsonArray("sittings").size(), is(0));
    }

    @Test
    void shouldGroupHearingsWithSameDateAndCourtRoomIntoOneSitting() {
        final UUID roomId = randomUUID();
        final UUID judicialId = randomUUID();
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomId))
                .thenReturn(of(courtRoomMapping("A")));

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID,
                        asList(
                                flatHearingWithJudiciary(of(roomId), judicialId),
                                flatHearingWithJudiciary(of(roomId), judicialId),
                                flatHearingWithJudiciary(of(roomId), judicialId)))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonArray sittings = courtLists.getJsonObject(0).getJsonArray("sittings");
        assertThat(sittings.size(), is(1));
        assertThat(sittings.getJsonObject(0).getJsonArray("hearings").size(), is(3));
    }

    @Test
    void shouldCreateSeparateSittingsForHearingsInDifferentCourtRooms() {
        final UUID roomA = randomUUID();
        final UUID roomB = randomUUID();
        final UUID judicialId = randomUUID();
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomA))
                .thenReturn(of(courtRoomMapping("A")));
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomB))
                .thenReturn(of(courtRoomMapping("A")));

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID,
                        asList(
                                flatHearingWithJudiciary(of(roomA), judicialId),
                                flatHearingWithJudiciary(of(roomB), judicialId)))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonArray sittings = courtLists.getJsonObject(0).getJsonArray("sittings");
        assertThat(sittings.size(), is(2));
    }

    @Test
    void shouldIncludeSittingDateInEachSittingEntry() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(empty())))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonObject sitting = courtLists.getJsonObject(0).getJsonArray("sittings").getJsonObject(0);
        assertThat(sitting.getString("sittingDate"), is(START_DATE.toString()));
    }

    @Test
    void shouldIncludeHearingTypeInEachHearing() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(flatHearing(empty())))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonObject hearing = courtLists.getJsonObject(0)
                .getJsonArray("sittings").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getJsonObject("hearingType").getString("description"), is("Trial"));
    }

    @Test
    void shouldOrderCourtSitesMainSiteFirstThenAlphabeticallyAndCourtRoomsByCrestNumber() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(
                        courtSite("B", "MANSFIELD MAGISTRATES' COURT", "DERBY"),
                        courtSite("C", "Chesterfield Justice Centre", "DERBY"),
                        courtSite("A", "DERBY", "DERBY")));

        final UUID derby10 = mappedRoom("A", "Court 10", "Courtroom 10");
        final UUID derby6 = mappedRoom("A", "Court 6", "Courtroom 06");
        final UUID derby1 = mappedRoom("A", "Court 1", "Courtroom 01");
        final UUID derby3 = mappedRoom("A", "Court 3", "Courtroom 03");
        final UUID derby2 = mappedRoom("A", "Court 2", "Courtroom 02");
        final UUID mansfield4 = mappedRoom("B", "Court 4", "Courtroom 04 (Mansfield)");
        final UUID mansfield3 = mappedRoom("B", "Court 3", "Courtroom 03 (Mansfield)");
        final UUID chesterfield1 = mappedRoom("C", "Court 1", "Courtroom 01 (Chesterfield)");

        // Hearings arrive in a scrambled order, as they do from the range search
        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(
                        flatHearing(of(mansfield4)),
                        flatHearing(of(derby3)),
                        flatHearing(of(derby10)),
                        flatHearing(of(chesterfield1)),
                        flatHearing(of(derby2)),
                        flatHearing(of(mansfield3)),
                        flatHearing(of(derby6)),
                        flatHearing(of(derby1))))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtSiteNames(courtLists),
                contains("DERBY", "Chesterfield Justice Centre", "MANSFIELD MAGISTRATES' COURT"));
        assertThat(courtRoomNames(courtLists.getJsonObject(0)),
                contains("Courtroom 01", "Courtroom 02", "Courtroom 03", "Courtroom 06", "Courtroom 10"));
        assertThat(courtRoomNames(courtLists.getJsonObject(1)),
                contains("Courtroom 01 (Chesterfield)"));
        assertThat(courtRoomNames(courtLists.getJsonObject(2)),
                contains("Courtroom 03 (Mansfield)", "Courtroom 04 (Mansfield)"));
    }

    @Test
    void shouldPlaceCourtRoomsWithoutCrestMappingAfterMappedCourtRooms() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A", "DERBY", "DERBY")));
        when(referenceDataService.getDefaultCrestCourtSiteCode(COURT_CENTRE_ID)).thenReturn("A");

        final UUID unmapped = randomUUID();
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, unmapped)).thenReturn(empty());
        when(referenceDataService.getCourtRoomDisplayName(COURT_CENTRE_ID, unmapped)).thenReturn(of("Courtroom 01 (Annex)"));
        final UUID derby2 = mappedRoom("A", "Court 2", "Courtroom 02");

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(
                        flatHearing(of(unmapped)),
                        flatHearing(of(derby2))))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        assertThat(courtRoomNames(courtLists.getJsonObject(0)), contains("Courtroom 02", "Courtroom 01 (Annex)"));
    }

    @Test
    void shouldPutSiteCodeAFirstEvenWhenItsNameDoesNotMatchTheCourtName() {
        // Real SHEFFIELD (crestCourtId 451) sites: alphabetical alone would wrongly put Doncaster first
        final List<JsonObject> sites = new ArrayList<>(asList(
                courtSite("E", "DONCASTER CROWN COURT", "SHEFFIELD"),
                courtSite("A", "THE LAW COURTS WEST BAR SHEFFIELD", "SHEFFIELD")));

        sites.sort(CourtListsBuilder.COURT_SITE_ORDER);

        assertThat(siteNames(sites), contains("THE LAW COURTS WEST BAR SHEFFIELD", "DONCASTER CROWN COURT"));
    }

    @Test
    void shouldOrderNonMainSitesAlphabeticallyByNameNotBySiteCode() {
        // Real CAMBRIDGE (crestCourtId 410) sites
        final List<JsonObject> sites = new ArrayList<>(asList(
                courtSite("D", "PETERBOROUGH MAGISTRATES COURT", "CAMBRIDGE"),
                courtSite("B", "PETERBOROUGH", "CAMBRIDGE"),
                courtSite("C", "HUNTINGDON LAW COURTS", "CAMBRIDGE"),
                courtSite("A", "CAMBRIDGE", "CAMBRIDGE")));

        sites.sort(CourtListsBuilder.COURT_SITE_ORDER);

        assertThat(siteNames(sites),
                contains("CAMBRIDGE", "HUNTINGDON LAW COURTS", "PETERBOROUGH", "PETERBOROUGH MAGISTRATES COURT"));
    }

    @Test
    void shouldFallBackToSiteMatchingCourtNameWhenThereIsNoSiteCodeA() {
        final List<JsonObject> sites = new ArrayList<>(asList(
                courtSite("C", "ALPHA ANNEX", "ZETA"),
                courtSite("B", "ZETA", "ZETA")));

        sites.sort(CourtListsBuilder.COURT_SITE_ORDER);

        assertThat(siteNames(sites), contains("ZETA", "ALPHA ANNEX"));
    }

    @Test
    void shouldOrderCourtSitesAlphabeticallyWhenNoMainSiteCanBeIdentified() {
        final List<JsonObject> sites = new ArrayList<>(asList(
                courtSite("B", "Zeta Site", "DERBY"),
                courtSite("C", "Alpha Site", "DERBY")));

        sites.sort(CourtListsBuilder.COURT_SITE_ORDER);

        assertThat(siteNames(sites), contains("Alpha Site", "Zeta Site"));
    }

    private List<String> siteNames(final List<JsonObject> sites) {
        return sites.stream().map(site -> site.getString("crestCourtSiteName")).collect(Collectors.toList());
    }

    @Test
    void shouldParseTrailingCourtRoomNumber() {
        assertThat(CourtListsBuilder.trailingNumber("Court 3"), is(of(3)));
        assertThat(CourtListsBuilder.trailingNumber("Court 10"), is(of(10)));
        assertThat(CourtListsBuilder.trailingNumber("Court -99"), is(of(99)));
        assertThat(CourtListsBuilder.trailingNumber("Annex"), is(empty()));
        assertThat(CourtListsBuilder.trailingNumber(null), is(empty()));
        assertThat(CourtListsBuilder.trailingNumber("Court 7  "), is(of(7)));
        assertThat(CourtListsBuilder.trailingNumber("Court 1A"), is(empty()));
        assertThat(CourtListsBuilder.trailingNumber("   "), is(empty()));
        assertThat(CourtListsBuilder.trailingNumber("Court 99999999999"), is(empty()));
    }

    @Test
    void shouldParseTrailingNumberOfVeryLongCourtRoomNameQuickly() {
        final String longDigitsThenText = "1".repeat(100_000) + " x" + " ".repeat(100_000);
        final String longNameEndingInNumber = " ".repeat(100_000) + "Court 4";

        assertTimeout(Duration.ofSeconds(1), () -> {
            assertThat(CourtListsBuilder.trailingNumber(longDigitsThenText), is(empty()));
            assertThat(CourtListsBuilder.trailingNumber(longNameEndingInNumber), is(of(4)));
        });
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    @Test
    void shouldOrderHearingsWithinCourtRoomByStartTimeThenListingSequence() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        final UUID room = mappedRoom("A", "Court 1", "Courtroom 01");
        final UUID judicialId = randomUUID();

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(
                        dailyFlatHearing(room, judicialId, "CASE-D", "14:00", 1),
                        dailyFlatHearing(room, judicialId, "CASE-C", "10:00", 3),
                        dailyFlatHearing(room, judicialId, "CASE-E", "10:30", null),
                        dailyFlatHearing(room, judicialId, "CASE-A", "10:00", 1),
                        dailyFlatHearing(room, judicialId, "CASE-F", "10:30", 2),
                        dailyFlatHearing(room, judicialId, "CASE-B", "10:00", 2)))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonArray sittings = courtLists.getJsonObject(0).getJsonArray("sittings");
        assertThat(sittings.size(), is(1));
        assertThat(caseReferences(sittings.getJsonObject(0)),
                contains("CASE-A", "CASE-B", "CASE-C", "CASE-F", "CASE-E", "CASE-D"));
    }

    @Test
    void shouldOrderHearingsWithinEachCourtRoomIndependently() {
        when(referenceDataService.getCrestCourtSitesForCrownCourtCentre(COURT_CENTRE_ID))
                .thenReturn(asList(courtSite("A")));
        final UUID room2 = mappedRoom("A", "Court 2", "Courtroom 02");
        final UUID room1 = mappedRoom("A", "Court 1", "Courtroom 01");
        final UUID judicialId = randomUUID();

        final JsonArray courtLists = CourtListsBuilder.forCourtCentre(referenceDataService)
                .prepareEmptyCourtSiteHearings(COURT_CENTRE_ID)
                .assignHearingsToCourtSitesUsingCourtRoom(COURT_CENTRE_ID, asList(
                        dailyFlatHearing(room2, judicialId, "R2-SECOND", "10:00", 2),
                        dailyFlatHearing(room1, judicialId, "R1-SECOND", "11:00", 1),
                        dailyFlatHearing(room2, judicialId, "R2-FIRST", "10:00", 1),
                        dailyFlatHearing(room1, judicialId, "R1-FIRST", "10:00", 1)))
                .groupFlatHearingsIntoSittings(START_DATE, END_DATE)
                .buildCourtListsArray(COURT_CENTRE_ID);

        final JsonObject courtList = courtLists.getJsonObject(0);
        assertThat(courtRoomNames(courtList), contains("Courtroom 01", "Courtroom 02"));
        assertThat(caseReferences(courtList.getJsonArray("sittings").getJsonObject(0)), contains("R1-FIRST", "R1-SECOND"));
        assertThat(caseReferences(courtList.getJsonArray("sittings").getJsonObject(1)), contains("R2-FIRST", "R2-SECOND"));
    }

    private List<String> caseReferences(final JsonObject sitting) {
        return sitting.getJsonArray("hearings").getValuesAs(JsonObject.class).stream()
                .map(hearing -> hearing.getJsonObject("caseIdentifier").getString("caseReference"))
                .collect(Collectors.toList());
    }

    private FlatHearing dailyFlatHearing(final UUID courtRoomId, final UUID judicialId, final String caseReference,
                                         final String startTime, final Integer sequence) {
        final JsonObjectBuilder hearingDay = JsonObjects.createObjectBuilder()
                .add("hearingDate", START_DATE.toString())
                .add("startTime", START_DATE + "T" + startTime + ":00.000Z")
                .add("endTime", START_DATE + "T16:00:00.000Z")
                .add("courtRoomId", courtRoomId.toString());
        if (sequence != null) {
            hearingDay.add("sequence", sequence);
        }
        final JsonObject caseHearings = JsonObjects.createObjectBuilder()
                .add("type", JsonObjects.createObjectBuilder().add("description", "Trial"))
                .add("judiciary", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder().add("judicialId", judicialId.toString())))
                .add("hearingDays", JsonObjects.createArrayBuilder().add(hearingDay))
                .add("listedCases", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("caseIdentifier", JsonObjects.createObjectBuilder().add("caseReference", caseReference))
                                .add("defendants", JsonObjects.createArrayBuilder())))
                .build();
        return new FlatHearing(START_DATE, caseHearings.getJsonArray("judiciary"), of(courtRoomId), caseHearings, false);
    }

    private UUID mappedRoom(final String crestCourtSiteCode, final String crestCourtRoomName, final String displayName) {
        final UUID roomId = randomUUID();
        when(referenceDataService.getCourtRoom(COURT_CENTRE_ID, roomId)).thenReturn(of(
                new CourtRoomMapping.Builder()
                        .withCrestCourtSiteCode(crestCourtSiteCode)
                        .withCrestCourtRoomName(crestCourtRoomName)
                        .build()));
        when(referenceDataService.getCourtRoomDisplayName(COURT_CENTRE_ID, roomId)).thenReturn(of(displayName));
        return roomId;
    }

    private List<String> courtSiteNames(final JsonArray courtLists) {
        return courtLists.getValuesAs(JsonObject.class).stream()
                .map(courtList -> courtList.getJsonObject("crestCourtSite").getString("crestCourtSiteName"))
                .collect(Collectors.toList());
    }

    private List<String> courtRoomNames(final JsonObject courtList) {
        return courtList.getJsonArray("sittings").getValuesAs(JsonObject.class).stream()
                .map(sitting -> sitting.getString("courtRoomName"))
                .collect(Collectors.toList());
    }

    private JsonObject courtSite(final String code, final String siteName, final String courtName) {
        return JsonObjects.createObjectBuilder()
                .add("crestCourtSiteCode", code)
                .add("crestCourtSiteName", siteName)
                .add("crestCourtName", courtName)
                .build();
    }

    private JsonObject courtSite(final String code) {
        return JsonObjects.createObjectBuilder()
                .add("crestCourtSiteCode", code)
                .add("crestCourtSiteName", "Site " + code)
                .build();
    }

    private CourtRoomMapping courtRoomMapping(final String crestCourtSiteCode) {
        return new CourtRoomMapping.Builder().withCrestCourtSiteCode(crestCourtSiteCode).build();
    }

    private FlatHearing flatHearing(final Optional<UUID> courtRoomId) {
        return flatHearingWithJudiciary(courtRoomId, randomUUID());
    }

    private FlatHearing flatHearingWithJudiciary(final Optional<UUID> courtRoomId, final UUID judicialId) {
        final JsonObject caseHearings = buildWeekCommencingCaseHearings(judicialId);
        return new FlatHearing(
                START_DATE,
                caseHearings.getJsonArray("judiciary"),
                courtRoomId,
                caseHearings,
                true);
    }

    private JsonObject buildWeekCommencingCaseHearings(final UUID judicialId) {
        return JsonObjects.createObjectBuilder()
                .add("weekCommencingStartDate", START_DATE.toString())
                .add("weekCommencingEndDate", "2024-01-19")
                .add("type", JsonObjects.createObjectBuilder()
                        .add("description", "Trial")
                        .build())
                .add("judiciary", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("judicialId", judicialId.toString())))
                .add("listedCases", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("restrictFromCourtList", false)
                                .add("caseIdentifier", JsonObjects.createObjectBuilder()
                                        .add("caseReference", "T12345")
                                        .build())
                                .add("defendants", JsonObjects.createArrayBuilder().build())))
                .build();
    }
}
