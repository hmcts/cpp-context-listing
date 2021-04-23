package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.listing.event.processor.command.SeedingHearingConverter.convertSeedingHearingForCoreDomain;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;


public class HearingConfirmedFactory extends PublicHearingFactory {

    public HearingConfirmed create(final HearingAllocatedForListing hearingAllocated, final JsonEnvelope envelope) {

        final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles = hearingAllocated.getJudiciary();
        final Type type = hearingAllocated.getType();
        return uk.gov.justice.listing.courts.HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildConfirmedHearing(hearingAllocated, judicialRoles, type, envelope))
                .build();


    }

    public HearingConfirmed createV2(final HearingAllocatedForListingV2 hearingAllocated, final JsonEnvelope envelope) {

        final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles = hearingAllocated.getJudiciary();
        final Type type = hearingAllocated.getType();
        return uk.gov.justice.listing.courts.HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildConfirmedHearingV2(hearingAllocated, judicialRoles, type, envelope))
                .build();


    }

    @SuppressWarnings("squid:S3358")
    private ConfirmedHearing buildConfirmedHearing(HearingAllocatedForListing hearingAllocated, List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, Type type, JsonEnvelope envelope) {
        ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingAllocated.getHearingId())
                .withCourtCentre(buildCourtCentre(hearingAllocated.getCourtCentreId(), hearingAllocated.getCourtRoomId(), envelope))
                .withHearingDays(hearingAllocated.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingAllocated.getHearingLanguage().toString()))
                .withJurisdictionType(JurisdictionType.valueFor(hearingAllocated.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
                .withProsecutionCases(isNull(hearingAllocated.getProsecutionCaseDefendantsOffenceIds()) ? null : hearingAllocated.getProsecutionCaseDefendantsOffenceIds().stream()
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
                        .collect(toList()))
                .withReportingRestrictionReason(hearingAllocated.getReportingRestrictionReason())
                .withType(buildType(type));
        if (!judicialRoles.isEmpty()) {
            builder.withJudiciary(judicialRoles.stream()
                    .map(this::buildJudicialRole)
                    .collect(toList()));
        }
        builder.withCourtApplicationIds(hearingAllocated.getCourtApplicationIds());
        return builder.build();
    }

    @SuppressWarnings("squid:S3358")
    private ConfirmedHearing buildConfirmedHearingV2(final HearingAllocatedForListingV2 hearingAllocated, final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles, final Type type, final JsonEnvelope envelope) {
        final ConfirmedHearing.Builder builder = ConfirmedHearing.confirmedHearing()
                .withId(hearingAllocated.getHearingId())
                .withCourtCentre(buildCourtCentre(hearingAllocated.getCourtCentreId(), hearingAllocated.getCourtRoomId(), envelope))
                .withHearingDays(hearingAllocated.getHearingDays().stream()
                        .map(this::buildHearingDay)
                        .collect(toList()))
                .withHearingLanguage(HearingLanguage.valueFor(hearingAllocated.getHearingLanguage().toString()))
                .withJurisdictionType(JurisdictionType.valueFor(hearingAllocated.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new))
                .withProsecutionCases(isNull(hearingAllocated.getProsecutionCaseDefendantsOffenceIds()) ? null : hearingAllocated.getProsecutionCaseDefendantsOffenceIds().stream()
                        .map(pcdo -> ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withDefendants(pcdo.getDefendants().stream()
                                        .map(d -> ConfirmedDefendant.confirmedDefendant()
                                                .withId(d.getId())
                                                .withOffences(d.getOffenceIds().stream()
                                                        .map(o -> ConfirmedOffence.confirmedOffence()
                                                                .withId(o.getId())
                                                                .withSeedingHearing(buildSeedingHearing(o))
                                                                .build())
                                                        .collect(toList()))
                                                .build())
                                        .collect(toList()))
                                .withId(pcdo.getId())
                                .build())
                        .collect(toList()))
                .withReportingRestrictionReason(hearingAllocated.getReportingRestrictionReason())
                .withType(buildType(type));
        if (!judicialRoles.isEmpty()) {
            builder.withJudiciary(judicialRoles.stream()
                    .map(this::buildJudicialRole)
                    .collect(toList()));
        }
        builder.withCourtApplicationIds(hearingAllocated.getCourtApplicationIds());
        return builder.build();
    }

    @SuppressWarnings("squid:S3655")
    private Optional<SeedingHearing> buildSeedingHearing(final OffenceIds offenceIds) {
        return offenceIds.getSeedingHearing().isPresent() ?
                convertSeedingHearingForCoreDomain(offenceIds.getSeedingHearing().get()) : empty();
    }

}
