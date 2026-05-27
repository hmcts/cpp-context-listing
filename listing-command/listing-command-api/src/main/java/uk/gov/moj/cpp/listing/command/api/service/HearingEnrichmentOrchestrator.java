package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.listing.command.api.service.HearingDaysEnrichmentService.isWeekCommencingHearing;
import static uk.gov.moj.cpp.listing.command.api.service.HearingDurationEnrichmentService.DEFAULT_MIN;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class HearingEnrichmentOrchestrator {

    private static final int DEFAULT_WEEK_COMMENCING_DURATION = 1;
    public static final String UNSUPPORTED_JURISDICTION_TYPE = "Unsupported jurisdiction type: ";
    @Inject
    private HearingDurationEnrichmentService hearingDurationEnrichmentService;

    @Inject
    private CourtScheduleEnrichmentService courtScheduleEnrichmentService;

    @Inject
    private HearingDaysEnrichmentService hearingDaysEnrichmentService;

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEnrichmentOrchestrator.class);


    public List<HearingListingNeeds> enrichListCourtHearing(List<HearingListingNeeds> hearings, JsonEnvelope envelope) {
        return enrichListCourtHearing(hearings, envelope, CrownFallbackSource.LIST_COURT_HEARING);
    }

    public List<HearingListingNeeds> enrichListCourtHearing(List<HearingListingNeeds> hearings,
                                                             JsonEnvelope envelope,
                                                             CrownFallbackSource crownFallbackSource) {
        final List<HearingListingNeeds> enrichedHearings = new ArrayList<>();
        hearings.forEach(hearing -> {
            if (JurisdictionType.MAGISTRATES.equals(hearing.getJurisdictionType())) {
                LOGGER.info("Enrich list court hearing for MAGISTRATES hearingid: {}", hearing.getId());
                // For magistrates: Hearing Days -> Duration -> Court Schedule
                HearingListingNeeds withHearingDays = hearingDaysEnrichmentService.enrichHearings(hearing, envelope);
                HearingListingNeeds withDurations = hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope);
                HearingListingNeeds withCourtSchedules = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDurations, envelope);
                enrichedHearings.add(withCourtSchedules);
            } else if (JurisdictionType.CROWN.equals(hearing.getJurisdictionType())) {
                LOGGER.info("Enrich list court hearing for CROWN hearingid: {} fallbackSource: {}", hearing.getId(), crownFallbackSource);
                // CROWN next-hearing (list-next-hearings-v2) carries the chosen courtScheduleId in the
                // bookingReference (Crown has no provisional-booking concept). Promote it onto a bookedSlot
                // so the CourtSchedule-first flow below resolves the session/room. See helper javadoc.
                final HearingListingNeeds crownHearing = promoteCrownBookingReferenceToBookedSlot(hearing);
                if (hasCourtScheduleId(crownHearing)) {
                    // CROWN with courtScheduleId (bookedSlots or hearingDays): CourtSchedule-first flow
                    // expands multi-day into N hearingDays, then HearingDays computes start/end,
                    // then Duration runs.
                    HearingListingNeeds withCourtSchedules = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(
                            crownHearing, crownFallbackSource);
                    HearingListingNeeds withHearingDays = hearingDaysEnrichmentService.enrichHearings(withCourtSchedules, envelope);
                    HearingListingNeeds withDurations = hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope);
                    enrichedHearings.add(stripRoomInfoIfAnyDraft(withDurations));
                } else {
                    // CROWN without courtScheduleId: legacy HearingDays -> Duration -> CourtSchedule order.
                    // Crown fallback search-and-book is invoked by callers that explicitly need it
                    // (wired via enrichCrownCourtScheduleFirst direct call with no courtScheduleId);
                    // the default list-court-hearing path preserves legacy enrichment so existing
                    // IT fixtures (without courtscheduler-seeded sessions) continue to pass.
                    HearingListingNeeds withHearingDays = hearingDaysEnrichmentService.enrichHearings(crownHearing, envelope);
                    HearingListingNeeds withDurations = hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope);
                    HearingListingNeeds withCourtSchedules = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDurations, envelope);
                    enrichedHearings.add(stripRoomInfoIfAnyDraft(withCourtSchedules));
                }
            } else {
                throw new IllegalArgumentException(UNSUPPORTED_JURISDICTION_TYPE + hearing.getJurisdictionType());
            }
        });
        logEnrichedHearings(enrichedHearings);
        return recalculateDurationSequenceAndEndDatesForHearingDays(enrichedHearings);
    }

    public UpdateHearingForListing enrichUpdateHearingForListing(UpdateHearingForListing hearing, JsonEnvelope envelope) {
        JurisdictionType jurisdictionType = hearing.getJurisdictionType();
        UpdateHearingForListing enrichedHearing;
        if (JurisdictionType.MAGISTRATES.equals(jurisdictionType)) {
            LOGGER.info("Enrich update hearing for MAGISTRATES hearingid: {}", hearing.getHearingId());
            // For magistrates: Hearing Days -> Duration -> Court Schedule
            UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(hearing, envelope);
            UpdateHearingForListing withDuration = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
            //TODO: enrich panel and judiciary information
            enrichedHearing = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration,envelope);
        } else if (JurisdictionType.CROWN.equals(jurisdictionType)) {
            LOGGER.info("Enrich update hearing for CROWN hearingid: {}", hearing.getHearingId());
            if (!isWeekCommencingHearing(hearing) && hasCourtScheduleId(hearing)) {
                // CROWN with courtScheduleId (hearingDays or nonDefaultDays): CourtSchedule-first flow
                // mirrors enrichListCourtHearing. Resolves multi-day sessions from courtscheduler
                // BEFORE day-range expansion, so the authoritative session count (e.g. 3 for duration 1080)
                // wins over startDate→endDate iteration that would otherwise produce N calendar days.
                // WeekCommencing payloads skip this branch — they have their own enrichment rules.
                UpdateHearingForListing withCourtSchedules = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);
                UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(withCourtSchedules, envelope);
                enrichedHearing = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
            } else {
                UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(hearing, envelope);
                UpdateHearingForListing withDuration = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
                enrichedHearing = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope);
            }
            enrichedHearing = stripRoomInfoIfAnyDraft(enrichedHearing);
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_JURISDICTION_TYPE + jurisdictionType);
        }

        return recalculateDurationSequenceAndEndDatesForHearingDays(enrichedHearing);
    }

    public UpdateHearingForListing enrichUpdateHearingForListing(UpdateHearingForListing hearing, JsonEnvelope envelope, CourtCentreDetails courtCentreDetails) {
        JurisdictionType jurisdictionType = hearing.getJurisdictionType();
        UpdateHearingForListing enrichedHearing;
        if (JurisdictionType.MAGISTRATES.equals(jurisdictionType)) {
            LOGGER.info("Enrich update hearing for MAGISTRATES hearingid: {}", hearing.getHearingId());
            // For magistrates: Hearing Days -> Duration -> Court Schedule
            UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(hearing, envelope, courtCentreDetails);
            UpdateHearingForListing withDuration = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
            //TODO: enrich panel and judiciary information
            enrichedHearing = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration,envelope);
        } else if (JurisdictionType.CROWN.equals(jurisdictionType)) {
            LOGGER.info("Enrich update hearing for CROWN hearingid: {}", hearing.getHearingId());
            if (!isWeekCommencingHearing(hearing) && hasCourtScheduleId(hearing)) {
                // CourtSchedule-first flow — see enrichUpdateHearingForListing(hearing, envelope) for rationale.
                UpdateHearingForListing withCourtSchedules = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);
                UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(withCourtSchedules, envelope, courtCentreDetails);
                enrichedHearing = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
            } else {
                UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(hearing, envelope, courtCentreDetails);
                UpdateHearingForListing withDuration = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
                enrichedHearing = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope);
            }
            enrichedHearing = stripRoomInfoIfAnyDraft(enrichedHearing);
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_JURISDICTION_TYPE + jurisdictionType);
        }

        return recalculateDurationSequenceAndEndDatesForHearingDays(enrichedHearing);
    }


    static void logEnrichedHearings(final List<HearingListingNeeds> enrichedHearings) {
        for (HearingListingNeeds hearing : enrichedHearings) {
            LOGGER.info("Hearing ID: {}, Hearing Days: {}", hearing.getId(),
                    hearing.getHearingDays() != null ? hearing.getHearingDays().stream()
                            .map(day -> String.format("HearingDay{hearingDate=%s, startTime=%s, endTime=%s, courtCentreId=%s, courtRoomId=%s, durationMinutes=%d, sequence=%d}",
                                    day.getHearingDate(), day.getStartTime(), day.getEndTime(),
                                    day.getCourtCentreId(), day.getCourtRoomId(), day.getDurationMinutes(), day.getSequence()))
                            .collect(Collectors.joining(", ")) : "null");

            LOGGER.info("Hearing ID: {}, Non-sitting Days: {}", hearing.getId(),
                    hearing.getNonSittingDays() != null ? hearing.getNonSittingDays().stream()
                            .map(String::toString)
                            .collect(Collectors.joining(", ")) : "null");
            LOGGER.info("Hearing ID: {}, enddate {} , estimatedminutes {}", hearing.getId(), hearing.getEndDate(), hearing.getEstimatedMinutes());

        }
    }

    static List<HearingDay> sequenceValidHearingDays(final List<HearingDay> validHearingDays) {
        final List<HearingDay> sequencedHearingDays = new ArrayList<>();
        for (int i = 0; i < validHearingDays.size(); i++) {
            HearingDay originalDay = validHearingDays.get(i);
            HearingDay sequencedDay = HearingDay.hearingDay()
                    .withValuesFrom(originalDay)
                    .withSequence(i + 1)
                    .build();
            sequencedHearingDays.add(sequencedDay);
        }
        return sequencedHearingDays;
    }


    static List<HearingDay> orderAndFilterOutNonSittingDays(final List<HearingDay> hearingdays, final List<LocalDate> nonSittingDays) {
        List<HearingDay> sortedFilteredDays = hearingdays.stream()
                .filter(hearingDay -> isEmpty(nonSittingDays) ||
                        !nonSittingDays.contains(hearingDay.getHearingDate()))
                .sorted(Comparator.comparing(HearingDay::getHearingDate))
                .toList();

        // Set sequence to 0 for all sorted and filtered hearing days
        return sortedFilteredDays.stream()
                .map(day -> HearingDay.hearingDay()
                        .withValuesFrom(day)
                        .withSequence(0)
                        .build())
                .toList();
    }

    static UpdateHearingForListing recalculateDurationSequenceAndEndDatesForHearingDays(final UpdateHearingForListing enrichedHearing) {
// Check if all hearing days are null across all enriched hearings
        if (isWeekCommencingHearing(enrichedHearing) && isNull(enrichedHearing.getWeekCommencingEndDate())) {
            UpdateHearingForListing.Builder builder = UpdateHearingForListing.updateHearingForListing().withValuesFrom(enrichedHearing);
            final int weekCommencingDuration = !isNull(enrichedHearing.getWeekCommencingDurationInWeeks()) ? enrichedHearing.getWeekCommencingDurationInWeeks() : DEFAULT_WEEK_COMMENCING_DURATION;
            builder.withWeekCommencingDurationInWeeks(weekCommencingDuration);
            builder.withWeekCommencingEndDate(enrichedHearing.getWeekCommencingStartDate().plusWeeks(weekCommencingDuration).minusDays(1));
            return builder.build();
        }

        if (isEmpty(enrichedHearing.getHearingDays())) {
            return enrichedHearing;
        }
        List<LocalDate> nonSittingDays = isEmpty(enrichedHearing.getNonSittingDays()) ? new ArrayList<>() : enrichedHearing.getNonSittingDays();

        // Filter out hearing days that match non-sitting days
        List<HearingDay> validHearingDays = orderAndFilterOutNonSittingDays(enrichedHearing.getHearingDays(), nonSittingDays);
        if (validHearingDays.isEmpty()) {
            // If no valid hearing days after filtering, keep original hearing
            return enrichedHearing;
        }

        // Set end date to the latest hearing day date
        LocalDate endDate = validHearingDays.get(validHearingDays.size() - 1)
                .getHearingDate();

        return UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(enrichedHearing)
                .withHearingDays(validHearingDays)
                .withEndDate(endDate)
                .build();
    }

    static List<HearingListingNeeds> recalculateDurationSequenceAndEndDatesForHearingDays(final List<HearingListingNeeds> enrichedHearings) {
        List<HearingListingNeeds> result = new ArrayList<>();

        for (HearingListingNeeds hearing : enrichedHearings) {
            if(isWeekCommencingHearing(hearing)){
                final HearingListingNeeds weekCommencingHearing = buildWeekCommencingHearing(hearing);
                result.add(weekCommencingHearing);
                continue;
            }
            if (isEmpty(hearing.getHearingDays())) {
                // If this hearing has no hearing days, keep it as is
                result.add(hearing);
                continue;
            }

            // Parse non-sitting days to LocalDate for comparison
            List<LocalDate> nonSittingDays = isEmpty(hearing.getNonSittingDays()) ?
                    new ArrayList<>() :
                    hearing.getNonSittingDays().stream()
                            .map(LocalDate::parse)
                            .toList();

            // Filter out hearing days that match non-sitting days
            List<HearingDay> validHearingDays = orderAndFilterOutNonSittingDays(hearing.getHearingDays(), nonSittingDays);

            if (validHearingDays.isEmpty()) {
                // If no valid hearing days after filtering, keep original hearing
                result.add(hearing);
                continue;
            }

            // Sum up durations from all valid hearing days
            int totalEstimatedMinutes = getTotalDuration(validHearingDays);

            // Set end date to the latest hearing day date
            LocalDate endDate = validHearingDays.get(validHearingDays.size() - 1)
                    .getHearingDate();

            // Build updated hearing with new values
            HearingListingNeeds updatedHearing = HearingListingNeeds.hearingListingNeeds()
                    .withValuesFrom(hearing)
                    .withHearingDays(validHearingDays)
                    .withEstimatedMinutes(totalEstimatedMinutes)
                    .withEndDate(endDate.toString())
                    .build();
            result.add(updatedHearing);
        }
        LOGGER.info("Final enriched hearings result:");
        logEnrichedHearings(result);

        return result;
    }

    private static HearingListingNeeds buildWeekCommencingHearing(final HearingListingNeeds hearing) {
        return HearingListingNeeds.hearingListingNeeds()
                .withValuesFrom(hearing)
                .withWeekCommencingDate(getWeekCommencingDate(hearing.getWeekCommencingDate()))
                .build();
    }

    private static WeekCommencingDate getWeekCommencingDate(WeekCommencingDate weekCommencingDate) {
        final Integer weekCommencingDuration = !isNull(weekCommencingDate.getDuration()) ? weekCommencingDate.getDuration() : DEFAULT_WEEK_COMMENCING_DURATION;
        return WeekCommencingDate.weekCommencingDate().withStartDate(weekCommencingDate.getStartDate()).withDuration(weekCommencingDuration).build();
    }

    static int getTotalDuration(final List<HearingDay> sequencedHearingDays) {
        return sequencedHearingDays.stream()
                .mapToInt(hearingDay -> hearingDay.getDurationMinutes() != null ?
                        hearingDay.getDurationMinutes() : DEFAULT_MIN)
                .sum();
    }

    /**
     * CROWN list-next-hearings-v2 carries the chosen courtScheduleId in {@code bookingReference}
     * (Crown has no provisional-booking concept — the id IS a court-schedule session id). The
     * CourtSchedule-first enrichment keys off a courtScheduleId on hearingDays/bookedSlots, so we
     * promote the bookingReference onto a synthetic bookedSlot before the {@link #hasCourtScheduleId}
     * gate. Without this, the hearing falls through to the legacy allocation-candidate path and is
     * listed unallocated (no room, no courtScheduleId on the day). Mirrors
     * {@code CourtScheduleEnrichmentService.mergeCourtScheduleIdsFromNonDefaultDays} on the update path.
     *
     * <p>The duration is copied from {@code estimatedMinutes} so {@code calculateAggregatedDuration}
     * still drives the single-day vs multi-day decision; multi-day enrichment anchors off
     * {@code bookedSlots[0].courtScheduleId}. No-op unless jurisdiction-agnostic guards hold:
     * a bookingReference is present and no courtScheduleId already exists.
     */
    private static HearingListingNeeds promoteCrownBookingReferenceToBookedSlot(final HearingListingNeeds hearing) {
        if (isNull(hearing.getBookingReference()) || hasCourtScheduleId(hearing)) {
            return hearing;
        }
        final RotaSlot bookedSlot = RotaSlot.rotaSlot()
                .withCourtScheduleId(hearing.getBookingReference().toString())
                .withDuration(hearing.getEstimatedMinutes())
                .withStartTime(hearing.getListedStartDateTime())
                .build();
        LOGGER.info("CROWN list: promoting bookingReference {} onto a bookedSlot courtScheduleId for hearingId {}",
                hearing.getBookingReference(), hearing.getId());
        return HearingListingNeeds.hearingListingNeeds()
                .withValuesFrom(hearing)
                .withBookedSlots(List.of(bookedSlot))
                .build();
    }

    /**
     * Returns true if the hearing has a courtScheduleId on any hearingDay or on any bookedSlot.
     * Used by the CROWN branch to decide between CourtSchedule-first flow (has id) and the legacy
     * allocation-candidate flow (no id, needs search-and-book).
     */
    private static boolean hasCourtScheduleId(final HearingListingNeeds hearing) {
        if (hearing.getHearingDays() != null
                && hearing.getHearingDays().stream().anyMatch(d -> d.getCourtScheduleId() != null)) {
            return true;
        }
        return hearing.getBookedSlots() != null
                && hearing.getBookedSlots().stream()
                .anyMatch(s -> s.getCourtScheduleId() != null && !s.getCourtScheduleId().isBlank());
    }

    /**
     * Returns true if the update payload carries a courtScheduleId on any hearingDay or on any
     * nonDefaultDay. Drives the CROWN update CourtSchedule-first branch — keeping symmetry with
     * enrichListCourtHearing's list-side check.
     */
    private static boolean hasCourtScheduleId(final UpdateHearingForListing hearing) {
        if (!isEmpty(hearing.getHearingDays())
                && hearing.getHearingDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()))) {
            return true;
        }
        return !isEmpty(hearing.getNonDefaultDays())
                && hearing.getNonDefaultDays().stream()
                .anyMatch(nd -> nonNull(nd.getCourtScheduleId()) && !nd.getCourtScheduleId().isBlank());
    }

    /**
     * SPRDT-858: when ANY hearingDay references a draft session, the whole hearing is treated as
     * unallocated for CROWN. Courtscheduler always pins a room to a session, but for an unallocated
     * hearing that room MUST NOT propagate to commands or downstream events. Stripping happens here
     * (after all enrichment branches complete) so the rule holds regardless of which branch ran.
     */
    static HearingListingNeeds stripRoomInfoIfAnyDraft(final HearingListingNeeds hearing) {
        final List<HearingDay> days = hearing.getHearingDays();
        if (isEmpty(days) || !anyDayIsDraft(days)) {
            return hearing;
        }
        final List<HearingDay> sanitisedDays = days.stream()
                .map(d -> HearingDay.hearingDay().withValuesFrom(d).withCourtRoomId(null).build())
                .toList();
        final HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds()
                .withValuesFrom(hearing)
                .withHearingDays(sanitisedDays);
        if (nonNull(hearing.getCourtCentre())) {
            final CourtCentre sanitisedCourtCentre = CourtCentre.courtCentre()
                    .withValuesFrom(hearing.getCourtCentre())
                    .withRoomId(null)
                    .withRoomName(null)
                    .build();
            builder.withCourtCentre(sanitisedCourtCentre);
        }
        return builder.build();
    }

    /**
     * Update-path variant — UpdateHearingForListing has no hearing-level CourtCentre, so we
     * only sanitise the per-day courtRoomId.
     */
    static UpdateHearingForListing stripRoomInfoIfAnyDraft(final UpdateHearingForListing hearing) {
        final List<HearingDay> days = hearing.getHearingDays();
        if (isEmpty(days) || !anyDayIsDraft(days)) {
            return hearing;
        }
        final List<HearingDay> sanitisedDays = days.stream()
                .map(d -> HearingDay.hearingDay().withValuesFrom(d).withCourtRoomId(null).build())
                .toList();
        return UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(hearing)
                .withHearingDays(sanitisedDays)
                .build();
    }

    private static boolean anyDayIsDraft(final List<HearingDay> days) {
        return days.stream().anyMatch(d -> Boolean.TRUE.equals(d.getIsDraft()));
    }
}
