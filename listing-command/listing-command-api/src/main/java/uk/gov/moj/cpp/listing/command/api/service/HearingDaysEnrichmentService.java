package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.command.api.ListingCommandApi.getCourtCentreId;
import static uk.gov.moj.cpp.listing.command.api.util.NonDefaultDayConverter.convertBookedSlotsToHearingDays;
import static uk.gov.moj.cpp.listing.command.api.util.NonDefaultDayConverter.convertCoreNonDefaultDaysToHearingDays;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.getUtcLocalTimeForDate;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;
import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary enrichment service that coordinates the enrichment process. This service is responsible
 * for calculating hearing days, non-sitting days, and non-default days, and for coordinating calls
 * to other enrichment services as needed.
 */
@ApplicationScoped
public class HearingDaysEnrichmentService implements EnrichmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDaysEnrichmentService.class);

    @Inject
    private ReferenceDataCache referenceDataCache;

    /**
     * Enriches a list of hearings by coordinating calls to other enrichment services. This is the
     * primary entry point for batch enrichment operations.
     *
     * @param hearing  The list of hearings to enrich
     * @param envelope The JSON envelope containing context information
     * @return The enriched list of hearings
     */
    @Override
    public HearingListingNeeds enrichHearings(HearingListingNeeds hearing, JsonEnvelope envelope) {
        HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds().withValuesFrom(hearing);
        if (JurisdictionType.MAGISTRATES.equals(hearing.getJurisdictionType())) {
            builder.withHearingDays(enrichHearingDaysForMagistrates(hearing));
            //If its MAGS, there will be only hearingdays, no nondefaultdays, no nonsittingdays
            builder.withNonDefaultDays(emptyList());
            builder.withNonSittingDays(emptyList());
        } else if (JurisdictionType.CROWN.equals(hearing.getJurisdictionType())) {
            // builder.withNonSittingDays(enrichNonSittingDaysForCrown(hearing)); no nonsittingdays on this journey
            // builder.withNonDefaultDays(enrichNonDefaultDaysForCrown(hearing)); no nondefaultdays on this journey
            if (isNull(hearing.getWeekCommencingDate())) {
                builder.withHearingDays(enrichHearingDaysForCrown(hearing));
            }
        }
        calculateStartAndEndDates(builder);
        return builder.build();
    }

    /**
     * Enriches a single hearing by coordinating calls to other enrichment services. This is the
     * primary entry point for single hearing enrichment operations.
     *
     * @param updateHearingForListing The hearing to enrich
     * @param envelope                The JSON envelope containing context information
     * @return The enriched hearing
     */
    @Override
    public UpdateHearingForListing enrichHearing(UpdateHearingForListing updateHearingForListing, JsonEnvelope envelope) {
        return enrichHearing(updateHearingForListing, envelope, null);
    }

    public UpdateHearingForListing enrichHearing(UpdateHearingForListing updateHearingForListing, JsonEnvelope envelope, CourtCentreDetails courtCentreDetails) {
        LOGGER.debug("Enriching hearing {} with court centre details", updateHearingForListing.getHearingId());
        //week commencing, no enrichment needed
        if (isWeekCommencingHearing(updateHearingForListing)) {
            LOGGER.info("Week commencing hearing {}", updateHearingForListing.getHearingId());
            return updateHearingForListing;
        }

        UpdateHearingForListing.Builder builder = UpdateHearingForListing.updateHearingForListing().withValuesFrom(updateHearingForListing);
        if (JurisdictionType.MAGISTRATES.equals(updateHearingForListing.getJurisdictionType())) {
            builder.withHearingDays(enrichHearingDaysForMagistrates(updateHearingForListing));
        } else if (JurisdictionType.CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            final List<LocalDate> nonSittingDays = enrichNonSittingDaysForCrown(updateHearingForListing);
            final List<NonDefaultDay> nonDefaultDays = enrichNonDefaultDaysForCrown(updateHearingForListing, nonSittingDays);
            final List<HearingDay> hearingDays = enrichHearingDaysForCrown(updateHearingForListing, nonSittingDays, nonDefaultDays, courtCentreDetails);
            builder.withNonSittingDays(nonSittingDays);
            builder.withNonDefaultDays(nonDefaultDays);
            builder.withHearingDays(hearingDays);
        }
        calculateStartAndEndDates(builder);

        return builder.build();

    }

    private void calculateStartAndEndDates(HearingListingNeeds.Builder builder) {
        final HearingListingNeeds tempBuilt = builder.build();
        //**CLEANUP
        LOGGER.info("CalculatingStartAndEndDates tempBuilt {}", tempBuilt);
        if (tempBuilt.getHearingDays() != null) {
            LOGGER.info("Hearing days count: {}", tempBuilt.getHearingDays().size());
            tempBuilt.getHearingDays().forEach(hearingDay ->
                    LOGGER.info("Hearing day: {}", hearingDay));
        }
        if (tempBuilt.getNonSittingDays() != null) {
            LOGGER.info("Non-sitting days count: {}", tempBuilt.getNonSittingDays().size());
            tempBuilt.getNonSittingDays().forEach(nonSittingDay ->
                    LOGGER.info("Non-sitting day: {}", nonSittingDay));
        }
        // Get hearing days and non-sitting days
        final List<HearingDay> hearingDays = tempBuilt.getHearingDays();
        final List<LocalDate> nonSittingDays = isNotEmpty(tempBuilt.getNonSittingDays())
                ? tempBuilt.getNonSittingDays().stream()
                .map(LocalDate::parse)
                .toList()
                : emptyList();
        if (hearingDays == null || hearingDays.isEmpty()) {
            return;
        }
        
        final List<HearingDay> validHearingDays = getValidHearingDays(hearingDays, nonSittingDays);
        if (!validHearingDays.isEmpty()) {
            final HearingDay firstHearingDay = validHearingDays.get(0);
            final HearingDay lastHearingDay = validHearingDays.get(validHearingDays.size() - 1);

            final ZonedDateTime earliestStartDateTime = firstHearingDay.getStartTime();
            final ZonedDateTime listedStartDateTime = firstHearingDay.getStartTime();
            final LocalDate endDate = lastHearingDay.getStartTime().toLocalDate();

            builder.withEarliestStartDateTime(earliestStartDateTime);
            builder.withListedStartDateTime(listedStartDateTime);
            builder.withEndDate(endDate.toString());
        }
    }

    private void calculateStartAndEndDates(UpdateHearingForListing.Builder builder) {
        final UpdateHearingForListing tempBuilt = builder.build();
        // Get hearing days and non-sitting days
        final List<HearingDay> hearingDays = tempBuilt.getHearingDays();
        final List<LocalDate> nonSittingDays = tempBuilt.getNonSittingDays();
        if (isEmpty(hearingDays)) {
            return;
        }
        
        final List<HearingDay> validHearingDays = getValidHearingDays(hearingDays, nonSittingDays);
        if (!validHearingDays.isEmpty()) {
            final HearingDay firstHearingDay = validHearingDays.get(0);
            final HearingDay lastHearingDay = validHearingDays.get(validHearingDays.size() - 1);
            builder.withStartDate(firstHearingDay.getStartTime().toLocalDate());
            builder.withEndDate(lastHearingDay.getStartTime().toLocalDate());
        }
    }

    private List<HearingDay> getValidHearingDays(List<HearingDay> hearingDays, List<LocalDate> nonSittingDays) {
        return hearingDays.stream()
                .filter(hearingDay -> isEmpty(nonSittingDays) ||
                        !nonSittingDays.contains(hearingDay.getStartTime().toLocalDate()))
                .sorted(comparing(hearingDay -> hearingDay.getStartTime().toLocalDate()))
                .toList();
    }

    static boolean isWeekCommencingHearing(final UpdateHearingForListing updateHearingForListing) {
        return nonNull(updateHearingForListing.getWeekCommencingStartDate());
    }

    static boolean isWeekCommencingHearing(final HearingListingNeeds hearing) {
        return nonNull(hearing.getWeekCommencingDate());
    }

    private List<HearingDay> enrichHearingDaysForCrown(UpdateHearingForListing updateHearingForListing, List<LocalDate> nonSittingDays, List<NonDefaultDay> nonDefaultDays) {
        return calculateHearingDaysForUpdate(updateHearingForListing, nonSittingDays, nonDefaultDays, false);//LPT-1090 consider if you're going to skip or generate nonSittingDays
    }

    private List<HearingDay> enrichHearingDaysForCrown(UpdateHearingForListing updateHearingForListing, List<LocalDate> nonSittingDays, List<NonDefaultDay> nonDefaultDays, CourtCentreDetails courtCentreDetails) {
        return calculateHearingDaysForUpdate(updateHearingForListing, nonSittingDays, nonDefaultDays, false, courtCentreDetails);//LPT-1090 consider if you're going to skip or generate nonSittingDays
    }

    private List<HearingDay> calculateHearingDaysForUpdate(final UpdateHearingForListing updateHearingForListing, final List<LocalDate> nonSittingDays, final List<NonDefaultDay> nonDefaultDays, boolean excludeWeekends) {
        return calculateHearingDaysForUpdate(updateHearingForListing, nonSittingDays, nonDefaultDays, excludeWeekends, null);
    }

    private List<HearingDay> calculateHearingDaysForUpdate(final UpdateHearingForListing updateHearingForListing, final List<LocalDate> nonSittingDays, final List<NonDefaultDay> nonDefaultDays, boolean excludeWeekends, CourtCentreDetails courtCentreDetails) {
        List<HearingDay> hearingDays = new ArrayList<>();
        //startdate and enddate are mandatory on schema if not weekcommencing.
        LocalDate startDate = updateHearingForListing.getStartDate();
        LocalDate endDate = updateHearingForListing.getEndDate();
        LOGGER.info("Enriching hearingDays for Crown for hearingid: {} startDate: {} endDate: {}", updateHearingForListing.getHearingId(), startDate, endDate);

        LocalDate currentDate = startDate;
        final UUID courtCentreIdForDefaultDays = getCourtCentreIdForDefaultDays(updateHearingForListing);
        final UUID courtRoomIdForDefaultDays = getCourtRoomIdForDefaultDays(updateHearingForListing);

        // Get the default start time from court centre details
        LocalTime defaultStartTime = getDefaultStartTime(courtCentreDetails);

        while (!currentDate.isAfter(endDate)) {
            //skip Sunday if requested because there's a very low chance of finding a session
            if (excludeWeekends && DayOfWeek.SUNDAY.equals(currentDate.getDayOfWeek()) && JurisdictionType.MAGISTRATES.equals(updateHearingForListing.getJurisdictionType())) {
                currentDate = currentDate.plusDays(1);
                continue;
            }
            // Skip non-sitting days
            if (!nonSittingDays.contains(currentDate)) {
                // Check if there's a non-default day for this date
                final LocalDate dateForLambda = currentDate;
                Optional<NonDefaultDay> matchingNonDefaultDay = nonDefaultDays.stream()
                        .filter(nonDefaultDay -> nonDefaultDay.getStartTime().toLocalDate().equals(dateForLambda))
                        .findFirst();

                if (matchingNonDefaultDay.isPresent()) {
                    hearingDays.add(createHearingDayFromNonDefaultDay(matchingNonDefaultDay.get()));
                } else {
                    // Use default court hours for this date
                    HearingDay hearingDay = createDefaultHearingDay(currentDate, courtCentreIdForDefaultDays, courtRoomIdForDefaultDays, defaultStartTime);
                    hearingDays.add(hearingDay);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        return hearingDays;
    }

    private HearingDay createDefaultHearingDay(LocalDate currentDate, UUID courtCentreIdForDefaultDays, UUID courtRoomIdForDefaultDays, LocalTime defaultStartTime) {
        if (defaultStartTime != null) {
            // Use court centre's default start time
            LocalTime defaultStartTimeUTC = getUtcLocalTimeForDate(currentDate, defaultStartTime.getHour(), defaultStartTime.getMinute());
            return HearingDay.hearingDay()
                    .withCourtCentreId(courtCentreIdForDefaultDays)
                    .withCourtRoomId(courtRoomIdForDefaultDays)
                    .withHearingDate(currentDate)
                    .withStartTime(ZonedDateTime.of(currentDate, defaultStartTimeUTC, ZoneOffset.UTC))
                    .withEndTime(ZonedDateTime.of(currentDate, LocalTime.of(17, 0), ZoneOffset.UTC))
                    .build();
        } else {
            // Use default court hours
            return HearingDay.hearingDay()
                    .withCourtCentreId(courtCentreIdForDefaultDays)
                    .withCourtRoomId(courtRoomIdForDefaultDays)
                    .withHearingDate(currentDate)
                    .withStartTime(ZonedDateTime.of(currentDate, LocalTime.of(9, 0), ZoneOffset.UTC))
                    .withEndTime(ZonedDateTime.of(currentDate, LocalTime.of(17, 0), ZoneOffset.UTC))
                    .build();
        }
    }

    private static HearingDay createHearingDayFromNonDefaultDay(final NonDefaultDay nonDefaultDay) {
        HearingDay.Builder hdbuilder = HearingDay.hearingDay()
                .withHearingDate(nonDefaultDay.getStartTime().toLocalDate())
                .withCourtCentreId(fromString(nonDefaultDay.getCourtCentreId()))
                .withStartTime(nonDefaultDay.getStartTime())
                .withDurationMinutes(nonDefaultDay.getDuration())
                .withEndTime(nonDefaultDay.getStartTime().plusMinutes(nonDefaultDay.getDuration()));
        if (nonNull(nonDefaultDay.getRoomId())) {
            hdbuilder.withCourtRoomId(fromString(nonDefaultDay.getRoomId()));
        }
        if (nonNull(nonDefaultDay.getCourtScheduleId())) {
            hdbuilder.withCourtScheduleId(fromString(nonDefaultDay.getCourtScheduleId()));
        }
        return hdbuilder.build();
    }

    private static LocalTime getDefaultStartTime(CourtCentreDetails courtCentreDetails) {
        LocalTime defaultStartTime = LocalTime.of(10, 0); // Default fallback
        if (courtCentreDetails != null && courtCentreDetails.getDefaultStartTime() != null) {
            defaultStartTime = courtCentreDetails.getDefaultStartTime();
        }
        return defaultStartTime;
    }

    private UUID getCourtRoomIdForDefaultDays(final UpdateHearingForListing updateHearingForListing) {
        return nonNull(updateHearingForListing.getSelectedCourtCentre()) ? updateHearingForListing.getSelectedCourtCentre().getCourtRoomId() : updateHearingForListing.getCourtRoomId();
    }

    private UUID getCourtCentreIdForDefaultDays(final UpdateHearingForListing updateHearingForListing) {
        return nonNull(updateHearingForListing.getSelectedCourtCentre()) ? updateHearingForListing.getSelectedCourtCentre().getId() : updateHearingForListing.getCourtCentreId();
    }

    private static List<NonDefaultDay> getValidNonDefaultDays(final List<NonDefaultDay> nonDefaultDays, final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays) {
        return nonDefaultDays.stream()
                .filter(nonDefaultDay -> !nonDefaultDay.getStartTime().toLocalDate().isBefore(startDate) && !nonDefaultDay.getStartTime().toLocalDate().isAfter(endDate))
                .filter(nonDefaultDay -> !nonSittingDays.contains(nonDefaultDay.getStartTime().toLocalDate()))
                .toList();
    }

    private List<HearingDay> enrichHearingDaysForMagistrates(UpdateHearingForListing updateHearingForListing) {
        //In magistrates, hearingdays are represented with nondefaultdays because hearingdays does not exists in api contract
        UpdateHearingForListing.Builder builder = UpdateHearingForListing.updateHearingForListing().withValuesFrom(updateHearingForListing);
        //Workaround will be removed with LPT-1411
        if (isRequestedByOldAllocatedUI(updateHearingForListing) && isMultidayMagsOnUpdate(updateHearingForListing)) {
            return  calculateHearingDaysForUpdate(updateHearingForListing, updateHearingForListing.getNonSittingDays(), updateHearingForListing.getNonDefaultDays(), true);
        }
        enrichByNonDefaultDaysIfPresent(updateHearingForListing, builder);
        enrichByHearingDaysIfPresent(updateHearingForListing, builder);//For Future use
        return builder.build().getHearingDays();
    }

    private boolean isRequestedByOldAllocatedUI(final UpdateHearingForListing updateHearingForListing) {
        return !isNull(updateHearingForListing.getSelectedCourtCentre());
    }

    private boolean isMultidayMagsOnUpdate(final UpdateHearingForListing updateHearingForListing) {
        return updateHearingForListing.getEndDate().isAfter(updateHearingForListing.getStartDate());
    }

    private List<HearingDay> enrichHearingDaysForMagistrates(HearingListingNeeds hearingListingNeeds) {
        //In magistrates, hearingdays are nondefaultdays for now until LPT-1090
        HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds().withValuesFrom(hearingListingNeeds);
        enrichByBookedSlotsIfPresent(hearingListingNeeds, builder);
        enrichByNonDefaultDaysIfPresent(hearingListingNeeds, builder);
        enrichByHearingDaysIfPresent(hearingListingNeeds, builder);
        //even if its unallocated you should have at least one hearingday ( UI requirement)
        enrichCandidate(hearingListingNeeds, builder);
        return builder.build().getHearingDays();
    }

    static String log(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(hearingDay -> String.format("HearingDay{hearingDate=%s, startTime=%s, endTime=%s, sequence=%s, courtCentreId=%s, courtRoomId=%s, courtScheduleId=%s, durationMinutes=%s}",
                        hearingDay.getHearingDate(),
                        hearingDay.getStartTime(),
                        hearingDay.getEndTime(),
                        hearingDay.getSequence(),
                        hearingDay.getCourtCentreId(),
                        hearingDay.getCourtRoomId(),
                        hearingDay.getCourtScheduleId(),
                        hearingDay.getDurationMinutes()))
                .collect(Collectors.joining(", "));
    }


    private void enrichByBookedSlotsIfPresent(HearingListingNeeds hearing, HearingListingNeeds.Builder builder) {
        if (isNotEmpty(hearing.getBookedSlots())) {
            LOGGER.info("enriching HearingDays by BookedSlots hearingid: {}", hearing.getId());
            builder.withHearingDays(convertBookedSlotsToHearingDays(hearing.getBookedSlots()));
        }
    }


    private void enrichByNonDefaultDaysIfPresent(HearingListingNeeds hearing, HearingListingNeeds.Builder builder) {
        if (isNotEmpty(hearing.getNonDefaultDays())) {
            LOGGER.info("enriching HearingDays by NonDefaultDays hearingid: {}", hearing.getId());
            builder.withHearingDays(convertCoreNonDefaultDaysToHearingDays(hearing.getNonDefaultDays()));
        }
    }

    private void enrichByNonDefaultDaysIfPresent(UpdateHearingForListing hearing, UpdateHearingForListing.Builder builder) {
        if (isNotEmpty(hearing.getNonDefaultDays())) {
            LOGGER.info("enriching HearingDays by NonDefaultDays hearingid: {}", hearing.getHearingId());
            builder.withHearingDays(convertNonDefaultDaysToHearingDays(hearing.getNonDefaultDays()));
        }
    }

    private List<HearingDay> convertNonDefaultDaysToHearingDays(List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream().map(nonDefaultDay -> {
            HearingDay.Builder hdbuilder = HearingDay.hearingDay();
            hdbuilder.withHearingDate(nonDefaultDay.getStartTime().toLocalDate())
                    .withCourtCentreId(fromString(nonDefaultDay.getCourtCentreId()))
                    .withDurationMinutes(nonDefaultDay.getDuration())
                    .withStartTime(nonDefaultDay.getStartTime())
                    .withEndTime(nonDefaultDay.getStartTime().plusMinutes(nonDefaultDay.getDuration()));
            if (nonNull(nonDefaultDay.getRoomId())) {
                hdbuilder.withCourtRoomId(fromString(nonDefaultDay.getRoomId()));
            }
            if (nonNull(nonDefaultDay.getCourtScheduleId())) {
                hdbuilder.withCourtScheduleId(fromString(nonDefaultDay.getCourtScheduleId()));
            }
            return hdbuilder.build();
        }).toList();
    }

    private void enrichByHearingDaysIfPresent(HearingListingNeeds hearing, HearingListingNeeds.Builder builder) {
        //If any additional logic needed for hearingdays to hearingdays
        if (isNotEmpty(hearing.getHearingDays())) {
            LOGGER.info("enriching HearingDays by Supplied HearingDays hearingid: {}", hearing.getId());
            builder.withHearingDays(hearing.getHearingDays());
        }
    }

    private void enrichByHearingDaysIfPresent(UpdateHearingForListing hearing, UpdateHearingForListing.Builder builder) {
        //If any additional logic needed for hearingdays to hearingdays
        if (isNotEmpty(hearing.getHearingDays())) {
            LOGGER.info("enriching HearingDays by Supplied HearingDays hearingid: {}", hearing.getHearingId());
            builder.withHearingDays(hearing.getHearingDays());
        }
    }

    private void enrichCandidate(HearingListingNeeds hearing, HearingListingNeeds.Builder builder) {
        if (noHearingDaysPopulatedonPriorSteps(builder)) {
            final ZonedDateTime startTime = nonNull(hearing.getListedStartDateTime()) ? hearing.getListedStartDateTime() : hearing.getEarliestStartDateTime();
            if (isNull(startTime)) {
                LOGGER.warn("Cannot enrich hearing days for hearing {}: both listedStartDateTime and earliestStartDateTime are null", hearing.getId());
                return;
            }
            LOGGER.info("enriching HearingDays by By AllocationCandidate hearingid: {}", hearing.getId());
            builder.withHearingDays(List.of(HearingDay.hearingDay()
                    .withHearingDate(startTime.toLocalDate())
                    .withStartTime(startTime)
                    .withCourtCentreId(hearing.getCourtCentre().getId())
                    .withCourtRoomId(hearing.getCourtCentre().getRoomId())
                    .withDurationMinutes(hearing.getEstimatedMinutes())
                    .withEndTime(nonNull(hearing.getEstimatedMinutes()) ? startTime.plusMinutes(hearing.getEstimatedMinutes()) : null)
                    .build()));
        }
    }

    static boolean noHearingDaysPopulatedonPriorSteps(final HearingListingNeeds.Builder builder) {
        return isEmpty(builder.build().getHearingDays());
    }

    private List<NonDefaultDay> enrichNonDefaultDaysForCrown(UpdateHearingForListing updateHearingForListing, List<LocalDate> nonSittingDays) {
        List<NonDefaultDay> validNonDefaultDays = getValidNonDefaultDays(updateHearingForListing.getNonDefaultDays(), updateHearingForListing.getStartDate(), updateHearingForListing.getEndDate(), nonSittingDays);
        return enrichValidNonDefaultDays(updateHearingForListing, validNonDefaultDays);
    }

    private List<NonDefaultDay> enrichValidNonDefaultDays(UpdateHearingForListing updateHearingForListing, List<NonDefaultDay> validNonDefaultDays) {
        return validNonDefaultDays.stream().map(nonDefaultDay -> {
            LOGGER.info("enriching NonDefaultDays by NonDefaultDays hearingid: {}", updateHearingForListing.getHearingId());
            final UUID courtCentreId = nonNull(nonDefaultDay.getCourtCentreId()) ? fromString(nonDefaultDay.getCourtCentreId()) : getCourtCentreId(updateHearingForListing);
            final UUID courtRoomId = nonNull(nonDefaultDay.getRoomId()) ? fromString(nonDefaultDay.getRoomId()) : getCourtRoomId(updateHearingForListing);
            NonDefaultDay.Builder builder = NonDefaultDay.nonDefaultDay().withValuesFrom(nonDefaultDay);
            builder.withCourtCentreId(courtCentreId.toString());
            if (nonNull(courtRoomId)) {
                builder.withRoomId(courtRoomId.toString());
                builder.withCourtRoomId(getCpCourtRoomNumber(courtCentreId, courtRoomId));
            }
            return builder.build();
        }).toList();
    }

    private Integer getCpCourtRoomNumber(final UUID courtCentreId, final UUID courtRoomUUID) {
        return referenceDataCache.getCpCourtRoomCache(courtCentreId)
                .stream()
                .filter(cpCourtRoom -> UUID.fromString(cpCourtRoom.getString("id")).equals(courtRoomUUID))
                .findFirst()
                .map(cpCourtRoom -> cpCourtRoom.getInt("courtroomId"))
                .orElseThrow(() -> new InvalidReferenceDataException("Cannot find court room uuid " + courtRoomUUID));
    }


    static UUID getCourtRoomId(final UpdateHearingForListing updateHearingForListing) {
        return nonNull(updateHearingForListing.getSelectedCourtCentre()) ? updateHearingForListing.getSelectedCourtCentre().getCourtRoomId() : updateHearingForListing.getCourtRoomId();
    }

    private List<LocalDate> enrichNonSittingDaysForCrown(UpdateHearingForListing updateHearingForListing) {
        return isNotEmpty(updateHearingForListing.getNonSittingDays()) ? updateHearingForListing.getNonSittingDays() : emptyList();
    }

    private List<HearingDay> enrichHearingDaysForCrown(HearingListingNeeds hearingListingNeeds) {
        HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds().withValuesFrom(hearingListingNeeds);
        enrichByBookedSlotsIfPresent(hearingListingNeeds, builder);
        enrichByNonDefaultDaysIfPresent(hearingListingNeeds, builder);
        enrichCandidate(hearingListingNeeds, builder);
        return builder.build().getHearingDays();
    }
}
