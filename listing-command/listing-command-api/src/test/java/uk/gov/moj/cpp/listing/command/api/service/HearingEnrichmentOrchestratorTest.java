package uk.gov.moj.cpp.listing.command.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
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
        HearingListingNeeds crownWithDurations = mock(HearingListingNeeds.class);

        // Mock the enrichment chain for magistrates (3 steps: days -> duration -> courtSchedule)
        when(hearingDaysEnrichmentService.enrichHearings(magistratesHearing, envelope))
                .thenReturn(magsWithHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurations(magsWithHearingDays, envelope))
                .thenReturn(magsWithDurations);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(magsWithDurations, envelope))
                .thenReturn(enrichedMagistratesHearing);

        // Mock the enrichment chain for crown (3 steps: days -> duration -> courtSchedule)
        when(hearingDaysEnrichmentService.enrichHearings(crownHearing, envelope))
                .thenReturn(crownWithHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurations(crownWithHearingDays, envelope))
                .thenReturn(crownWithDurations);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(crownWithDurations, envelope))
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

        HearingListingNeeds withHearingDays = mock(HearingListingNeeds.class);
        HearingListingNeeds withDurations = mock(HearingListingNeeds.class);

        // Crown now has 3 enrichment steps: days -> duration -> courtSchedule
        when(hearingDaysEnrichmentService.enrichHearings(crownHearing, envelope))
                .thenReturn(withHearingDays);
        when(hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope))
                .thenReturn(withDurations);
        when(courtScheduleEnrichmentService.enrichWithCourtSchedules(withDurations, envelope))
                .thenReturn(enrichedCrownHearing);

        // When
        List<HearingListingNeeds> result = orchestrator.enrichListCourtHearing(hearings, envelope);

        // Then
        verify(hearingDaysEnrichmentService).enrichHearings(crownHearing, envelope);
        verify(hearingDurationEnrichmentService).enrichWithDurations(withHearingDays, envelope);
        verify(courtScheduleEnrichmentService).enrichWithCourtSchedules(withDurations, envelope);

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
}