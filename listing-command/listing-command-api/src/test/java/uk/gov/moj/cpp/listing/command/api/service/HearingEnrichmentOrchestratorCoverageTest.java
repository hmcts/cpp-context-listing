package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.listing.commands.HearingDay.hearingDay;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;
import static uk.gov.justice.listing.commands.UpdateHearingForListing.updateHearingForListing;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the static helper methods and all jurisdiction branches of
 * HearingEnrichmentOrchestrator using anonymous service stubs injected via
 * reflection — no Mockito inline mocking required.
 */
public class HearingEnrichmentOrchestratorCoverageTest {

    private final HearingDaysEnrichmentService hearingDaysStub = new HearingDaysEnrichmentService() {
        @Override
        public HearingListingNeeds enrichHearings(HearingListingNeeds hearing, JsonEnvelope envelope) {
            return hearing;
        }
        @Override
        public UpdateHearingForListing enrichHearing(UpdateHearingForListing hearing, JsonEnvelope envelope) {
            return hearing;
        }
        @Override
        public UpdateHearingForListing enrichHearing(UpdateHearingForListing hearing, JsonEnvelope envelope, CourtCentreDetails courtCentreDetails) {
            return hearing;
        }
    };

    private final HearingDurationEnrichmentService durationStub = new HearingDurationEnrichmentService() {
        @Override
        public HearingListingNeeds enrichWithDurations(HearingListingNeeds hearing, JsonEnvelope envelope) {
            return hearing;
        }
        @Override
        public UpdateHearingForListing enrichWithDurationForUpdate(UpdateHearingForListing hearing, JsonEnvelope envelope) {
            return hearing;
        }
    };

    private final CourtScheduleEnrichmentService scheduleStub = new CourtScheduleEnrichmentService() {
        @Override
        public HearingListingNeeds enrichWithCourtSchedules(HearingListingNeeds hearing, JsonEnvelope envelope) {
            return hearing;
        }
        @Override
        public UpdateHearingForListing enrichWithCourtSchedules(UpdateHearingForListing hearing, JsonEnvelope envelope) {
            return hearing;
        }
    };

    private HearingEnrichmentOrchestrator orchestrator;

    @BeforeEach
    void setUp() throws Exception {
        orchestrator = new HearingEnrichmentOrchestrator();
        injectField("hearingDaysEnrichmentService", hearingDaysStub);
        injectField("hearingDurationEnrichmentService", durationStub);
        injectField("courtScheduleEnrichmentService", scheduleStub);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = HearingEnrichmentOrchestrator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(orchestrator, value);
    }

    // ── enrichListCourtHearing — all jurisdiction branches ────────────────────

