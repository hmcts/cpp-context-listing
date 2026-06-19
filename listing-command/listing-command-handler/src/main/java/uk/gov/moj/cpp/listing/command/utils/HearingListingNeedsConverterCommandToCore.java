package uk.gov.moj.cpp.listing.command.utils;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.services.common.converter.Converter;

@SuppressWarnings({"pmd:NullAssignment", "squid:S2583", "squid:S1172", "squid:CommentedOutCodeLine", "pmd:NullAssignment", "squid:MethodCyclomaticComplexity", "squid:S3655", "squid:S1067"})
public class HearingListingNeedsConverterCommandToCore implements Converter<HearingListingNeeds, uk.gov.justice.listing.commands.HearingListingNeeds> {

    @SuppressWarnings({"squid:S3655"})
    @Override
    public uk.gov.justice.listing.commands.HearingListingNeeds convert(final HearingListingNeeds commandHearing) {
        uk.gov.justice.listing.commands.HearingListingNeeds.Builder builder = uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds()
                .withId(commandHearing.getId())
                .withType(commandHearing.getType())
                .withJurisdictionType(commandHearing.getJurisdictionType())
                .withCourtCentre(commandHearing.getCourtCentre())
                .withEstimatedMinutes(commandHearing.getEstimatedMinutes());

        if (commandHearing.getListedStartDateTime() != null) {
            builder.withListedStartDateTime(commandHearing.getListedStartDateTime());
        }
        if (commandHearing.getEarliestStartDateTime() != null) {
            builder.withEarliestStartDateTime(commandHearing.getEarliestStartDateTime());
        }
        if (commandHearing.getEndDate() != null) {
            builder.withEndDate(commandHearing.getEndDate());
        }
        if (commandHearing.getEstimatedDuration() != null) {
            builder.withEstimatedDuration(commandHearing.getEstimatedDuration());
        }
        if (commandHearing.getProsecutorDatesToAvoid() != null) {
            builder.withProsecutorDatesToAvoid(commandHearing.getProsecutorDatesToAvoid());
        }
        if (commandHearing.getListingDirections() != null) {
            builder.withListingDirections(commandHearing.getListingDirections());
        }
        if (commandHearing.getReportingRestrictionReason() != null) {
            builder.withReportingRestrictionReason(commandHearing.getReportingRestrictionReason());
        }
        if (commandHearing.getJudiciary() != null) {
            builder.withJudiciary(commandHearing.getJudiciary());
        }
        if (commandHearing.getProsecutionCases() != null) {
            builder.withProsecutionCases(commandHearing.getProsecutionCases());
        }
        if (commandHearing.getCourtApplications() != null) {
            builder.withCourtApplications(commandHearing.getCourtApplications());
        }
        if (commandHearing.getDefendantListingNeeds() != null) {
            builder.withDefendantListingNeeds(commandHearing.getDefendantListingNeeds());
        }
        if (commandHearing.getCourtApplicationPartyListingNeeds() != null) {
            builder.withCourtApplicationPartyListingNeeds(commandHearing.getCourtApplicationPartyListingNeeds());
        }
        if (commandHearing.getBookingReference() != null) {
            builder.withBookingReference(commandHearing.getBookingReference());
        }

        if (isNotEmpty(commandHearing.getHearingDays())) {
            builder.withHearingDays(commandHearing.getHearingDays());
        }

        if (isNotEmpty(commandHearing.getNonDefaultDays())) {
            builder.withNonDefaultDays(commandHearing.getNonDefaultDays());
        }

        if (isNotEmpty(commandHearing.getNonSittingDays())) {
            builder.withNonSittingDays(commandHearing.getNonSittingDays());
        }


        if (commandHearing.getWeekCommencingDate() != null) {
            builder.withWeekCommencingDate(commandHearing.getWeekCommencingDate());
        }
        if (commandHearing.getBookedSlots() != null) {
            builder.withBookedSlots(commandHearing.getBookedSlots());
        }
        if (commandHearing.getBookingType() != null) {
            builder.withBookingType(commandHearing.getBookingType());
        }
        if (commandHearing.getPriority() != null) {
            builder.withPriority(commandHearing.getPriority());
        }
        if (commandHearing.getSpecialRequirements() != null) {
            builder.withSpecialRequirements(commandHearing.getSpecialRequirements());
        }
        if (commandHearing.getIsGroupProceedings() != null) {
            builder.withIsGroupProceedings(commandHearing.getIsGroupProceedings());
        }
        if (commandHearing.getNumberOfGroupCases() != null) {
            builder.withNumberOfGroupCases(commandHearing.getNumberOfGroupCases());
        }
        if (commandHearing.getNonDefaultDays() != null) {
            builder.withNonDefaultDays(commandHearing.getNonDefaultDays());
        }

        return builder.build();
    }
}
