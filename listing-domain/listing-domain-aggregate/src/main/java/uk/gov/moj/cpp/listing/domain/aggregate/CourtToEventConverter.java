package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.LaaReference;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Prosecutor;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.aggregate.converter.ReportingRestrictionConverter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CourtToEventConverter {

    private CourtToEventConverter() {
    }

    public static ListedCase buildListedCase(final ProsecutionCase pc, final List<UUID> shadowListedOffences) {
        final List<Defendant> defendants = pc.getDefendants().stream()
                .map(defendant -> buildDefendant(defendant, shadowListedOffences))
                .collect(toList());

        final boolean caseShadowListed = defendants.stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .allMatch(offence -> offence.getShadowListed().orElse(Boolean.FALSE));

        return ListedCase.listedCase()
                .withId(pc.getId())
                .withCaseIdentifier(buildCaseIdentifier(pc))
                .withProsecutor(buildProsecutor(pc.getProsecutor()))
                .withMarkers(isNull(pc.getCaseMarkers()) ? emptyList() : pc.getCaseMarkers().stream()
                        .map(CourtToEventConverter::convertCaseMarkersToMarkers)
                        .collect(toList()))
                .withDefendants(defendants)
                .withRestrictFromCourtList(of(Boolean.FALSE))
                .withCaseStatus(pc.getCaseStatus())
                .withShadowListed(of(caseShadowListed))
                .build();
    }

    private static CaseIdentifier buildCaseIdentifier(final ProsecutionCase pc) {
        return CaseIdentifier.caseIdentifier()
                .withAuthorityCode(pc.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                .withAuthorityId(pc.getProsecutionCaseIdentifier().getProsecutionAuthorityId())
                .withCaseReference(ofNullable(pc.getProsecutionCaseIdentifier().getCaseURN()).orElse(pc.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()))
                .build();
    }

    private static Prosecutor buildProsecutor(final Optional<uk.gov.justice.core.courts.Prosecutor> prosecutor) {
        if (prosecutor.isPresent()) {
            return Prosecutor.prosecutor()
                    .withProsecutorCode(prosecutor.get().getProsecutorCode())
                    .withAddress(buildAddress(prosecutor.get().getAddress()))
                    .withProsecutorId(prosecutor.get().getProsecutorId())
                    .withProsecutorName(prosecutor.get().getProsecutorName())
                    .build();
        } else {
            return null;
        }
    }

    private static uk.gov.justice.listing.events.Address buildAddress(final Optional<Address> address) {
        if (address.isPresent()) {
            return uk.gov.justice.listing.events.Address.address()
                    .withAddress1(address.get().getAddress1())
                    .withAddress2(address.get().getAddress2())
                    .withAddress3(address.get().getAddress3())
                    .withAddress4(address.get().getAddress4())
                    .withAddress5(address.get().getAddress5())
                    .withPostcode(address.get().getPostcode())
                    .withWelshAddress1(address.get().getWelshAddress1())
                    .withWelshAddress2(address.get().getWelshAddress2())
                    .withWelshAddress3(address.get().getWelshAddress3())
                    .withWelshAddress4(address.get().getWelshAddress4())
                    .withWelshAddress5(address.get().getWelshAddress5())
                    .build();
        } else {
            return null;
        }
    }


    private static Marker convertCaseMarkersToMarkers(final uk.gov.justice.core.courts.Marker caseMarker) {
        return Marker.marker().withId(caseMarker.getId())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .withMarkerTypeid(caseMarker.getMarkerTypeid()).build();
    }

    private static Defendant buildDefendant(final uk.gov.justice.core.courts.Defendant d, final List<UUID> shadowListedOffences) {
        final Person personDetail = getPersonDetail(d);
        return Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(ofNullable(d.getMasterDefendantId()))
                .withCourtProceedingsInitiated(ofNullable(d.getCourtProceedingsInitiated()))
                .withDateOfBirth(getDateOfBirth(personDetail))
                .withFirstName(getFirstName(personDetail))
                .withLastName(getLastName(personDetail))
                .withOrganisationName(getOrganisationName(d.getDefenceOrganisation()))
                .withOffences(d.getOffences().stream()
                        .map(offence -> buildOffence(offence, shadowListedOffences))
                        .collect(toList()))
                .withDefenceOrganisation(getDefenceOrganisationName(d.getAssociatedDefenceOrganisation()))
                .withBailStatus(getBailStatus(d))
                .withRestrictFromCourtList(of(Boolean.FALSE))
                .withIsYouth(d.getIsYouth())
                .withAddress(nonNull(personDetail) ? personDetail.getAddress() : empty())
                .build();
    }

    private static Optional<String> getDateOfBirth(final Person personDetail) {
        return nonNull(personDetail) ? personDetail.getDateOfBirth() : empty();
    }

    private static Optional<String> getFirstName(final Person personDetail) {
        return nonNull(personDetail) ? personDetail.getFirstName() : empty();
    }

    private static Optional<String> getLastName(final Person personDetail) {
        return nonNull(personDetail) ? of(personDetail.getLastName()) : empty();
    }

    @SuppressWarnings({"squid:S3655"})
    private static Optional<BailStatus> getBailStatus(final uk.gov.justice.core.courts.Defendant d) {
        return nonNull(d.getPersonDefendant()) && d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getBailStatus() : empty();
    }


    private static Optional<String> getOrganisationName(final Optional<Organisation> organisation) {
        return nonNull(organisation) && organisation.isPresent() ? ofNullable(organisation.get().getName()) : empty();

    }

    private static Optional<String> getDefenceOrganisationName(final Optional<AssociatedDefenceOrganisation> organisation) {
        if (nonNull(organisation) && organisation.isPresent() && nonNull(organisation.get().getDefenceOrganisation()) && nonNull(organisation.get().getDefenceOrganisation().getOrganisation())) {
            return ofNullable(organisation.get().getDefenceOrganisation().getOrganisation().getName());
        }
        return empty();

    }

    @SuppressWarnings({"squid:S3655"})
    private static Offence buildOffence(final uk.gov.justice.core.courts.Offence o, final List<UUID> shadowListedOffences) {
        final Offence.Builder builder = Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withRestrictFromCourtList(of(Boolean.FALSE))
                .withLaaApplnReference(buildLaaReference(o.getLaaApplnReference()))
                .withLaidDate(o.getLaidDate())
                .withSeedingHearing(o.getSeedingHearing().isPresent() ? buildSeedingHearing(o.getSeedingHearing().get()) : null)
                .withShadowListed(of(isNotEmpty(shadowListedOffences) && shadowListedOffences.contains(o.getId())));

        if (!isNull(o.getReportingRestrictions()) && !o.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(o.getReportingRestrictions().stream()
                    .map(ReportingRestrictionConverter::courtsToEvents)
                    .collect(toList())
            );
        }

        return builder.build();
    }

    @SuppressWarnings({"squid:S3655"})
    private static Person getPersonDetail(final uk.gov.justice.core.courts.Defendant d) {
        if (d.getPersonDefendant().isPresent()) {
            return d.getPersonDefendant().get().getPersonDetails();
        }
        return null;
    }

    private static StatementOfOffence buildStatementOfOffence(final uk.gov.justice.core.courts.Offence o) {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(o.getOffenceLegislation())
                .withTitle(o.getOffenceTitle())
                .withWelshLegislation(o.getOffenceLegislationWelsh())
                .withWelshTitle(o.getOffenceTitleWelsh().orElse(""))
                .build();
    }

    private static Optional<LaaReference> buildLaaReference(final Optional<uk.gov.justice.core.courts.LaaReference> laaReference) {
        if (laaReference.isPresent()) {
            return of(LaaReference.laaReference()
                    .withApplicationReference(laaReference.get().getApplicationReference())
                    .withEffectiveEndDate(laaReference.get().getEffectiveEndDate())
                    .withEffectiveStartDate(laaReference.get().getEffectiveStartDate())
                    .withStatusCode(laaReference.get().getStatusCode())
                    .withStatusDescription(laaReference.get().getStatusDescription())
                    .withStatusDate(laaReference.get().getStatusDate())
                    .withStatusId(laaReference.get().getStatusId())
                    .build());
        }
        return empty();
    }

    private static SeedingHearing buildSeedingHearing(final uk.gov.justice.core.courts.SeedingHearing seedingHearing) {
        return SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                .withJurisdictionType(JurisdictionType.valueOf(seedingHearing.getJurisdictionType().toString()))
                .withSittingDay(seedingHearing.getSittingDay())
                .build();
    }

}
