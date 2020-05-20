package uk.gov.moj.cpp.listing.event.processor;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;


public class AllocatedHearingExtendedFactory extends PublicHearingFactory {

    public HearingConfirmed create(final AllocatedHearingExtendedForListing hearingExtendedForListing, JsonEnvelope envelope) {

        final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles = hearingExtendedForListing.getJudiciary();
        final Type type = hearingExtendedForListing.getType();
        return uk.gov.justice.listing.courts.HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildConfirmedHearing(hearingExtendedForListing, judicialRoles, type, envelope))
                .build();
    }

    private ConfirmedHearing buildConfirmedHearing(AllocatedHearingExtendedForListing hearingExtendedForListing, List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, Type type, JsonEnvelope envelope) {
        ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingExtendedForListing.getExistingHearingId())
                .withCourtCentre(buildCourtCentre(hearingExtendedForListing.getCourtCentreId(), hearingExtendedForListing.getCourtRoomId(), envelope))
                .withHearingDays(hearingExtendedForListing.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingExtendedForListing.getHearingLanguage().toString()))
                .withJurisdictionType(JurisdictionType.valueFor(hearingExtendedForListing.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
                .withCourtApplicationIds(hearingExtendedForListing.getCourtApplicationIds())
                .withReportingRestrictionReason(hearingExtendedForListing.getReportingRestrictionReason())
                .withType(buildType(type));
        if (hearingExtendedForListing.getProsecutionCaseDefendantsOffenceIds() != null) {
            builder.withProsecutionCases(hearingExtendedForListing.getProsecutionCaseDefendantsOffenceIds().stream()
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
                    .collect(toList()));
        }

        if (!judicialRoles.isEmpty()) {
            builder.withJudiciary(judicialRoles.stream()
                    .map(this::buildJudicialRole)
                    .collect(toList()));
        }
        if(Objects.nonNull(hearingExtendedForListing.getExistingHearingId())){
            builder.withExistingHearingId(Optional.of(hearingExtendedForListing.getHearingId()));
        }
        return builder.build();
    }
}
