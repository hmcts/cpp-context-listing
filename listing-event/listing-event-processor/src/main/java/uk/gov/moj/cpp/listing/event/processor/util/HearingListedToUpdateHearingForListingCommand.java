package uk.gov.moj.cpp.listing.event.processor.util;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.services.common.converter.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class HearingListedToUpdateHearingForListingCommand implements Converter<Hearing, UpdateHearingForListingEnriched> {

    @Override
    public UpdateHearingForListingEnriched convert(final Hearing hearing) {

        return UpdateHearingForListingEnriched.updateHearingForListingEnriched()
                .withUpdateHearingForListing(UpdateHearingForListing.updateHearingForListing()
                        .withCourtCentreId(hearing.getCourtCentreId())
                        .withCourtRoomId(hearing.getCourtRoomId())
                        .withEndDate(hearing.getEndDate())
                        .withHearingId(hearing.getId())
                        .withHearingLanguage(getHearingLanguage(hearing))
                        .withJudiciary(convertJudicialRoles(hearing))
                        .withJurisdictionType(JurisdictionType.valueOf(hearing.getJurisdictionType().name()))
                        .withNonDefaultDays(convertNonDefaultDays(hearing))
                        .withNonSittingDays(hearing.getNonSittingDays())
                        .withStartDate(hearing.getStartDate())
                        .withType(HearingType.hearingType()
                                .withId(hearing.getType().getId())
                                .withDescription(hearing.getType().getDescription())
                                .build())
                        .withWeekCommencingDurationInWeeks(hearing.getWeekCommencingDurationInWeeks())
                        .withWeekCommencingEndDate(hearing.getWeekCommencingEndDate())
                        .withWeekCommencingStartDate(hearing.getWeekCommencingStartDate())
                        .build())
                .withCourtCentreDetails(convertCourtCentreDetails(hearing))
                .build();
    }

    private HearingLanguage getHearingLanguage(final Hearing hearing) {
        final String language = nonNull(hearing.getHearingLanguage()) ? hearing.getHearingLanguage().name() : null;
        final Optional<HearingLanguage> hearingLanguage = isNotEmpty(language) ? HearingLanguage.valueFor(language) : Optional.empty();
        return hearingLanguage.isPresent() ? hearingLanguage.get() : null;
    }


    private CourtCentreDetails convertCourtCentreDetails(final Hearing hearing) {
        final Optional<uk.gov.justice.listing.events.CourtCentreDetails> courtCentreDetails = hearing.getCourtCentreDetails();

        return courtCentreDetails.isPresent() ? CourtCentreDetails.courtCentreDetails()
                .withDefaultStartTime(courtCentreDetails.get().getDefaultStartTime())
                .withId(courtCentreDetails.get().getId())
                .withDefaultDuration(courtCentreDetails.get().getDefaultDuration())
                .build() : null;
    }

    private List<NonDefaultDay> convertNonDefaultDays(final Hearing hearing) {
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        hearing.getNonDefaultDays().forEach(n -> nonDefaultDays.add(NonDefaultDay.nonDefaultDay()
                .withCourtRoomId(n.getCourtRoomId())
                .withCourtScheduleId(n.getCourtScheduleId())
                .withDuration(n.getDuration())
                .withOucode(n.getOucode())
                .withSession(n.getSession())
                .withStartTime(n.getStartTime())
                .build()));
        return nonDefaultDays;
    }

    @SuppressWarnings({"squid:S1168"})
    private List<JudicialRole> convertJudicialRoles(final Hearing hearing) {
        if (Objects.isNull(hearing.getJudiciary())) {
            return null;
        }

        final List<JudicialRole> judiciaryList = new ArrayList<>();
        hearing.getJudiciary().forEach(j -> judiciaryList.add(JudicialRole.judicialRole()
                .withIsBenchChairman(j.getIsBenchChairman())
                .withIsDeputy(j.getIsDeputy())
                .withJudicialId(j.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudicialRoleTypeId(j.getJudicialRoleType().getJudicialRoleTypeId())
                        .withJudiciaryType(j.getJudicialRoleType().getJudiciaryType())
                        .build())
                .build()));
        return judiciaryList;
    }
}
