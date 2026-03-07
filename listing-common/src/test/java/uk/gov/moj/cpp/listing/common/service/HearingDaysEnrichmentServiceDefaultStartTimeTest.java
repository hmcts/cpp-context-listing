package uk.gov.moj.cpp.listing.common.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for HearingDaysEnrichmentService to verify defaultStartTime functionality
 * with court centre details, including summer and winter time scenarios.
 */
@ExtendWith(MockitoExtension.class)
class HearingDaysEnrichmentServiceDefaultStartTimeTest {

    @InjectMocks
    private HearingDaysEnrichmentService hearingDaysEnrichmentService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    private UUID courtCentreId;
    private UUID courtRoomId;
    private UUID hearingId;

    @BeforeEach
    void setUp() {
        courtCentreId = randomUUID();
        courtRoomId = randomUUID();
        hearingId = randomUUID();
    }

    @Test
    void shouldUseCourtCentreDefaultStartTimeForSummerDate() {
        // Given - Summer date (BST - British Summer Time)
        LocalDate summerDate = LocalDate.of(2024, 7, 15); // July 15, 2024 (summer)
        LocalTime courtDefaultStartTime = LocalTime.of(10, 0); // 10:00 AM local time

        CourtCentreDetails courtCentreDetails = createCourtCentreDetails(courtDefaultStartTime);
        UpdateHearingForListing hearing = createUpdateHearingForListing(summerDate, summerDate);

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, courtCentreDetails);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(1, enrichedHearing.getHearingDays().size());

        // Verify the start time is correctly converted from BST to UTC
        // 10:00 BST = 09:00 UTC (BST is UTC+1)
        ZonedDateTime expectedStartTime = ZonedDateTime.of(summerDate, LocalTime.of(9, 0), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
    }

    @Test
    void shouldUseCourtCentreDefaultStartTimeForWinterDate() {
        // Given - Winter date (GMT - Greenwich Mean Time)
        LocalDate winterDate = LocalDate.of(2024, 1, 15); // January 15, 2024 (winter)
        LocalTime courtDefaultStartTime = LocalTime.of(10, 0); // 10:00 AM local time

        CourtCentreDetails courtCentreDetails = createCourtCentreDetails(courtDefaultStartTime);
        UpdateHearingForListing hearing = createUpdateHearingForListing(winterDate, winterDate);

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, courtCentreDetails);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(1, enrichedHearing.getHearingDays().size());

        // Verify the start time is correctly converted from GMT to UTC
        // 10:00 GMT = 10:00 UTC (GMT is UTC+0)
        ZonedDateTime expectedStartTime = ZonedDateTime.of(winterDate, LocalTime.of(10, 0), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
    }

    @Test
    void shouldUseCourtCentreDefaultStartTimeForSpringTransitionDate() {
        // Given - Spring transition date (clocks go forward)
        LocalDate springTransitionDate = LocalDate.of(2024, 3, 31); // March 31, 2024 (spring transition)
        LocalTime courtDefaultStartTime = LocalTime.of(10, 0); // 10:00 AM local time

        CourtCentreDetails courtCentreDetails = createCourtCentreDetails(courtDefaultStartTime);
        UpdateHearingForListing hearing = createUpdateHearingForListing(springTransitionDate, springTransitionDate);

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, courtCentreDetails);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(1, enrichedHearing.getHearingDays().size());

        // Verify the start time is correctly converted from BST to UTC
        // 10:00 BST = 09:00 UTC (BST is UTC+1)
        ZonedDateTime expectedStartTime = ZonedDateTime.of(springTransitionDate, LocalTime.of(9, 0), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
    }

    @Test
    void shouldUseCourtCentreDefaultStartTimeForAutumnTransitionDate() {
        // Given - Autumn transition date (clocks go back)
        LocalDate autumnTransitionDate = LocalDate.of(2024, 10, 27); // October 27, 2024 (autumn transition)
        LocalTime courtDefaultStartTime = LocalTime.of(10, 0); // 10:00 AM local time

        CourtCentreDetails courtCentreDetails = createCourtCentreDetails(courtDefaultStartTime);
        UpdateHearingForListing hearing = createUpdateHearingForListing(autumnTransitionDate, autumnTransitionDate);

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, courtCentreDetails);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(1, enrichedHearing.getHearingDays().size());

        // Verify the start time is correctly converted from GMT to UTC
        // 10:00 GMT = 10:00 UTC (GMT is UTC+0)
        ZonedDateTime expectedStartTime = ZonedDateTime.of(autumnTransitionDate, LocalTime.of(10, 0), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
    }

    @Test
    void shouldUseCourtCentreDefaultStartTimeForMultiDayHearing() {
        // Given - Multi-day hearing spanning summer and winter
        LocalDate startDate = LocalDate.of(2024, 1, 15); // Winter
        LocalDate endDate = LocalDate.of(2024, 1, 17); // Winter
        LocalTime courtDefaultStartTime = LocalTime.of(9, 30); // 9:30 AM local time

        CourtCentreDetails courtCentreDetails = createCourtCentreDetails(courtDefaultStartTime);
        UpdateHearingForListing hearing = createUpdateHearingForListing(startDate, endDate);

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, courtCentreDetails);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(3, enrichedHearing.getHearingDays().size()); // 3 days

