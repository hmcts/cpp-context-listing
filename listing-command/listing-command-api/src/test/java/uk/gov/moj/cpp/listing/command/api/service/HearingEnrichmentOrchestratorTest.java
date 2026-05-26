package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackSource;

import java.time.ZonedDateTime;
import java.util.UUID;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingEnrichmentOrchestratorTest {

    @Mock
    private HearingDurationEnrichmentService hearingDurationEnrichmentService;

    @Mock
    private CourtScheduleEnrichmentService courtScheduleEnrichmentService;

    @Mock
    private HearingDaysEnrichmentService hearingDaysEnrichmentService;

    @Mock
    private JsonEnvelope envelope;

    @InjectMocks
    private HearingEnrichmentOrchestrator orchestrator;

    private HearingListingNeeds magistratesHearing;
    private HearingListingNeeds crownHearing;
    private HearingListingNeeds enrichedMagistratesHearing;
    private HearingListingNeeds enrichedCrownHearing;

    @BeforeEach
    public void setUp() {
        magistratesHearing = mock(HearingListingNeeds.class);
        crownHearing = mock(HearingListingNeeds.class);
        enrichedMagistratesHearing = mock(HearingListingNeeds.class);
        enrichedCrownHearing = mock(HearingListingNeeds.class);
        lenient().when(magistratesHearing.getJurisdictionType()).thenReturn(JurisdictionType.MAGISTRATES);
        lenient().when(crownHearing.getJurisdictionType()).thenReturn(JurisdictionType.CROWN);
        // CROWN with courtScheduleId on bookedSlot -> triggers the CourtSchedule-first flow in enrichListCourtHearing.
        // Tests that need the allocation-candidate flow should override getBookedSlots() to return null/empty.
        lenient().when(crownHearing.getBookedSlots()).thenReturn(Collections.singletonList(
                RotaSlot.rotaSlot().withCourtScheduleId(java.util.UUID.randomUUID().toString()).build()));
    }

    @Test
    public void shouldEnrichListCourtHearings() {
        // Given
        List<HearingListingNeeds> hearings = Arrays.asList(magistratesHearing, crownHearing);

        // Mock objects for magistrates hearing chain
        HearingListingNeeds magsWithHearingDays = mock(HearingListingNeeds.class);
        HearingListingNeeds magsWithDurations = mock(HearingListingNeeds.class);

        // Mock objects for crown hearing chain
        HearingListingNeeds crownWithHearingDays = mock(HearingListingNeeds.class);

        // Mock the enrichment chain for magistrates (3 steps: days -> duration -> courtSchedule)
        when(hearingDaysEnrichmentService.enrichHearings(magistratesHearing, envelope))
                .thenReturn(magsWithHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurations(magsWithHearingDays, envelope))
                .thenReturn(magsWithDurations);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(magsWithDurations, envelope))
                .thenReturn(enrichedMagistratesHearing);

        // Mock the enrichment chain for crown (3 steps: crownCourtScheduleFirst -> days -> duration)
        // Orchestrator now calls the 2-arg overload with CrownFallbackSource.LIST_COURT_HEARING by default.
        HearingListingNeeds crownWithCourtSchedules = mock(HearingListingNeeds.class);
        when(courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(crownHearing, CrownFallbackSource.LIST_COURT_HEARING))
                .thenReturn(crownWithCourtSchedules);
        when(hearingDaysEnrichmentService.enrichHearings(crownWithCourtSchedules, envelope))
                .thenReturn(crownWithHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurations(crownWithHearingDays, envelope))
                .thenReturn(enrichedCrownHearing);

        // When
        List<HearingListingNeeds> result = orchestrator.enrichListCourtHearing(hearings, envelope);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(enrichedMagistratesHearing));
        assertTrue(result.contains(enrichedCrownHearing));
    }

    @Test
    public void shouldEnrichListMagsHearing() {
        // Given
        List<HearingListingNeeds> hearings = Arrays.asList(magistratesHearing);

        HearingListingNeeds withDurations = mock(HearingListingNeeds.class);
        HearingListingNeeds withHearingDays = mock(HearingListingNeeds.class);

        when(hearingDaysEnrichmentService.enrichHearings(magistratesHearing, envelope))
                .thenReturn(withHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope))
                .thenReturn(withDurations);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(withDurations, envelope))
                .thenReturn(enrichedMagistratesHearing);

        // When
        List<HearingListingNeeds> result = orchestrator.enrichListCourtHearing(hearings, envelope);

        // Then
        verify(hearingDaysEnrichmentService).enrichHearings(magistratesHearing, envelope);
        verify(hearingDurationEnrichmentService).enrichWithDurations(withHearingDays, envelope);
        verify(courtScheduleEnrichmentService).enrichWithCourtSchedules(withDurations, envelope);

        assertEquals(1, result.size());
        assertEquals(enrichedMagistratesHearing, result.get(0));
    }

    @Test
    public void shouldEnrichListCrownHearing() {
        // Given
        List<HearingListingNeeds> hearings = Arrays.asList(crownHearing);

        HearingListingNeeds withCourtSchedules = mock(HearingListingNeeds.class);
        HearingListingNeeds withHearingDays = mock(HearingListingNeeds.class);

        // CROWN order: crownCourtScheduleFirst -> days -> duration
        // Orchestrator calls 2-arg overload; default source is CrownFallbackSource.LIST_COURT_HEARING.
        when(courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(crownHearing, CrownFallbackSource.LIST_COURT_HEARING))
                .thenReturn(withCourtSchedules);
        when(hearingDaysEnrichmentService.enrichHearings(withCourtSchedules, envelope))
                .thenReturn(withHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope))
                .thenReturn(enrichedCrownHearing);

        // When
        List<HearingListingNeeds> result = orchestrator.enrichListCourtHearing(hearings, envelope);

        // Then
        verify(courtScheduleEnrichmentService).enrichCrownCourtScheduleFirst(crownHearing, CrownFallbackSource.LIST_COURT_HEARING);
        verify(hearingDaysEnrichmentService).enrichHearings(withCourtSchedules, envelope);
        verify(hearingDurationEnrichmentService).enrichWithDurations(withHearingDays, envelope);

        assertEquals(1, result.size());
        assertEquals(enrichedCrownHearing, result.get(0));
    }

    @Test
    public void shouldEnrichUpdateHearingForListingForCrown() {
        // Given
        UpdateHearingForListing crownUpdateHearing = mock(UpdateHearingForListing.class);
        lenient().when(crownUpdateHearing.getJurisdictionType()).thenReturn(JurisdictionType.CROWN);

        UpdateHearingForListing withHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing withDuration = mock(UpdateHearingForListing.class);
        UpdateHearingForListing enrichedUpdate = mock(UpdateHearingForListing.class);

        // Crown update has 3 enrichment steps: days -> duration -> courtSchedule
        when(hearingDaysEnrichmentService.enrichHearing(crownUpdateHearing, envelope))
                .thenReturn(withHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope))
                .thenReturn(withDuration);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope))
                .thenReturn(enrichedUpdate);

        // When
        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(crownUpdateHearing, envelope);

        // Then
        verify(hearingDaysEnrichmentService).enrichHearing(crownUpdateHearing, envelope);
        verify(hearingDurationEnrichmentService).enrichWithDurationForUpdate(withHearingDays, envelope);
        verify(courtScheduleEnrichmentService).enrichWithCourtSchedules(withDuration, envelope);
        assertEquals(enrichedUpdate, result);
    }

    @Test
    public void shouldEnrichUpdateHearingForListingWithCourtCentreDetailsForCrown() {
        // Given
        UpdateHearingForListing crownUpdateHearing = mock(UpdateHearingForListing.class);
        lenient().when(crownUpdateHearing.getJurisdictionType()).thenReturn(JurisdictionType.CROWN);
        CourtCentreDetails courtCentreDetails = mock(CourtCentreDetails.class);

        UpdateHearingForListing withHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing withDuration = mock(UpdateHearingForListing.class);
        UpdateHearingForListing enrichedUpdate = mock(UpdateHearingForListing.class);

        // Crown update with courtCentreDetails has 3 enrichment steps: days -> duration -> courtSchedule
        when(hearingDaysEnrichmentService.enrichHearing(crownUpdateHearing, envelope, courtCentreDetails))
                .thenReturn(withHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope))
                .thenReturn(withDuration);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope))
                .thenReturn(enrichedUpdate);

        // When
        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(crownUpdateHearing, envelope, courtCentreDetails);

        // Then
        verify(hearingDaysEnrichmentService).enrichHearing(crownUpdateHearing, envelope, courtCentreDetails);
        verify(hearingDurationEnrichmentService).enrichWithDurationForUpdate(withHearingDays, envelope);
        verify(courtScheduleEnrichmentService).enrichWithCourtSchedules(withDuration, envelope);
        assertEquals(enrichedUpdate, result);
    }

    @Test
    public void shouldRouteCrownUpdateThroughMultiDayExtension_whenMultiDayDurationOnNonDefaultDays() {
        UpdateHearingForListing crownUpdate = mock(UpdateHearingForListing.class);
        lenient().when(crownUpdate.getJurisdictionType()).thenReturn(JurisdictionType.CROWN);
        lenient().when(crownUpdate.getHearingDays()).thenReturn(Collections.emptyList());
        NonDefaultDay withId = NonDefaultDay.nonDefaultDay()
                .withStartTime(ZonedDateTime.parse("2026-05-27T09:00:00Z"))
                .withDuration(1080)
                .withCourtScheduleId(UUID.randomUUID().toString())
                .build();
        lenient().when(crownUpdate.getNonDefaultDays()).thenReturn(Collections.singletonList(withId));

        UpdateHearingForListing afterExtension = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterDuration = mock(UpdateHearingForListing.class);

        when(courtScheduleEnrichmentService.handleCrownMultiDayExtension(crownUpdate)).thenReturn(afterExtension);
        when(hearingDaysEnrichmentService.enrichHearing(afterExtension, envelope)).thenReturn(afterHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(afterHearingDays, envelope)).thenReturn(afterDuration);

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(crownUpdate, envelope);

        verify(courtScheduleEnrichmentService).handleCrownMultiDayExtension(crownUpdate);
        verify(courtScheduleEnrichmentService, never()).enrichCrownCourtScheduleFirst(any(UpdateHearingForListing.class));
        verify(hearingDaysEnrichmentService).enrichHearing(afterExtension, envelope);
        verify(hearingDurationEnrichmentService).enrichWithDurationForUpdate(afterHearingDays, envelope);
        assertEquals(afterDuration, result);
    }

    @Test
    public void shouldRouteCrownUpdateThroughCourtScheduleFirst_whenSingleDayDurationOnHearingDays() {
        UpdateHearingForListing crownUpdate = mock(UpdateHearingForListing.class);
        lenient().when(crownUpdate.getJurisdictionType()).thenReturn(JurisdictionType.CROWN);
        HearingDay dayWithId = HearingDay.hearingDay()
                .withHearingDate(java.time.LocalDate.parse("2026-05-27"))
                .withCourtScheduleId(UUID.randomUUID())
                .withDurationMinutes(360)
                .build();
        lenient().when(crownUpdate.getHearingDays()).thenReturn(Collections.singletonList(dayWithId));
        lenient().when(crownUpdate.getNonDefaultDays()).thenReturn(Collections.emptyList());

        UpdateHearingForListing afterCourtSchedule = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterDuration = mock(UpdateHearingForListing.class);

        when(courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(crownUpdate)).thenReturn(afterCourtSchedule);
        when(hearingDaysEnrichmentService.enrichHearing(afterCourtSchedule, envelope)).thenReturn(afterHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(afterHearingDays, envelope)).thenReturn(afterDuration);

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(crownUpdate, envelope);

        verify(courtScheduleEnrichmentService).enrichCrownCourtScheduleFirst(crownUpdate);
        verify(courtScheduleEnrichmentService, never()).handleCrownMultiDayExtension(any(UpdateHearingForListing.class));
        assertEquals(afterDuration, result);
    }

    @Test
    public void shouldRouteCrownUpdateWithCourtCentreDetailsThroughMultiDayExtension_whenMultiDayDurationOnHearingDays() {
        UpdateHearingForListing crownUpdate = mock(UpdateHearingForListing.class);
        lenient().when(crownUpdate.getJurisdictionType()).thenReturn(JurisdictionType.CROWN);
        HearingDay d1 = HearingDay.hearingDay()
                .withHearingDate(java.time.LocalDate.parse("2026-05-27"))
                .withCourtScheduleId(UUID.randomUUID())
                .withDurationMinutes(360)
                .build();
        HearingDay d2 = HearingDay.hearingDay()
                .withHearingDate(java.time.LocalDate.parse("2026-05-28"))
                .withDurationMinutes(360)
                .build();
        lenient().when(crownUpdate.getHearingDays()).thenReturn(java.util.Arrays.asList(d1, d2));
        lenient().when(crownUpdate.getNonDefaultDays()).thenReturn(Collections.emptyList());
        CourtCentreDetails courtCentreDetails = mock(CourtCentreDetails.class);

        UpdateHearingForListing afterExtension = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterDuration = mock(UpdateHearingForListing.class);

        when(courtScheduleEnrichmentService.handleCrownMultiDayExtension(crownUpdate)).thenReturn(afterExtension);
        when(hearingDaysEnrichmentService.enrichHearing(afterExtension, envelope, courtCentreDetails)).thenReturn(afterHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(afterHearingDays, envelope)).thenReturn(afterDuration);

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(crownUpdate, envelope, courtCentreDetails);

        verify(courtScheduleEnrichmentService).handleCrownMultiDayExtension(crownUpdate);
        verify(courtScheduleEnrichmentService, never()).enrichCrownCourtScheduleFirst(any(UpdateHearingForListing.class));
        assertEquals(afterDuration, result);
    }

    @Test
    public void shouldRouteCrownUpdateWithCourtCentreDetailsThroughCourtScheduleFirst_whenCourtScheduleIdOnHearingDays() {
        UpdateHearingForListing crownUpdate = mock(UpdateHearingForListing.class);
        lenient().when(crownUpdate.getJurisdictionType()).thenReturn(JurisdictionType.CROWN);
        HearingDay dayWithId = HearingDay.hearingDay()
                .withHearingDate(LocalDate.parse("2026-05-27"))
                .withCourtScheduleId(UUID.randomUUID())
                .withDurationMinutes(360)
                .build();
        lenient().when(crownUpdate.getHearingDays()).thenReturn(Collections.singletonList(dayWithId));
        lenient().when(crownUpdate.getNonDefaultDays()).thenReturn(Collections.emptyList());
        CourtCentreDetails courtCentreDetails = mock(CourtCentreDetails.class);

        UpdateHearingForListing afterCourtSchedule = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing afterDuration = mock(UpdateHearingForListing.class);

        when(courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(crownUpdate)).thenReturn(afterCourtSchedule);
        when(hearingDaysEnrichmentService.enrichHearing(afterCourtSchedule, envelope, courtCentreDetails)).thenReturn(afterHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(afterHearingDays, envelope)).thenReturn(afterDuration);

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(crownUpdate, envelope, courtCentreDetails);

        verify(courtScheduleEnrichmentService).enrichCrownCourtScheduleFirst(crownUpdate);
        verify(hearingDaysEnrichmentService).enrichHearing(afterCourtSchedule, envelope, courtCentreDetails);
        verify(hearingDurationEnrichmentService).enrichWithDurationForUpdate(afterHearingDays, envelope);
        assertEquals(afterDuration, result);
    }

    // ─── MAGS update enrichment tests ────────────────────────────────────

    @Test
    public void shouldEnrichUpdateHearingForListingForMagistrates() {
        // Given
        UpdateHearingForListing magsUpdateHearing = mock(UpdateHearingForListing.class);
        lenient().when(magsUpdateHearing.getJurisdictionType()).thenReturn(JurisdictionType.MAGISTRATES);

        UpdateHearingForListing withHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing withDuration = mock(UpdateHearingForListing.class);
        UpdateHearingForListing enrichedUpdate = mock(UpdateHearingForListing.class);

        when(hearingDaysEnrichmentService.enrichHearing(magsUpdateHearing, envelope))
                .thenReturn(withHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope))
                .thenReturn(withDuration);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope))
                .thenReturn(enrichedUpdate);

        // When
        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(magsUpdateHearing, envelope);

        // Then
        verify(hearingDaysEnrichmentService).enrichHearing(magsUpdateHearing, envelope);
        verify(hearingDurationEnrichmentService).enrichWithDurationForUpdate(withHearingDays, envelope);
        verify(courtScheduleEnrichmentService).enrichWithCourtSchedules(withDuration, envelope);
        assertEquals(enrichedUpdate, result);
    }

    @Test
    public void shouldEnrichUpdateHearingForListingWithCourtCentreDetailsForMagistrates() {
        // Given
        UpdateHearingForListing magsUpdateHearing = mock(UpdateHearingForListing.class);
        lenient().when(magsUpdateHearing.getJurisdictionType()).thenReturn(JurisdictionType.MAGISTRATES);
        CourtCentreDetails courtCentreDetails = mock(CourtCentreDetails.class);

        UpdateHearingForListing withHearingDays = mock(UpdateHearingForListing.class);
        UpdateHearingForListing withDuration = mock(UpdateHearingForListing.class);
        UpdateHearingForListing enrichedUpdate = mock(UpdateHearingForListing.class);

        when(hearingDaysEnrichmentService.enrichHearing(magsUpdateHearing, envelope, courtCentreDetails))
                .thenReturn(withHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope))
                .thenReturn(withDuration);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope))
                .thenReturn(enrichedUpdate);

        // When
        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(magsUpdateHearing, envelope, courtCentreDetails);

        // Then
        verify(hearingDaysEnrichmentService).enrichHearing(magsUpdateHearing, envelope, courtCentreDetails);
        verify(hearingDurationEnrichmentService).enrichWithDurationForUpdate(withHearingDays, envelope);
        verify(courtScheduleEnrichmentService).enrichWithCourtSchedules(withDuration, envelope);
        assertEquals(enrichedUpdate, result);
    }

    // ─── Unsupported jurisdiction type tests ─────────────────────────────

    @Test
    public void shouldThrowExceptionForUnsupportedJurisdictionInEnrichListCourtHearing() {
        HearingListingNeeds unsupportedHearing = mock(HearingListingNeeds.class);
        when(unsupportedHearing.getJurisdictionType()).thenReturn(null);

        List<HearingListingNeeds> hearings = Arrays.asList(unsupportedHearing);

        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.enrichListCourtHearing(hearings, envelope));
    }

    @Test
    public void shouldThrowExceptionForUnsupportedJurisdictionInEnrichUpdateHearingForListing() {
        UpdateHearingForListing unsupportedHearing = mock(UpdateHearingForListing.class);
        when(unsupportedHearing.getJurisdictionType()).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.enrichUpdateHearingForListing(unsupportedHearing, envelope));
    }

    @Test
    public void shouldThrowExceptionForUnsupportedJurisdictionInEnrichUpdateHearingForListingWithCourtCentre() {
        UpdateHearingForListing unsupportedHearing = mock(UpdateHearingForListing.class);
        when(unsupportedHearing.getJurisdictionType()).thenReturn(null);
        CourtCentreDetails courtCentreDetails = mock(CourtCentreDetails.class);

        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.enrichUpdateHearingForListing(unsupportedHearing, envelope, courtCentreDetails));
    }

    // ─── Static utility method tests ─────────────────────────────────────

    @Test
    public void shouldSequenceValidHearingDays() {
        HearingDay day1 = HearingDay.hearingDay()
                .withHearingDate(LocalDate.of(2026, 3, 2))
                .withDurationMinutes(240)
                .build();
        HearingDay day2 = HearingDay.hearingDay()
                .withHearingDate(LocalDate.of(2026, 3, 3))
                .withDurationMinutes(240)
                .build();

        List<HearingDay> result = HearingEnrichmentOrchestrator.sequenceValidHearingDays(Arrays.asList(day1, day2));

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getSequence(), is(1));
        assertThat(result.get(1).getSequence(), is(2));
    }

    @Test
    public void shouldOrderAndFilterOutNonSittingDays() {
        LocalDate mar2 = LocalDate.of(2026, 3, 2);
        LocalDate mar3 = LocalDate.of(2026, 3, 3);
        LocalDate mar4 = LocalDate.of(2026, 3, 4);

        HearingDay day1 = HearingDay.hearingDay().withHearingDate(mar4).withDurationMinutes(240).build();
        HearingDay day2 = HearingDay.hearingDay().withHearingDate(mar2).withDurationMinutes(240).build();
        HearingDay day3 = HearingDay.hearingDay().withHearingDate(mar3).withDurationMinutes(240).build();

        // mar3 is a non-sitting day
        List<HearingDay> result = HearingEnrichmentOrchestrator.orderAndFilterOutNonSittingDays(
                Arrays.asList(day1, day2, day3), Arrays.asList(mar3));

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getHearingDate(), is(mar2));
        assertThat(result.get(1).getHearingDate(), is(mar4));
        // Sequence should be reset to 0
        assertThat(result.get(0).getSequence(), is(0));
    }

    @Test
    public void shouldOrderAndFilterWithEmptyNonSittingDays() {
        LocalDate mar2 = LocalDate.of(2026, 3, 2);
        HearingDay day1 = HearingDay.hearingDay().withHearingDate(mar2).withDurationMinutes(360).build();

        List<HearingDay> result = HearingEnrichmentOrchestrator.orderAndFilterOutNonSittingDays(
                Arrays.asList(day1), new ArrayList<>());

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getHearingDate(), is(mar2));
    }

    @Test
    public void shouldGetTotalDuration() {
        HearingDay day1 = HearingDay.hearingDay().withHearingDate(LocalDate.now()).withDurationMinutes(240).build();
        HearingDay day2 = HearingDay.hearingDay().withHearingDate(LocalDate.now().plusDays(1)).withDurationMinutes(360).build();

        int total = HearingEnrichmentOrchestrator.getTotalDuration(Arrays.asList(day1, day2));

        assertThat(total, is(600));
    }

    @Test
    public void shouldGetTotalDurationWithNullMinutes() {
        HearingDay day1 = HearingDay.hearingDay().withHearingDate(LocalDate.now()).withDurationMinutes(240).build();
        HearingDay day2 = HearingDay.hearingDay().withHearingDate(LocalDate.now().plusDays(1)).build(); // null duration

        int total = HearingEnrichmentOrchestrator.getTotalDuration(Arrays.asList(day1, day2));

        // null durationMinutes defaults to DEFAULT_MIN (20)
        assertThat(total, is(260));
    }

    // ─── recalculateDurationSequenceAndEndDatesForHearingDays (UpdateHearingForListing) tests ─────

    @Test
    public void shouldReturnUnchangedUpdateHearingWhenHearingDaysEmpty() {
        UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingDays(Collections.emptyList())
                .build();

        UpdateHearingForListing result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing);

        assertThat(result.getHearingDays(), is(Collections.emptyList()));
    }

    @Test
    public void shouldRecalculateEndDateForUpdateHearing() {
        LocalDate mar2 = LocalDate.of(2026, 3, 2);
        LocalDate mar3 = LocalDate.of(2026, 3, 3);

        HearingDay day1 = HearingDay.hearingDay().withHearingDate(mar2).withDurationMinutes(360).build();
        HearingDay day2 = HearingDay.hearingDay().withHearingDate(mar3).withDurationMinutes(360).build();

        UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingDays(Arrays.asList(day1, day2))
                .build();

        UpdateHearingForListing result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing);

        assertThat(result.getEndDate(), is(mar3));
        assertThat(result.getHearingDays().size(), is(2));
    }

    @Test
    public void shouldFilterNonSittingDaysFromUpdateHearing() {
        LocalDate mar2 = LocalDate.of(2026, 3, 2);
        LocalDate mar3 = LocalDate.of(2026, 3, 3);
        LocalDate mar4 = LocalDate.of(2026, 3, 4);

        HearingDay day1 = HearingDay.hearingDay().withHearingDate(mar2).withDurationMinutes(360).build();
        HearingDay day2 = HearingDay.hearingDay().withHearingDate(mar3).withDurationMinutes(360).build();
        HearingDay day3 = HearingDay.hearingDay().withHearingDate(mar4).withDurationMinutes(360).build();

        UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingDays(Arrays.asList(day1, day2, day3))
                .withNonSittingDays(Collections.singletonList(mar3))
                .build();

        UpdateHearingForListing result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing);

        assertThat(result.getHearingDays().size(), is(2));
        assertThat(result.getEndDate(), is(mar4));
    }

    // ─── recalculateDurationSequenceAndEndDatesForHearingDays (List<HearingListingNeeds>) tests ──

    @Test
    public void shouldRecalculateEndDateForHearingListingNeeds() {
        LocalDate mar2 = LocalDate.of(2026, 3, 2);
        LocalDate mar4 = LocalDate.of(2026, 3, 4);

        HearingDay day1 = HearingDay.hearingDay().withHearingDate(mar2).withDurationMinutes(360).build();
        HearingDay day2 = HearingDay.hearingDay().withHearingDate(mar4).withDurationMinutes(360).build();

        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withHearingDays(Arrays.asList(day1, day2))
                .withEstimatedMinutes(720)
                .build();

        List<HearingListingNeeds> result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(
                Arrays.asList(hearing));

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getEndDate(), is(mar4.toString()));
        assertThat(result.get(0).getEstimatedMinutes(), is(720));
    }

    @Test
    public void shouldPreserveHearingWithEmptyHearingDays() {
        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withEstimatedMinutes(240)
                .build();

        List<HearingListingNeeds> result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(
                Arrays.asList(hearing));

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getEstimatedMinutes(), is(240));
    }

    @Test
    public void shouldFilterNonSittingDaysForHearingListingNeeds() {
        LocalDate mar2 = LocalDate.of(2026, 3, 2);
        LocalDate mar3 = LocalDate.of(2026, 3, 3);

        HearingDay day1 = HearingDay.hearingDay().withHearingDate(mar2).withDurationMinutes(360).build();
        HearingDay day2 = HearingDay.hearingDay().withHearingDate(mar3).withDurationMinutes(360).build();

        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withHearingDays(Arrays.asList(day1, day2))
                .withNonSittingDays(Collections.singletonList(mar3.toString()))
                .withEstimatedMinutes(720)
                .build();

        List<HearingListingNeeds> result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(
                Arrays.asList(hearing));

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getHearingDays().size(), is(1));
        assertThat(result.get(0).getEndDate(), is(mar2.toString()));
    }
}