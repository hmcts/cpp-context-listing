package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

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

    // ─── helpers ────────────────────────────────────────────────────────────────

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
