package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static uk.gov.moj.cpp.listing.command.api.service.HearingEnrichmentOrchestrator.stripRoomInfoIfAnyDraft;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * SPRDT-858: when any hearing day's session is draft, the hearing is treated as unallocated
 * for CROWN. The post-enrichment sweep must strip courtroom information so it never reaches
 * commands or downstream events. These tests exercise the sweep directly with real domain
 * objects, since the existing orchestrator tests use Mockito mocks where the sweep is a no-op.
 */
class HearingEnrichmentOrchestratorStripDraftRoomTest {

    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID OTHER_ROOM_ID = UUID.randomUUID();
    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final LocalDate DAY_1 = LocalDate.of(2026, 5, 27);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 5, 28);

    // ─── HearingListingNeeds (list-court-hearing path) ───────────────────

    @Test
    void listingNeeds_noHearingDays_returnsHearingUnchanged() {
        final HearingListingNeeds hearing = listingNeedsWithCourtCentreRoom()
                .withHearingDays(List.of())
                .build();

        final HearingListingNeeds result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result, is(sameInstance(hearing)));
    }

    @Test
    void listingNeeds_noDraftDay_returnsHearingUnchanged() {
        final HearingListingNeeds hearing = listingNeedsWithCourtCentreRoom()
                .withHearingDays(List.of(
                        day(DAY_1, ROOM_ID, false),
                        day(DAY_2, ROOM_ID, false)))
                .build();

        final HearingListingNeeds result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result, is(sameInstance(hearing)));
        assertThat(result.getCourtCentre().getRoomId(), is(notNullValue()));
        assertThat(result.getHearingDays(), everyItem(hasProperty("courtRoomId", is(notNullValue()))));
    }

    @Test
    void listingNeeds_allDaysDraft_stripsRoomFromDaysAndCourtCentre() {
        final HearingListingNeeds hearing = listingNeedsWithCourtCentreRoom()
                .withHearingDays(List.of(
                        day(DAY_1, ROOM_ID, true),
                        day(DAY_2, ROOM_ID, true)))
                .build();

        final HearingListingNeeds result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result.getHearingDays(), everyItem(hasProperty("courtRoomId", is(nullValue()))));
        assertThat(result.getCourtCentre().getRoomId(), is(nullValue()));
        assertThat(result.getCourtCentre().getRoomName(), is(nullValue()));
        // Courthouse identity preserved.
        assertThat(result.getCourtCentre().getId(), is(COURT_CENTRE_ID));
        assertThat(result.getCourtCentre().getName(), is("Liverpool Crown"));
    }

    @Test
    void listingNeeds_anyDayDraft_stripsAllRooms_anyToAllRule() {
        // First day is allocated, second day is draft. Per the "any → all" rule, BOTH days lose
        // their room and the hearing-level courtCentre loses its room too — the whole hearing is
        // treated as unallocated.
        final HearingListingNeeds hearing = listingNeedsWithCourtCentreRoom()
                .withHearingDays(List.of(
                        day(DAY_1, ROOM_ID, false),
                        day(DAY_2, OTHER_ROOM_ID, true)))
                .build();

        final HearingListingNeeds result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result.getHearingDays(), everyItem(hasProperty("courtRoomId", is(nullValue()))));
        assertThat(result.getCourtCentre().getRoomId(), is(nullValue()));
        assertThat(result.getCourtCentre().getRoomName(), is(nullValue()));
    }

    @Test
    void listingNeeds_courtCentreNull_stripsRoomFromDaysOnly() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(List.of(day(DAY_1, ROOM_ID, true)))
                .build();

        final HearingListingNeeds result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result.getCourtCentre(), is(nullValue()));
        assertThat(result.getHearingDays(), hasItem(hasProperty("courtRoomId", is(nullValue()))));
    }

    @Test
    void listingNeeds_draftFlagPreserved_onlyRoomStripped() {
        // Stripping room info must not erase the isDraft flag itself — downstream code (e.g. the
        // aggregate's canAllocateForCrown gate) still needs to see draft=true to deny allocation.
        final HearingListingNeeds hearing = listingNeedsWithCourtCentreRoom()
                .withHearingDays(List.of(day(DAY_1, ROOM_ID, true)))
                .build();

        final HearingListingNeeds result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result.getHearingDays().get(0).getIsDraft(), is(Boolean.TRUE));
        assertThat(result.getHearingDays().get(0).getCourtRoomId(), is(nullValue()));
    }

    // ─── UpdateHearingForListing (update-hearing-for-listing path) ───────

    @Test
    void update_noHearingDays_returnsHearingUnchanged() {
        final UpdateHearingForListing hearing = updateHearing()
                .withHearingDays(List.of())
                .build();

        final UpdateHearingForListing result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result, is(sameInstance(hearing)));
    }

    @Test
    void update_noDraftDay_returnsHearingUnchanged() {
        final UpdateHearingForListing hearing = updateHearing()
                .withHearingDays(List.of(day(DAY_1, ROOM_ID, false)))
                .build();

        final UpdateHearingForListing result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result, is(sameInstance(hearing)));
        assertThat(result.getHearingDays().get(0).getCourtRoomId(), is(notNullValue()));
    }

    @Test
    void update_anyDayDraft_stripsAllRooms() {
        final UpdateHearingForListing hearing = updateHearing()
                .withHearingDays(List.of(
                        day(DAY_1, ROOM_ID, false),
                        day(DAY_2, OTHER_ROOM_ID, true)))
                .build();

        final UpdateHearingForListing result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result.getHearingDays(), everyItem(hasProperty("courtRoomId", is(nullValue()))));
    }

    @Test
    void update_draftFlagPreserved_onlyRoomStripped() {
        final UpdateHearingForListing hearing = updateHearing()
                .withHearingDays(List.of(day(DAY_1, ROOM_ID, true)))
                .build();

        final UpdateHearingForListing result = stripRoomInfoIfAnyDraft(hearing);

        assertThat(result.getHearingDays().get(0).getIsDraft(), is(Boolean.TRUE));
        assertThat(result.getHearingDays().get(0).getCourtRoomId(), is(nullValue()));
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private static HearingDay day(final LocalDate date, final UUID roomId, final boolean isDraft) {
        return HearingDay.hearingDay()
                .withHearingDate(date)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(roomId)
                .withCourtScheduleId(SCHEDULE_ID)
                .withIsDraft(isDraft)
                .build();
    }

    private static HearingListingNeeds.Builder listingNeedsWithCourtCentreRoom() {
        return HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withName("Liverpool Crown")
                        .withRoomId(ROOM_ID)
                        .withRoomName("Court 7")
                        .build());
    }

    private static UpdateHearingForListing.Builder updateHearing() {
        return UpdateHearingForListing.updateHearingForListing()
                .withHearingId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN);
    }
}
