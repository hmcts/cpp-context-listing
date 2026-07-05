package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.listing.command.api.service.HearingDurationEnrichmentService.MINUTES_IN_DAY;

import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Payload-shape rules for CROWN update-hearing-for-listing nonDefaultDays.
 *
 * <p>Two DIFFERENT virtual=true shapes exist in the wild and must be told apart by duration:
 * <ul>
 *   <li><b>Block descriptor</b> — ONE virtual day whose duration spans the whole block
 *       (&gt; {@code MINUTES_IN_DAY}) and whose courtScheduleId anchors the multi-day booking
 *       (the frontend change-hearing-details shape). The rules below apply to this shape.</li>
 *   <li><b>Per-day proxies</b> — N virtual days each ≤ one court day, carrying per-day room/slot
 *       info (e.g. the court-room-change flow, HearingDayCourtRoomChangeForCrownIT). These are
 *       filtered out before the aggregate like any proxy and are NOT constrained here.</li>
 * </ul>
 *
 * Rules for the block-descriptor shape (violations are caller errors → 400, mirroring the
 * date checks in ListingCommandApi):
 * <ul>
 *   <li>at most ONE block descriptor per payload;</li>
 *   <li>the command's startDate must equal the descriptor's date (it anchors the block);</li>
 *   <li>genuine days (virtual absent/false) must fall within startDate..endDate. Enforced only
 *       when a descriptor is present — legacy callers without one send stale out-of-window
 *       genuine days and rely on the long-standing silent filter in
 *       {@code HearingDaysEnrichmentService.getValidNonDefaultDays}.</li>
 * </ul>
 *
 * Per-day session-selection rules for the per-day proxy shape (only when NO block descriptor
 * exists), 400ing with the {@code INCOMPLETE_SESSION_SELECTION} code:
 * <ul>
 *   <li>if any per-day proxy carries a courtScheduleId, ALL of them must (mixed presence is
 *       ambiguous — ids absent on every proxy is the legacy court-room-override shape and is
 *       left unconstrained);</li>
 *   <li>proxy dates must be distinct (no duplicates);</li>
 *   <li>proxy dates must fall within startDate..endDate;</li>
 *   <li>when ids are present, one proxy must cover every sitting day of startDate..endDate
 *       (weekends and nonSittingDays are exempt — Crown does not sit weekends).</li>
 * </ul>
 */
public final class CrownNonDefaultDaysValidator {

    private CrownNonDefaultDaysValidator() {
    }

    static void validateForCrownUpdate(final UpdateHearingForListing hearing) {
        final List<NonDefaultDay> nonDefaultDays = hearing.getNonDefaultDays();
        if (isEmpty(nonDefaultDays)) {
            return;
        }

        final List<NonDefaultDay> blockDescriptors = nonDefaultDays.stream()
                .filter(CrownNonDefaultDaysValidator::isBlockDescriptor)
                .toList();

        if (blockDescriptors.size() > 1) {
            throw new BadRequestException(
                    "CROWN update-hearing-for-listing: at most one virtual nonDefaultDay may carry the block total (duration > "
                            + MINUTES_IN_DAY + "), found " + blockDescriptors.size()
                            + " for hearingId " + hearing.getHearingId());
        }

        if (blockDescriptors.isEmpty()) {
            validatePerDaySessionSelection(hearing, nonDefaultDays);
            return;
        }

        final LocalDate startDate = hearing.getStartDate();
        final ZonedDateTime descriptorStartTime = blockDescriptors.get(0).getStartTime();
        if (nonNull(startDate) && nonNull(descriptorStartTime)
                && !startDate.equals(descriptorStartTime.toLocalDate())) {
            throw new BadRequestException(
                    "CROWN update-hearing-for-listing: startDate " + startDate
                            + " must equal the virtual nonDefaultDay's date " + descriptorStartTime.toLocalDate()
                            + " for hearingId " + hearing.getHearingId());
        }

        final LocalDate endDate = hearing.getEndDate();
        if (nonNull(startDate) && nonNull(endDate)) {
            final List<LocalDate> outsideWindow = nonDefaultDays.stream()
                    .filter(nd -> !Boolean.TRUE.equals(nd.getVirtual()))
                    .map(NonDefaultDay::getStartTime)
                    .filter(t -> nonNull(t))
                    .map(ZonedDateTime::toLocalDate)
                    .filter(d -> d.isBefore(startDate) || d.isAfter(endDate))
                    .toList();
            if (!outsideWindow.isEmpty()) {
                throw new BadRequestException(
                        "CROWN update-hearing-for-listing: nonDefaultDays without virtual=true must fall within startDate "
                                + startDate + " and endDate " + endDate + "; outside: " + outsideWindow
                                + " for hearingId " + hearing.getHearingId());
            }
        }
    }

