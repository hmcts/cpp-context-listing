package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.listing.command.api.service.HearingDaysEnrichmentService.isWeekCommencingHearing;
import static uk.gov.moj.cpp.listing.command.api.service.HearingDurationEnrichmentService.DEFAULT_MIN;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;

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
                LOGGER.info("Enrich list court hearing for CROWN hearingid: {}", hearing.getId());
                // For crown: Hearing Days -> Duration -> Court Schedule
                HearingListingNeeds withHearingDays = hearingDaysEnrichmentService.enrichHearings(hearing, envelope);
                HearingListingNeeds withDurations = hearingDurationEnrichmentService.enrichWithDurations(withHearingDays, envelope);
                HearingListingNeeds withCourtSchedules = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDurations, envelope);
                enrichedHearings.add(withCourtSchedules);
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
            // For crown: Hearing Days -> Duration -> Court Schedule
            UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(hearing, envelope);
            UpdateHearingForListing withDuration = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
            enrichedHearing = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope);
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
            // For crown: Hearing Days -> Duration -> Court Schedule
            UpdateHearingForListing withHearingDays = hearingDaysEnrichmentService.enrichHearing(hearing, envelope, courtCentreDetails);
            UpdateHearingForListing withDuration = hearingDurationEnrichmentService.enrichWithDurationForUpdate(withHearingDays, envelope);
            enrichedHearing = courtScheduleEnrichmentService.enrichWithCourtSchedules(withDuration, envelope);
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
}