package uk.gov.moj.cpp.listing.event.processor;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.Type;

import java.util.List;


public class HearingConfirmedFactory extends PublicHearingFactory {

    public HearingConfirmed create(final HearingAllocatedForListing hearingAllocated) {

        final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles = hearingAllocated.getJudiciary();
        final Type type = hearingAllocated.getType();
        return uk.gov.justice.listing.courts.HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildConfirmedHearing(hearingAllocated, judicialRoles, type))
                .build();


    }

    private ConfirmedHearing buildConfirmedHearing(HearingAllocatedForListing hearingAllocated, List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, Type type) {
        ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingAllocated.getHearingId())
                .withCourtCentre(buildCourtCentre(hearingAllocated.getCourtCentreId(), hearingAllocated.getCourtRoomId()))
                .withHearingDays(hearingAllocated.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingAllocated.getHearingLanguage().toString()))
                .withJurisdictionType(JurisdictionType.valueFor(hearingAllocated.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
                .withProsecutionCases(hearingAllocated.getProsecutionCaseDefendantsOffenceIds().stream()
                        .map(pcdo -> ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withDefendants(pcdo.getDefendants().stream()
                                        .map(d -> ConfirmedDefendant.confirmedDefendant()
                                                .withId(d.getId())
                                                .withOffences(d.getOffenceIds().stream()
                                                        .map(o -> ConfirmedOffence.confirmedOffence().withId(o).build())
                                                        .collect(toList()))
                                                .build())
                                        .collect(toList()))
                                .withId(pcdo.getId())
                                .build())
                        .collect(toList()))
                .withReportingRestrictionReason(hearingAllocated.getReportingRestrictionReason())
                .withType(buildType(type));
        if(!judicialRoles.isEmpty()){
            builder.withJudiciary(judicialRoles.stream()
                    .map(this::buildJudicialRole)
                    .collect(toList()));
        }
        return builder.build();
    }

}
