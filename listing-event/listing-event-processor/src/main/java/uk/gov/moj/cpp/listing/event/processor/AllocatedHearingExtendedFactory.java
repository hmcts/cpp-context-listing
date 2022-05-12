package uk.gov.moj.cpp.listing.event.processor;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2;
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

    public HearingConfirmed create(final AllocatedHearingExtendedForListingV2 hearingExtendedForListingV2, JsonEnvelope envelope) {

        final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles = hearingExtendedForListingV2.getJudiciary();
        final Type type = hearingExtendedForListingV2.getType();
        return uk.gov.justice.listing.courts.HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildConfirmedHearing(hearingExtendedForListingV2, judicialRoles, type, envelope))
                .build();
    }

    private ConfirmedHearing buildConfirmedHearing(AllocatedHearingExtendedForListingV2 hearingExtendedForListingV2, List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, Type type, JsonEnvelope envelope) {
        final ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingExtendedForListingV2.getExistingHearingId())
                .withCourtCentre(buildCourtCentre(hearingExtendedForListingV2.getCourtCentreId(), hearingExtendedForListingV2.getCourtRoomId(), envelope))
                .withHearingDays(hearingExtendedForListingV2.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingExtendedForListingV2.getHearingLanguage().toString()))
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueFor(hearingExtendedForListingV2.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
                .withCourtApplicationIds(hearingExtendedForListingV2.getCourtApplicationIds())
                .withReportingRestrictionReason(hearingExtendedForListingV2.getReportingRestrictionReason())
                .withType(buildType(type));
        if (hearingExtendedForListingV2.getProsecutionCaseDefendantsOffenceIds() != null) {
            builder.withProsecutionCases(hearingExtendedForListingV2.getProsecutionCaseDefendantsOffenceIds().stream()
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
        if(Objects.nonNull(hearingExtendedForListingV2.getExistingHearingId())){
            builder.withExistingHearingId(Optional.of(hearingExtendedForListingV2.getHearingId()));
        }
        builder.withFullExtension(hearingExtendedForListingV2.getFullExtension());
        return builder.build();
    }

    private ConfirmedHearing buildConfirmedHearing(AllocatedHearingExtendedForListing hearingExtendedForListing, List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, Type type, JsonEnvelope envelope) {
        ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingExtendedForListing.getExistingHearingId())
                .withCourtCentre(buildCourtCentre(hearingExtendedForListing.getCourtCentreId(), hearingExtendedForListing.getCourtRoomId(), envelope))
                .withHearingDays(hearingExtendedForListing.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingExtendedForListing.getHearingLanguage().toString()))
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueFor(hearingExtendedForListing.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
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
