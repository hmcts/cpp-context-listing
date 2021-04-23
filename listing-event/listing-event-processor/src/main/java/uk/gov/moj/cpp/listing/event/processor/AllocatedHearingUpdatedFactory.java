package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.command.SeedingHearingConverter;

import java.util.List;

public class AllocatedHearingUpdatedFactory extends PublicHearingFactory {

    public HearingUpdated create(final AllocatedHearingUpdatedForListing hearingUpdatedForListing, JsonEnvelope envelope) {

        final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles = hearingUpdatedForListing.getJudiciary();
        final Type type = hearingUpdatedForListing.getType();
        return uk.gov.justice.listing.courts.HearingUpdated.hearingUpdated()
                .withUpdatedHearing(buildConfirmedHearing(hearingUpdatedForListing, judicialRoles, type, envelope))
                .build();


    }

    public HearingUpdated createV2(final AllocatedHearingUpdatedForListingV2 hearingUpdatedForListing, final JsonEnvelope envelope) {

        final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles = hearingUpdatedForListing.getJudiciary();
        final Type type = hearingUpdatedForListing.getType();
        return uk.gov.justice.listing.courts.HearingUpdated.hearingUpdated()
                .withUpdatedHearing(buildConfirmedHearingV2(hearingUpdatedForListing, judicialRoles, type, envelope))
                .build();


    }

    private ConfirmedHearing buildConfirmedHearingV2(final AllocatedHearingUpdatedForListingV2 hearingUpdatedForListing, final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, final Type type, final JsonEnvelope envelope) {
        final ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingUpdatedForListing.getHearingId())
                .withCourtCentre(buildCourtCentre(hearingUpdatedForListing.getCourtCentreId(), hearingUpdatedForListing.getCourtRoomId(), envelope))
                .withHearingDays(hearingUpdatedForListing.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingUpdatedForListing.getHearingLanguage().toString()))
                .withJurisdictionType(JurisdictionType.valueFor(hearingUpdatedForListing.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
                .withCourtApplicationIds(hearingUpdatedForListing.getCourtApplicationIds())
                .withReportingRestrictionReason(hearingUpdatedForListing.getReportingRestrictionReason())
                .withType(buildType(type));
        if (hearingUpdatedForListing.getProsecutionCaseDefendantsOffenceIds() != null) {
            builder.withProsecutionCases(hearingUpdatedForListing.getProsecutionCaseDefendantsOffenceIds().stream()
                    .map(pcdo -> ConfirmedProsecutionCase.confirmedProsecutionCase()
                            .withDefendants(pcdo.getDefendants().stream()
                                    .map(d -> ConfirmedDefendant.confirmedDefendant()
                                            .withId(d.getId())
                                            .withOffences(d.getOffenceIds().stream()
                                                    .map(o -> ConfirmedOffence.confirmedOffence()
                                                            .withId(o.getId())
                                                            .withSeedingHearing(o.getSeedingHearing().isPresent() ?
                                                                    SeedingHearingConverter.convertSeedingHearingForCoreDomain(o.getSeedingHearing().get()) : empty())
                                                            .build())
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
        return builder.build();
    }

    private ConfirmedHearing buildConfirmedHearing(AllocatedHearingUpdatedForListing hearingUpdatedForListing, List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, Type type, JsonEnvelope envelope) {
        ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingUpdatedForListing.getHearingId())
                .withCourtCentre(buildCourtCentre(hearingUpdatedForListing.getCourtCentreId(), hearingUpdatedForListing.getCourtRoomId(), envelope))
                .withHearingDays(hearingUpdatedForListing.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingUpdatedForListing.getHearingLanguage().toString()))
                .withJurisdictionType(JurisdictionType.valueFor(hearingUpdatedForListing.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
                .withCourtApplicationIds(hearingUpdatedForListing.getCourtApplicationIds())
                .withReportingRestrictionReason(hearingUpdatedForListing.getReportingRestrictionReason())
                .withType(buildType(type));
        if (hearingUpdatedForListing.getProsecutionCaseDefendantsOffenceIds() != null) {
            builder.withProsecutionCases(hearingUpdatedForListing.getProsecutionCaseDefendantsOffenceIds().stream()
                    .map(pcdo -> ConfirmedProsecutionCase.confirmedProsecutionCase()
                            .withDefendants(pcdo.getDefendants().stream()
                                    .map(d -> ConfirmedDefendant.confirmedDefendant()
                                            .withId(d.getId())
                                            .withOffences(d.getOffenceIds().stream()
                                                    .map(o -> ConfirmedOffence.confirmedOffence()
                                                            .withId(o)
                                                            .build())
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
        return builder.build();
    }

}
