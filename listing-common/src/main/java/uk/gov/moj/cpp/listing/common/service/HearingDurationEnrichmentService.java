package uk.gov.moj.cpp.listing.common.service;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;
import static uk.gov.justice.listing.commands.UpdateHearingForListing.updateHearingForListing;
import static uk.gov.moj.cpp.listing.common.service.HearingDaysEnrichmentService.isWeekCommencingHearing;
import static uk.gov.moj.cpp.listing.common.util.NonDefaultDayConverter.convertCoreNonDefaultDaysToHearingDays;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.NonDefaultDay;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.courtcentre.HearingTypeFactory;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S6813"})
@ApplicationScoped
public class HearingDurationEnrichmentService implements EnrichmentService {

    public static final int DEFAULT_MIN = 20;
    public static final int MINUTES_IN_DAY = 360; //6 working hours per day

    @Inject
    private HearingTypeFactory hearingTypeFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDaysEnrichmentService.class);


    public HearingListingNeeds enrichWithDurations(HearingListingNeeds hearing, JsonEnvelope envelope) {
        if (!needsEnrichment(hearing)) {
            return hearing;
        }
        Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(envelope);

        return enrichSingleHearing(hearing, hearingTypesIdDurationMap);

    }


    public UpdateHearingForListing enrichWithDurationForUpdate(UpdateHearingForListing updateHearingForListing, JsonEnvelope envelope) {

        if (isWeekCommencingHearing(updateHearingForListing) && isNull(updateHearingForListing.getWeekCommencingDurationInWeeks())) {
            return updateHearingForListing().withValuesFrom(updateHearingForListing).withWeekCommencingDurationInWeeks(1).build();
        }

        if (!needsEnrichmentForUpdate(updateHearingForListing)) {
            return updateHearingForListing;
        }
        Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(envelope);
        return enrichHearingWithDurationForUpdate(updateHearingForListing, hearingTypesIdDurationMap);
    }

    private UpdateHearingForListing enrichHearingWithDurationForUpdate(UpdateHearingForListing updateHearingForListing,
                                                                       Map<String, Integer> hearingTypesIdDurationMap) {

        List<uk.gov.justice.listing.commands.NonDefaultDay> enrichedNonDefaultDays = enrichNonDefaultDaysForUpdate(updateHearingForListing.getNonDefaultDays(), updateHearingForListing.getType(), hearingTypesIdDurationMap);
        List<HearingDay> enrichedHearingDays = enrichHearingDays(updateHearingForListing.getHearingDays(), updateHearingForListing.getType(), hearingTypesIdDurationMap);
        return updateHearingForListing()
                .withValuesFrom(updateHearingForListing)
                .withHearingDays(enrichedHearingDays)
                .withNonDefaultDays(enrichedNonDefaultDays)
                .build();
    }

    private HearingListingNeeds enrichSingleHearing(HearingListingNeeds hearing, Map<String, Integer> hearingTypesIdDurationMap) {

        HearingListingNeeds.Builder builder = hearingListingNeeds().withValuesFrom(hearing);
        enrichBookedSlotsIfPresent(hearing, hearingTypesIdDurationMap, builder);
        enrichNonDefaultDaysIfPresent(hearing, hearingTypesIdDurationMap, builder);
        enrichHearingDaysIfPresent(hearing, hearingTypesIdDurationMap, builder);
        enrichOnlyEstimatedMinutesIfNoSlotsOrNonDefaultDaysAvailable(hearing, hearingTypesIdDurationMap, builder);
        return builder.build();
    }

    private void enrichOnlyEstimatedMinutesIfNoSlotsOrNonDefaultDaysAvailable(HearingListingNeeds hearing, Map<String, Integer> hearingTypesIdDurationMap, HearingListingNeeds.Builder builder) {
        //In some scenarios no nondefaultdays or hearingdays are given, then update only estimatedMinutes
        if (CollectionUtils.isEmpty(hearing.getBookedSlots()) && CollectionUtils.isEmpty(hearing.getNonDefaultDays())) {
            LOGGER.info("Enriching estimated minutes only for hearing id: {}", hearing.getId());
            builder.withEstimatedMinutes(getDurationByHearingTypeOrDefault(hearing.getType(), hearingTypesIdDurationMap));
        }
    }

    private void enrichBookedSlotsIfPresent(HearingListingNeeds hearing,
                                            Map<String, Integer> hearingTypesIdDurationMap,
                                            HearingListingNeeds.Builder builder) {
        if (isNotEmpty(hearing.getBookedSlots())) {
            LOGGER.info("Enriching booked slots for hearing id: {}", hearing.getId());
            List<RotaSlot> enrichedSlots = enrichRotaSlots(
                    hearing.getBookedSlots(),
                    hearing.getType(),
                    hearingTypesIdDurationMap
            );
            int totalDuration = calculateTotalDuration(enrichedSlots, RotaSlot::getDuration);
            builder.withBookedSlots(enrichedSlots)
                    .withEstimatedMinutes(totalDuration);
        }
    }

    private void enrichNonDefaultDaysIfPresent(HearingListingNeeds hearing,
                                               Map<String, Integer> hearingTypesIdDurationMap,
                                               HearingListingNeeds.Builder builder) {
        if (isNotEmpty(hearing.getNonDefaultDays())) {
            LOGGER.info("Enriching nondefaultdays for hearing id: {}", hearing.getId());
            List<NonDefaultDay> enrichedDays = enrichNonDefaultDays(
                    hearing.getNonDefaultDays(),
                    hearing.getType(),
                    hearingTypesIdDurationMap
            );
            int totalDuration = calculateTotalDuration(enrichedDays, NonDefaultDay::getDuration);
            builder.withNonDefaultDays(enrichedDays)
                    .withEstimatedMinutes(totalDuration);
        }
    }

    private void enrichHearingDaysIfPresent(HearingListingNeeds hearing,
                                            Map<String, Integer> hearingTypesIdDurationMap,
                                            HearingListingNeeds.Builder builder) {
        /*
         recently added hearingDays, for now its a copy of Nondefaultdays.
         For MAGS there will be no Nondefaultdays in the future,
         For CROWN we will keep only nondefaultdays coming from UI, hearingDays are actual sitting days.
         Nonsittings days will remain the same, they're only an array of Dates no sitting will happen.
         * */
        //If hearingDays supplied, keep them, otherwisecopy it from NonDefaultDays for backward compability.
        List<HearingDay> actualHearingDays = getActualHearingDaysIfAvailable(hearing);
        LOGGER.info("Enriching hearingdays for hearing id: {}", hearing.getId());
        if (isNotEmpty(actualHearingDays)) {
            List<HearingDay> enrichedHearingDays = enrichHearingDays(
                    actualHearingDays,
                    hearing.getType(),
                    hearingTypesIdDurationMap
            );

            int totalDuration = calculateTotalDuration(enrichedHearingDays, HearingDay::getDurationMinutes);
            builder.withHearingDays(enrichedHearingDays)
                    .withEstimatedMinutes(totalDuration);
        }
    }

    private static List<HearingDay> getActualHearingDaysIfAvailable(final HearingListingNeeds hearing) {
        List<HearingDay> hearingDays = hearing.getHearingDays();
        if (isNotEmpty(hearingDays)) {
            return hearingDays;
        }
        List<NonDefaultDay> nonDefaultDays = hearing.getNonDefaultDays();
        return isNotEmpty(nonDefaultDays)
                ? convertCoreNonDefaultDaysToHearingDays(nonDefaultDays)
                : Collections.emptyList();
    }


    static <T> int calculateTotalDuration(List<T> items, Function<T, Integer> durationExtractor) {
        return items.stream()
                .mapToInt(durationExtractor::apply)
                .sum();
    }

    static boolean hasEmptyOrZeroDurationForNonDefaultDays(NonDefaultDay nonDefaultDay) {

        return isNull(nonDefaultDay.getDuration()) || nonDefaultDay.getDuration() == 0 || nonDefaultDay.getDuration() == 1;
    }

    static boolean hasEmptyOrZeroDurationForNonDefaultDays(uk.gov.justice.listing.commands.NonDefaultDay nonDefaultDay) {

        return isNull(nonDefaultDay.getDuration()) || nonDefaultDay.getDuration() == 0 || nonDefaultDay.getDuration() == 1;
    }

    static boolean hasEmptyOrZeroDurationForHearingDays(HearingDay hearingDay) {
        return isNull(hearingDay.getDurationMinutes()) || hearingDay.getDurationMinutes() == 0 || hearingDay.getDurationMinutes() == 1;
    }

    static boolean hasEmptyOrZeroDuration(RotaSlot rotaSlot) {
        Integer duration = rotaSlot.getDuration();
        return duration == null || duration == 0 || duration == 1;
    }

    static boolean hasInvalidEstimatedMinutes(HearingListingNeeds hearing) {
        return hearing.getEstimatedMinutes() == null || hearing.getEstimatedMinutes() == 0 || hearing.getEstimatedMinutes() == 1;
    }

    static boolean needsEnrichmentForUpdate(UpdateHearingForListing updateHearingForListing) {
        boolean invalidNonDefaultDays = isEmpty(updateHearingForListing.getNonDefaultDays()) ? false : hasCommandNonDefaultDaysWithInvalidDuration(updateHearingForListing.getNonDefaultDays());
        boolean invalidHearingDays = isEmpty(updateHearingForListing.getHearingDays()) ? false :  updateHearingForListing.getHearingDays().stream().anyMatch(HearingDurationEnrichmentService::hasEmptyOrZeroDurationForHearingDays);

        return invalidNonDefaultDays || invalidHearingDays;
    }

    static boolean needsEnrichment(HearingListingNeeds hearing) {
        // If hearing has booked slots, only check if booked slots have invalid durations
        if (isNotEmpty(hearing.getBookedSlots())) {
            return hasBookedSlotsWithInvalidDuration(hearing.getBookedSlots());
        }
        // If no booked slots, check other conditions
        final boolean invalidEstimatedMinutes = hasInvalidEstimatedMinutes(hearing);
        boolean invalidNonDefaultDays = isEmpty(hearing.getNonDefaultDays()) ? false : hasCourtNonDefaultDaysWithInvalidDuration(hearing.getNonDefaultDays());
        boolean invalidHearingDays = isEmpty(hearing.getHearingDays()) ? false : hearing.getHearingDays().stream().anyMatch(HearingDurationEnrichmentService::hasEmptyOrZeroDurationForHearingDays);

        return invalidEstimatedMinutes || invalidNonDefaultDays || invalidHearingDays;
    }

    /**
     * the variety of the same methods with different nondefaultdays coming from input params: for
     * HearingListingNeeds for UpdatehearingForListing  will be addressed with LPT-1090
     */
    static boolean hasCourtNonDefaultDaysWithInvalidDuration(final List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream().anyMatch(HearingDurationEnrichmentService::hasEmptyOrZeroDurationForNonDefaultDays);
    }

    static boolean hasCommandNonDefaultDaysWithInvalidDuration(final List<uk.gov.justice.listing.commands.NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream().anyMatch(HearingDurationEnrichmentService::hasEmptyOrZeroDurationForNonDefaultDays);
    }

    static boolean hasBookedSlotsWithInvalidDuration(final List<RotaSlot> bookedSlots) {
        return bookedSlots.stream().anyMatch(HearingDurationEnrichmentService::hasEmptyOrZeroDuration);
    }


    private HearingListingNeeds enrichHearingWithDuration(HearingListingNeeds hearing,
                                                          Map<String, Integer> hearingTypesIdDurationMap) {
        if (isNotEmpty(hearing.getBookedSlots())) {

        }
        return hearingListingNeeds().withValuesFrom(hearing).withNonDefaultDays(List.of(NonDefaultDay.nonDefaultDay().build())).build();
    }

    private List<NonDefaultDay> enrichNonDefaultDays(List<NonDefaultDay> days,
                                                     HearingType hearingType,
                                                     Map<String, Integer> hearingTypesIdDurationMap) {
        if (CollectionUtils.isEmpty(days)) {//return empty
            return days;
        }
        if (days.size() > 1) {//Multiday
            return days.stream().map(nonDefaultDay -> NonDefaultDay.nonDefaultDay().withValuesFrom(nonDefaultDay).withDuration(MINUTES_IN_DAY).build()).toList();
        }
        final Integer duration = getDurationByHearingTypeOrDefault(hearingType, hearingTypesIdDurationMap);
        return days.stream().map(day -> NonDefaultDay.nonDefaultDay().withValuesFrom(day).withDuration(duration).build()).toList();
    }

    private List<HearingDay> enrichHearingDays(List<HearingDay> days,
                                               HearingType hearingType,
                                               Map<String, Integer> hearingTypesIdDurationMap) {
        if (CollectionUtils.isEmpty(days)) {//return empty
            return days;
        }
        if (days.size() > 1) {//Multiday
            return days.stream().map(hearingDay -> HearingDay.hearingDay().withValuesFrom(hearingDay).withDurationMinutes(MINUTES_IN_DAY).build()).toList();
        }
        final Integer duration = getDurationByHearingTypeOrDefault(hearingType, hearingTypesIdDurationMap);
        return days.stream().map(day -> HearingDay.hearingDay().withValuesFrom(day).withDurationMinutes(duration).build()).toList();
    }

    private List<uk.gov.justice.listing.commands.NonDefaultDay> enrichNonDefaultDaysForUpdate(List<uk.gov.justice.listing.commands.NonDefaultDay> days,
                                                                                              HearingType hearingType,
                                                                                              Map<String, Integer> hearingTypesIdDurationMap) {
        if (CollectionUtils.isEmpty(days)) {//return empty
            return days;
        }
        if (days.size() > 1) {//Multiday
            return days.stream().map(nonDefaultDay -> uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay().withValuesFrom(nonDefaultDay).withDuration(MINUTES_IN_DAY).build()).toList();
        }
        final Integer duration = getDurationByHearingTypeOrDefault(hearingType, hearingTypesIdDurationMap);
        return days.stream().map(day -> uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay().withValuesFrom(day).withDuration(duration).build()).toList();
    }

    private List<RotaSlot> enrichRotaSlots(List<RotaSlot> slots, HearingType hearingType, Map<String, Integer> hearingTypesIdDurationMap) {
        if (CollectionUtils.isEmpty(slots)) {//return empty
            return slots;
        }
        if (slots.size() > 1) {//Multiday
            return slots.stream().map(rotaSlot -> RotaSlot.rotaSlot().withValuesFrom(rotaSlot).withDuration(MINUTES_IN_DAY).build()).toList();
        }
        final Integer slotDuration = getDurationByHearingTypeOrDefault(hearingType, hearingTypesIdDurationMap);
        return slots.stream().map(rotaSlot -> RotaSlot.rotaSlot().withValuesFrom(rotaSlot).withDuration(hasEmptyOrZeroDuration(rotaSlot) ? slotDuration : rotaSlot.getDuration()).build()).toList();
    }

    private uk.gov.justice.listing.commands.NonDefaultDay enrichNonDefaultDayForUpdate(uk.gov.justice.listing.commands.NonDefaultDay day,
                                                                                       UpdateHearingForListing hearing,
                                                                                       Map<String, Integer> hearingTypesIdDurationMap) {
        if (!hasEmptyOrZeroDurationForNonDefaultDays(day)) {
            return day;
        }

        Integer duration = CROWN.equals(hearing.getJurisdictionType()) ? calculateCrownDurationForUpdate(hearing) : MAGISTRATES.equals(hearing.getJurisdictionType()) ? calculateMagsCourtDurationForUpdate(hearing, hearingTypesIdDurationMap) : DEFAULT_MIN;
        return uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withValuesFrom(day)
                .withDuration(duration)
                .build();
    }

    static Integer calculateCrownDuration(List<NonDefaultDay> nonDefaultDays) {
        boolean hasMultipleNonDefaultDays = nonDefaultDays.size() > 1;
        return hasMultipleNonDefaultDays ? MINUTES_IN_DAY : DEFAULT_MIN;
    }

    static Integer getDurationByHearingTypeOrDefault(HearingType hearingType,
                                                     Map<String, Integer> hearingTypesIdDurationMap) {
        if (hearingTypesIdDurationMap.isEmpty() || isNull(hearingType)) {
            return DEFAULT_MIN;
        }

        Integer duration = hearingTypesIdDurationMap.getOrDefault(
                hearingType.getId().toString(),
                DEFAULT_MIN
        );

        return duration == 0 || duration == 1 ? DEFAULT_MIN : duration;
    }

    static Integer calculateCrownDurationForUpdate(UpdateHearingForListing hearing) {
        boolean hasMultipleNonDefaultDays = hearing.getNonDefaultDays().size() > 1;
        return hasMultipleNonDefaultDays ? MINUTES_IN_DAY : DEFAULT_MIN;
    }

    private Integer calculateMagsCourtDurationForUpdate(UpdateHearingForListing hearing,
                                                        Map<String, Integer> hearingTypesIdDurationMap) {
        if (hearingTypesIdDurationMap.isEmpty() || isNull(hearing.getType())) {
            return DEFAULT_MIN;
        }

        return hearingTypesIdDurationMap.getOrDefault(
                hearing.getType().getId().toString(),
                DEFAULT_MIN
        );
    }
}
