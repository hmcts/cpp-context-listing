package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CrownNonDefaultDaysValidatorTest {

    private static final LocalDate START_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate END_DATE = START_DATE.plusDays(3);

    // Anchored Monday so weekend-exemption in coverage checks is deterministic.
    private static final LocalDate MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Test
    void shouldRejectMoreThanOneBlockDescriptorVirtualDay() {
        final UpdateHearingForListing hearing = crownUpdate(Arrays.asList(
                virtualDay(START_DATE, 1440),
                virtualDay(START_DATE.plusDays(1), 720)));

        final BadRequestException e = assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
        assertThat(e.getMessage(), containsString("at most one virtual nonDefaultDay may carry the block total"));
    }

    @Test
    void shouldAcceptIdLessPerDayVirtualProxies() {
        // Legacy court-room-change shape (HearingDayCourtRoomChangeForCrownIT): N virtual days
        // each ≤ one court day WITHOUT courtScheduleIds — partial coverage stays accepted.
        final UpdateHearingForListing hearing = crownUpdate(Arrays.asList(
                virtualDayNoId(START_DATE.plusDays(2), 360),
                virtualDayNoId(START_DATE.plusDays(3), 360)));

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldAcceptCompletePerDaySessionSelection() {
        final UpdateHearingForListing hearing = crownUpdateWindow(MONDAY, MONDAY.plusDays(2), Arrays.asList(
                virtualDay(MONDAY, 360),
                virtualDay(MONDAY.plusDays(1), 360),
                virtualDay(MONDAY.plusDays(2), 360)));

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldRejectPerDaySessionSelectionWithGap() {
        // The real bug shape: window Mon..Wed but proxies only for Tue+Wed (the changed days).
        final UpdateHearingForListing hearing = crownUpdateWindow(MONDAY, MONDAY.plusDays(2), Arrays.asList(
                virtualDay(MONDAY.plusDays(1), 360),
                virtualDay(MONDAY.plusDays(2), 360)));

        final BadRequestException e = assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
        assertThat(e.getMessage(), containsString("INCOMPLETE_SESSION_SELECTION"));
        assertThat(e.getMessage(), containsString(MONDAY.toString()));
    }

    @Test
    void shouldRejectMixedIdPresenceOnPerDayProxies() {
        final UpdateHearingForListing hearing = crownUpdateWindow(MONDAY, MONDAY.plusDays(1), Arrays.asList(
                virtualDay(MONDAY, 360),
                virtualDayNoId(MONDAY.plusDays(1), 360)));

        final BadRequestException e = assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
        assertThat(e.getMessage(), containsString("INCOMPLETE_SESSION_SELECTION"));
    }

    @Test
    void shouldRejectDuplicatePerDayProxyDates() {
        final UpdateHearingForListing hearing = crownUpdateWindow(MONDAY, MONDAY.plusDays(1), Arrays.asList(
                virtualDay(MONDAY, 360),
                virtualDay(MONDAY, 360),
                virtualDay(MONDAY.plusDays(1), 360)));

        final BadRequestException e = assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
        assertThat(e.getMessage(), containsString("INCOMPLETE_SESSION_SELECTION"));
    }

    @Test
    void shouldRejectPerDayProxyOutsideWindow() {
        final UpdateHearingForListing hearing = crownUpdateWindow(MONDAY, MONDAY.plusDays(1), Arrays.asList(
                virtualDay(MONDAY, 360),
                virtualDay(MONDAY.plusDays(1), 360),
                virtualDay(MONDAY.plusDays(3), 360)));

        final BadRequestException e = assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
        assertThat(e.getMessage(), containsString("INCOMPLETE_SESSION_SELECTION"));
    }

    @Test
    void shouldExemptWeekendsFromCoverage() {
        // Fri..Mon window: Sat+Sun are not sitting days, so Fri+Mon proxies are complete.
        final LocalDate friday = MONDAY.plusDays(4);
        final UpdateHearingForListing hearing = crownUpdateWindow(friday, friday.plusDays(3), Arrays.asList(
                virtualDay(friday, 360),
                virtualDay(friday.plusDays(3), 360)));

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldExemptNonSittingDaysFromCoverage() {
        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(MONDAY)
                .withEndDate(MONDAY.plusDays(2))
                .withNonSittingDays(Collections.singletonList(MONDAY.plusDays(1)))
                .withNonDefaultDays(Arrays.asList(
                        virtualDay(MONDAY, 360),
                        virtualDay(MONDAY.plusDays(2), 360)))
                .build();

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldNotRunCoverageForBlockDescriptorWithGaps() {
        // Single virtual day > 360 = block descriptor with anchor courtScheduleId: window gaps
        // are expected (courtscheduler finds the sessions) — coverage must NOT fire.
        final UpdateHearingForListing hearing = crownUpdateWindow(MONDAY, MONDAY.plusDays(2),
                Collections.singletonList(virtualDay(MONDAY, 1080)));

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldAcceptGenuineDayOutsideWindowWhenNoBlockDescriptor() {
        // Legacy shape (HearingIT.updateHearingResultsWhenCourtRoomNotSelected): a stale genuine
        // day outside the window with no block descriptor relies on the silent enrichment filter.
        final UpdateHearingForListing hearing = crownUpdate(Collections.singletonList(
                genuineDay(START_DATE.minusYears(6), 15)));

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldRejectWhenStartDateDiffersFromVirtualDayDate() {
        final UpdateHearingForListing hearing = crownUpdate(Collections.singletonList(
                virtualDay(START_DATE.plusDays(1), 1440)));

        final BadRequestException e = assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
        assertThat(e.getMessage(), containsString("must equal the virtual nonDefaultDay's date"));
    }

    @Test
    void shouldRejectGenuineNonDefaultDayOutsideWindow() {
        final UpdateHearingForListing hearing = crownUpdate(Arrays.asList(
                virtualDay(START_DATE, 1440),
                genuineDay(END_DATE.plusDays(1), 360)));

        final BadRequestException e = assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
        assertThat(e.getMessage(), containsString("must fall within startDate"));
    }

    @Test
    void shouldRejectGenuineNonDefaultDayBeforeWindowWhenBlockDescriptorPresent() {
        final UpdateHearingForListing hearing = crownUpdate(Arrays.asList(
                virtualDay(START_DATE, 1440),
                genuineDay(START_DATE.minusDays(1), 360)));

        assertThrows(BadRequestException.class,
                () -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldAcceptValidMixedPayload() {
        final UpdateHearingForListing hearing = crownUpdate(Arrays.asList(
                virtualDay(START_DATE, 1440),
                genuineDay(START_DATE.plusDays(2), 360)));

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldAcceptGenuineDaysOnWindowBoundaries() {
        final UpdateHearingForListing hearing = crownUpdate(Arrays.asList(
                virtualDay(START_DATE, 1440),
                genuineDay(START_DATE, 360),
                genuineDay(END_DATE, 360)));

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    @Test
    void shouldAcceptWhenNoNonDefaultDays() {
        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(crownUpdate(null)));
        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(crownUpdate(Collections.emptyList())));
    }

    @Test
    void shouldSkipDateRulesWhenStartDateAbsent() {
        // weekCommencing shape: no startDate/endDate on the command — only the uniqueness rule applies.
        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withNonDefaultDays(Arrays.asList(
                        virtualDay(START_DATE.plusDays(5), 1440),
                        genuineDay(START_DATE.plusDays(20), 360)))
                .build();

        assertDoesNotThrow(() -> CrownNonDefaultDaysValidator.validateForCrownUpdate(hearing));
    }

    private static UpdateHearingForListing crownUpdate(final List<NonDefaultDay> nonDefaultDays) {
        return UpdateHearingForListing.updateHearingForListing()
                .withHearingId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(START_DATE)
                .withEndDate(END_DATE)
                .withNonDefaultDays(nonDefaultDays)
                .build();
    }

    private static UpdateHearingForListing crownUpdateWindow(final LocalDate start, final LocalDate end,
                                                             final List<NonDefaultDay> nonDefaultDays) {
        return UpdateHearingForListing.updateHearingForListing()
                .withHearingId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(start)
                .withEndDate(end)
                .withNonDefaultDays(nonDefaultDays)
                .build();
    }

    private static NonDefaultDay virtualDay(final LocalDate date, final int duration) {
        return NonDefaultDay.nonDefaultDay()
                .withStartTime(ZonedDateTime.parse(date + "T09:00:00Z"))
                .withDuration(duration)
                .withCourtScheduleId(UUID.randomUUID().toString())
                .withVirtual(Boolean.TRUE)
                .build();
    }

    private static NonDefaultDay virtualDayNoId(final LocalDate date, final int duration) {
        return NonDefaultDay.nonDefaultDay()
                .withStartTime(ZonedDateTime.parse(date + "T09:00:00Z"))
                .withDuration(duration)
                .withVirtual(Boolean.TRUE)
                .build();
    }

    private static NonDefaultDay genuineDay(final LocalDate date, final int duration) {
        return NonDefaultDay.nonDefaultDay()
                .withStartTime(ZonedDateTime.parse(date + "T09:00:00Z"))
                .withDuration(duration)
                .build();
    }
}