    @Test
    void shouldEnrichListCourtHearingForMagistrates() {
        HearingListingNeeds hearing = hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .build();

        List<HearingListingNeeds> result = orchestrator.enrichListCourtHearing(List.of(hearing), null);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getJurisdictionType(), is(JurisdictionType.MAGISTRATES));
    }

    @Test
    void shouldEnrichListCourtHearingForCrown() {
        HearingListingNeeds hearing = hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        List<HearingListingNeeds> result = orchestrator.enrichListCourtHearing(List.of(hearing), null);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getJurisdictionType(), is(JurisdictionType.CROWN));
    }

    @Test
    void shouldThrowForUnsupportedJurisdictionInEnrichListCourtHearing() {
        HearingListingNeeds hearing = hearingListingNeeds().build(); // jurisdictionType = null

        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.enrichListCourtHearing(List.of(hearing), null));
    }

    // ── enrichUpdateHearingForListing(hearing, envelope) ─────────────────────

    @Test
    void shouldEnrichUpdateHearingForListingForMagistrates() {
        UpdateHearingForListing hearing = updateHearingForListing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .build();

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(hearing, null);

        assertThat(result, notNullValue());
    }

    @Test
    void shouldEnrichUpdateHearingForListingForCrown() {
        UpdateHearingForListing hearing = updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(hearing, null);

        assertThat(result, notNullValue());
    }

    @Test
    void shouldThrowForUnsupportedJurisdictionInUpdateHearing() {
        UpdateHearingForListing hearing = updateHearingForListing().build(); // jurisdictionType = null

        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.enrichUpdateHearingForListing(hearing, null));
    }

    // ── enrichUpdateHearingForListing(hearing, envelope, courtCentreDetails) ──

    @Test
    void shouldEnrichUpdateHearingWithCourtCentreForMagistrates() {
        UpdateHearingForListing hearing = updateHearingForListing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .build();

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(hearing, null, null);

        assertThat(result, notNullValue());
    }

    @Test
    void shouldEnrichUpdateHearingWithCourtCentreForCrown() {
        UpdateHearingForListing hearing = updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        UpdateHearingForListing result = orchestrator.enrichUpdateHearingForListing(hearing, null, null);

        assertThat(result, notNullValue());
    }

    @Test
    void shouldThrowForUnsupportedJurisdictionInUpdateHearingWithCourtCentre() {
        UpdateHearingForListing hearing = updateHearingForListing().build(); // jurisdictionType = null

        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.enrichUpdateHearingForListing(hearing, null, null));
    }

    // ── Static: sequenceValidHearingDays ─────────────────────────────────────

    @Test
    void shouldSequenceHearingDaysStartingFromOne() {
        HearingDay day1 = hearingDay().withHearingDate(LocalDate.of(2020, 8, 18)).withSequence(99).withDurationMinutes(30).build();
        HearingDay day2 = hearingDay().withHearingDate(LocalDate.of(2020, 8, 19)).withSequence(99).withDurationMinutes(10).build();

        List<HearingDay> result = HearingEnrichmentOrchestrator.sequenceValidHearingDays(List.of(day1, day2));

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getSequence(), is(1));
        assertThat(result.get(1).getSequence(), is(2));
    }

    // ── Static: orderAndFilterOutNonSittingDays ───────────────────────────────

    @Test
    void shouldFilterNonSittingDaysAndSortByDate() {
        LocalDate nonSitting = LocalDate.of(2020, 8, 19);
        HearingDay day1 = hearingDay().withHearingDate(LocalDate.of(2020, 8, 18)).withSequence(1).withDurationMinutes(30).build();
        HearingDay day2 = hearingDay().withHearingDate(nonSitting).withSequence(2).withDurationMinutes(10).build();
        HearingDay day3 = hearingDay().withHearingDate(LocalDate.of(2020, 8, 17)).withSequence(3).withDurationMinutes(20).build();

        List<HearingDay> result = HearingEnrichmentOrchestrator.orderAndFilterOutNonSittingDays(
                List.of(day1, day2, day3), List.of(nonSitting));

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getHearingDate(), is(LocalDate.of(2020, 8, 17)));
        assertThat(result.get(1).getHearingDate(), is(LocalDate.of(2020, 8, 18)));
        assertThat(result.get(0).getSequence(), is(0));
    }

    @Test
    void shouldReturnAllDaysOrderedWhenNonSittingListIsEmpty() {
        HearingDay day1 = hearingDay().withHearingDate(LocalDate.of(2020, 8, 18)).withSequence(2).withDurationMinutes(30).build();
        HearingDay day2 = hearingDay().withHearingDate(LocalDate.of(2020, 8, 17)).withSequence(1).withDurationMinutes(20).build();

        List<HearingDay> result = HearingEnrichmentOrchestrator.orderAndFilterOutNonSittingDays(
                List.of(day1, day2), List.of());

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getHearingDate(), is(LocalDate.of(2020, 8, 17)));
        assertThat(result.get(0).getSequence(), is(0));
    }

    // ── Static: getTotalDuration ──────────────────────────────────────────────

    @Test
    void shouldSumAllHearingDayDurations() {
        HearingDay day1 = hearingDay().withDurationMinutes(30).withHearingDate(LocalDate.of(2020, 8, 18)).build();
        HearingDay day2 = hearingDay().withDurationMinutes(20).withHearingDate(LocalDate.of(2020, 8, 19)).build();

        assertThat(HearingEnrichmentOrchestrator.getTotalDuration(List.of(day1, day2)), is(50));
    }

    @Test
    void shouldUseDefaultMinWhenHearingDayDurationIsNull() {
        // DEFAULT_MIN = 20; null duration contributes 20
        HearingDay dayWithNull = hearingDay().withHearingDate(LocalDate.of(2020, 8, 18)).build();
        HearingDay dayWithValue = hearingDay().withDurationMinutes(30).withHearingDate(LocalDate.of(2020, 8, 19)).build();

        assertThat(HearingEnrichmentOrchestrator.getTotalDuration(List.of(dayWithNull, dayWithValue)), is(50));
    }

    // ── Static: recalculateDurationSequenceAndEndDatesForHearingDays(UpdateHearingForListing) ──

    @Test
    void shouldSetWeekCommencingEndDateUsingExplicitDuration() {
        LocalDate startDate = LocalDate.of(2020, 8, 17);
        UpdateHearingForListing hearing = updateHearingForListing()
                .withWeekCommencingStartDate(startDate)
                .withWeekCommencingDurationInWeeks(2)
                .build();

        UpdateHearingForListing result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing);

        assertThat(result.getWeekCommencingDurationInWeeks(), is(2));
        assertThat(result.getWeekCommencingEndDate(), is(startDate.plusWeeks(2).minusDays(1)));
    }

    @Test
    void shouldUseDefaultWeekDurationWhenWeekCommencingDurationIsNull() {
        LocalDate startDate = LocalDate.of(2020, 8, 17);
        UpdateHearingForListing hearing = updateHearingForListing()
                .withWeekCommencingStartDate(startDate)
                .build(); // weekCommencingDurationInWeeks = null → uses DEFAULT = 1

        UpdateHearingForListing result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing);

        assertThat(result.getWeekCommencingDurationInWeeks(), is(1));
        assertThat(result.getWeekCommencingEndDate(), is(startDate.plusWeeks(1).minusDays(1)));
    }

    @Test
    void shouldReturnUpdateHearingAsIsWhenHearingDaysIsEmpty() {
        UpdateHearingForListing hearing = updateHearingForListing()
                .withHearingDays(List.of())
                .build();

        assertThat(HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing), is(hearing));
    }

    @Test
    void shouldSetEndDateToLastHearingDayAfterFilteringNonSittingDays() {
        LocalDate date1 = LocalDate.of(2020, 8, 18);
        LocalDate date2 = LocalDate.of(2020, 8, 19);
        LocalDate date3 = LocalDate.of(2020, 8, 20);
        HearingDay day1 = hearingDay().withHearingDate(date1).withDurationMinutes(30).withSequence(1).build();
        HearingDay day2 = hearingDay().withHearingDate(date2).withDurationMinutes(20).withSequence(2).build();
        HearingDay day3 = hearingDay().withHearingDate(date3).withDurationMinutes(10).withSequence(3).build();

        UpdateHearingForListing hearing = updateHearingForListing()
                .withHearingDays(List.of(day1, day2, day3))
                .withNonSittingDays(List.of(date2))
                .build();

        UpdateHearingForListing result = HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing);

        assertThat(result.getHearingDays(), hasSize(2));
        assertThat(result.getEndDate(), is(date3));
    }

    @Test
    void shouldReturnOriginalUpdateHearingWhenAllHearingDaysAreNonSitting() {
        LocalDate nonSittingDay = LocalDate.of(2020, 8, 18);
        HearingDay day1 = hearingDay().withHearingDate(nonSittingDay).withDurationMinutes(30).withSequence(1).build();

        UpdateHearingForListing hearing = updateHearingForListing()
                .withHearingDays(List.of(day1))
                .withNonSittingDays(List.of(nonSittingDay))
                .build();

        assertThat(HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(hearing), is(hearing));
    }

    // ── Static: recalculateDurationSequenceAndEndDatesForHearingDays(List) ───

    @Test
    void shouldHandleWeekCommencingHearingListingNeedsWithExplicitDuration() {
        WeekCommencingDate weekCommencingDate = WeekCommencingDate.weekCommencingDate()
                .withStartDate("2020-08-17").withDuration(3).build();

        HearingListingNeeds hearing = hearingListingNeeds().withWeekCommencingDate(weekCommencingDate).build();

        List<HearingListingNeeds> result =
                HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(List.of(hearing));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getWeekCommencingDate().getDuration(), is(3));
    }

    @Test
    void shouldUseDefaultDurationForWeekCommencingWhenNull() {
        WeekCommencingDate weekCommencingDate = WeekCommencingDate.weekCommencingDate()
                .withStartDate("2020-08-17").build(); // duration = null → DEFAULT = 1

        HearingListingNeeds hearing = hearingListingNeeds().withWeekCommencingDate(weekCommencingDate).build();

        List<HearingListingNeeds> result =
                HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(List.of(hearing));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getWeekCommencingDate().getDuration(), is(1));
    }

    @Test
    void shouldReturnHearingListingNeedsAsIsWhenNoHearingDays() {
        HearingListingNeeds hearing = hearingListingNeeds().withHearingDays(List.of()).build();

        List<HearingListingNeeds> result =
                HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(List.of(hearing));

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(hearing));
    }

    @Test
    void shouldCalculateEndDateAndEstimatedMinutesAfterFilteringNonSittingDays() {
        LocalDate date1 = LocalDate.of(2020, 8, 18);
        LocalDate date2 = LocalDate.of(2020, 8, 20);
        HearingDay day1 = hearingDay().withHearingDate(date1).withDurationMinutes(30).withSequence(1).build();
        HearingDay day2 = hearingDay().withHearingDate(date2).withDurationMinutes(20).withSequence(2).build();

        HearingListingNeeds hearing = hearingListingNeeds()
                .withHearingDays(List.of(day1, day2))
                .withNonSittingDays(List.of(date1.toString()))
                .build();

        List<HearingListingNeeds> result =
                HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(List.of(hearing));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getHearingDays(), hasSize(1));
        assertThat(result.get(0).getEndDate(), is(date2.toString()));
        assertThat(result.get(0).getEstimatedMinutes(), is(20));
    }

    @Test
    void shouldReturnOriginalHearingListingNeedsWhenAllDaysMatchNonSittingDays() {
        LocalDate date1 = LocalDate.of(2020, 8, 18);
        HearingDay day1 = hearingDay().withHearingDate(date1).withDurationMinutes(30).withSequence(1).build();

        HearingListingNeeds hearing = hearingListingNeeds()
                .withHearingDays(List.of(day1))
                .withNonSittingDays(List.of(date1.toString()))
                .build();

        List<HearingListingNeeds> result =
                HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(List.of(hearing));

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is(hearing));
    }

    @Test
    void shouldCalculateEndDateWhenHearingDaysExistAndNonSittingDaysIsEmpty() {
        LocalDate date1 = LocalDate.of(2020, 8, 18);
        LocalDate date2 = LocalDate.of(2020, 8, 19);
        HearingDay day1 = hearingDay().withHearingDate(date1).withDurationMinutes(30).withSequence(1).build();
        HearingDay day2 = hearingDay().withHearingDate(date2).withDurationMinutes(20).withSequence(2).build();

        // No nonSittingDays → isEmpty branch produces new ArrayList<>()
        HearingListingNeeds hearing = hearingListingNeeds()
                .withHearingDays(List.of(day1, day2))
                .build();

        List<HearingListingNeeds> result =
                HearingEnrichmentOrchestrator.recalculateDurationSequenceAndEndDatesForHearingDays(List.of(hearing));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getEndDate(), is(date2.toString()));
        assertThat(result.get(0).getEstimatedMinutes(), is(50));
    }

    // ── Static: logEnrichedHearings ───────────────────────────────────────────

    @Test
    void shouldLogEnrichedHearingsWithNullAndNonNullFields() {
        HearingListingNeeds hearingWithNullDays = hearingListingNeeds().build();
        HearingListingNeeds hearingWithDays = hearingListingNeeds()
                .withHearingDays(List.of(
                        hearingDay().withHearingDate(LocalDate.of(2020, 8, 18))
                                .withDurationMinutes(30).withSequence(1).build()))
                .withNonSittingDays(List.of("2020-08-19"))
                .build();

        HearingEnrichmentOrchestrator.logEnrichedHearings(List.of(hearingWithNullDays, hearingWithDays));
        // no exception = success
    }
}