        // Verify all days use the correct start time (9:30 GMT = 9:30 UTC)
        ZonedDateTime expectedStartTime = ZonedDateTime.of(startDate, LocalTime.of(9, 30), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());

        expectedStartTime = ZonedDateTime.of(startDate.plusDays(1), LocalTime.of(9, 30), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(1).getStartTime());

        expectedStartTime = ZonedDateTime.of(startDate.plusDays(2), LocalTime.of(9, 30), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(2).getStartTime());
    }

    @Test
    void shouldFallbackToDefaultTimeWhenCourtCentreDetailsIsNull() {
        // Given - No court centre details provided
        LocalDate testDate = LocalDate.of(2024, 7, 15); // Summer date
        UpdateHearingForListing hearing = createUpdateHearingForListing(testDate, testDate);

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, null);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(1, enrichedHearing.getHearingDays().size());

        // Should fallback to default 9:00 AM UTC
        ZonedDateTime expectedStartTime = ZonedDateTime.of(testDate, LocalTime.of(9, 0), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
    }

    @Test
    void shouldFallbackToDefaultTimeWhenCourtCentreDefaultStartTimeIsNull() {
        // Given - Court centre details with null default start time
        LocalDate testDate = LocalDate.of(2024, 7, 15); // Summer date
        CourtCentreDetails courtCentreDetails = createCourtCentreDetails(null);
        UpdateHearingForListing hearing = createUpdateHearingForListing(testDate, testDate);

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, courtCentreDetails);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(1, enrichedHearing.getHearingDays().size());

        // Should fallback to default 9:00 AM UTC
        ZonedDateTime expectedStartTime = ZonedDateTime.of(testDate, LocalTime.of(9, 0), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
    }

    @Test
    void shouldUseCourtCentreDefaultStartTimeForDifferentTimes() {
        // Given - Different start times to test various scenarios
        LocalDate testDate = LocalDate.of(2024, 7, 15); // Summer date
        LocalTime[] testTimes = {
                LocalTime.of(8, 0),   // 8:00 AM
                LocalTime.of(9, 30), // 9:30 AM
                LocalTime.of(14, 0), // 2:00 PM
                LocalTime.of(16, 45) // 4:45 PM
        };

        for (LocalTime testTime : testTimes) {
            // Given
            CourtCentreDetails courtCentreDetails = createCourtCentreDetails(testTime);
            UpdateHearingForListing hearing = createUpdateHearingForListing(testDate, testDate);

            // When
            UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                    hearing, jsonEnvelope, courtCentreDetails);

            // Then
            assertNotNull(enrichedHearing);
            assertNotNull(enrichedHearing.getHearingDays());
            assertEquals(1, enrichedHearing.getHearingDays().size());

            // Verify the start time is correctly converted from BST to UTC
            // For summer: BST = UTC+1, so subtract 1 hour
            LocalTime expectedUtcTime = testTime.minusHours(1);
            ZonedDateTime expectedStartTime = ZonedDateTime.of(testDate, expectedUtcTime, ZoneOffset.UTC);
            assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
        }
    }

    @Test
    void shouldUseCourtCentreDefaultStartTimeForCrownJurisdiction() {
        // Given - Crown jurisdiction with court centre default start time
        LocalDate testDate = LocalDate.of(2024, 7, 15); // Summer date
        LocalTime courtDefaultStartTime = LocalTime.of(10, 30); // 10:30 AM local time

        CourtCentreDetails courtCentreDetails = createCourtCentreDetails(courtDefaultStartTime);
        UpdateHearingForListing hearing = createUpdateHearingForListing(testDate, testDate);
        hearing = UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(hearing)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(
                hearing, jsonEnvelope, courtCentreDetails);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertEquals(1, enrichedHearing.getHearingDays().size());

        // Verify the start time is correctly converted from BST to UTC
        // 10:30 BST = 09:30 UTC (BST is UTC+1)
        ZonedDateTime expectedStartTime = ZonedDateTime.of(testDate, LocalTime.of(9, 30), ZoneOffset.UTC);
        assertEquals(expectedStartTime, enrichedHearing.getHearingDays().get(0).getStartTime());
    }

    private CourtCentreDetails createCourtCentreDetails(LocalTime defaultStartTime) {
        return CourtCentreDetails.courtCentreDetails()
                .withId(courtCentreId)
                .withDefaultStartTime(defaultStartTime)
                .withDefaultDuration(480) // 8 hours
                .build();
    }

    private UpdateHearingForListing createUpdateHearingForListing(LocalDate startDate, LocalDate endDate) {
        return UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withNonSittingDays(List.of())
                .withNonDefaultDays(List.of())
                .build();
    }
}