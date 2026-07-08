package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CrownNonDefaultDaysValidatorTest {

    private static final LocalDate START_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate END_DATE = START_DATE.plusDays(3);

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
    void shouldAcceptMultiplePerDayVirtualProxies() {
        // Court-room-change shape (HearingDayCourtRoomChangeForCrownIT): N virtual days each ≤ one
        // court day, dates inside the window but NOT on startDate — must stay accepted.
        final UpdateHearingForListing hearing = crownUpdate(Arrays.asList(
                virtualDay(START_DATE.plusDays(2), 360),
                virtualDay(START_DATE.plusDays(3), 360)));

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

    private static NonDefaultDay virtualDay(final LocalDate date, final int duration) {
        return NonDefaultDay.nonDefaultDay()
                .withStartTime(ZonedDateTime.parse(date + "T09:00:00Z"))
                .withDuration(duration)
                .withCourtScheduleId(UUID.randomUUID().toString())
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