    /**
     * A virtual day that claims to describe the whole multi-day block: its duration exceeds one
     * court day. Per-day virtual proxies (duration ≤ MINUTES_IN_DAY) do not qualify.
     */
    static boolean isBlockDescriptor(final NonDefaultDay nonDefaultDay) {
        return Boolean.TRUE.equals(nonDefaultDay.getVirtual())
                && nonNull(nonDefaultDay.getDuration())
                && nonDefaultDay.getDuration() > MINUTES_IN_DAY;
    }

    /**
     * Per-day session-selection rules (only when NO block descriptor exists): when every virtual
     * proxy carries a courtScheduleId the payload claims "these ARE the chosen sessions", so it
     * must name one session for every sitting day of startDate..endDate (weekends and
     * nonSittingDays exempt — Crown does not sit weekends). Ids on only SOME proxies is ambiguous.
     * Violations are caller errors -> 400 with the INCOMPLETE_SESSION_SELECTION code so the UI can
     * key the failure, mirroring the NO_SESSION_FOUND pattern.
     */
    private static void validatePerDaySessionSelection(final UpdateHearingForListing hearing,
                                                       final List<NonDefaultDay> nonDefaultDays) {
        final List<NonDefaultDay> perDayProxies = nonDefaultDays.stream()
                .filter(nd -> Boolean.TRUE.equals(nd.getVirtual()))
                .toList();
        if (perDayProxies.isEmpty()) {
            return;
        }
        final long withIds = perDayProxies.stream().filter(CrownNonDefaultDaysValidator::carriesCourtScheduleId).count();
        if (withIds == 0) {
            return; // legacy court-room-override shape: no session selection claimed
        }
        if (withIds < perDayProxies.size()) {
            throw new BadRequestException(
                    "INCOMPLETE_SESSION_SELECTION: all virtual nonDefaultDays must carry a courtScheduleId when any does; "
                            + withIds + " of " + perDayProxies.size() + " have one for hearingId " + hearing.getHearingId());
        }

        final LocalDate startDate = hearing.getStartDate();
        final LocalDate endDate = hearing.getEndDate();
        if (isNull(startDate) || isNull(endDate)) {
            return; // weekCommencing payloads carry no window
        }

        final List<LocalDate> proxyDates = perDayProxies.stream()
                .map(NonDefaultDay::getStartTime)
                .filter(Objects::nonNull)
                .map(ZonedDateTime::toLocalDate)
                .toList();
        final Set<LocalDate> distinctDates = new HashSet<>(proxyDates);
        if (distinctDates.size() < proxyDates.size()) {
            throw new BadRequestException(
                    "INCOMPLETE_SESSION_SELECTION: duplicate virtual nonDefaultDay dates " + proxyDates
                            + " for hearingId " + hearing.getHearingId());
        }
        final List<LocalDate> outsideWindow = proxyDates.stream()
                .filter(d -> d.isBefore(startDate) || d.isAfter(endDate))
                .toList();
        if (!outsideWindow.isEmpty()) {
            throw new BadRequestException(
                    "INCOMPLETE_SESSION_SELECTION: virtual nonDefaultDays outside window " + startDate + ".."
                            + endDate + ": " + outsideWindow + " for hearingId " + hearing.getHearingId());
        }

        final List<LocalDate> nonSittingDays = isEmpty(hearing.getNonSittingDays())
                ? List.of() : hearing.getNonSittingDays();
        final List<LocalDate> uncovered = startDate.datesUntil(endDate.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .filter(d -> !nonSittingDays.contains(d))
                .filter(d -> !distinctDates.contains(d))
                .toList();
        if (!uncovered.isEmpty()) {
            throw new BadRequestException(
                    "INCOMPLETE_SESSION_SELECTION: virtual nonDefaultDays with courtScheduleIds must cover every sitting day in "
                            + startDate + ".." + endDate + "; uncovered: " + uncovered
                            + " for hearingId " + hearing.getHearingId());
        }
    }

    private static boolean carriesCourtScheduleId(final NonDefaultDay nonDefaultDay) {
        return nonNull(nonDefaultDay.getCourtScheduleId()) && !nonDefaultDay.getCourtScheduleId().isBlank();
    }
}
