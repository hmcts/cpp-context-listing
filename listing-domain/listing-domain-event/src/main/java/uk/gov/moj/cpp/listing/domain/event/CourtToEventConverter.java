package uk.gov.moj.cpp.listing.domain.event;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CommittingCourt;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.LaaReference;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Prosecutor;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.ReportingRestriction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("pmd:NullAssignment")
public class CourtToEventConverter {

    private CourtToEventConverter() {
    }

    public static uk.gov.justice.listing.events.Defendant buildDefendantFromCoreDefendant(final uk.gov.justice.core.courts.Defendant d, final List<UUID> shadowListedOffences, final ListedCase listedCase) {
        final Person personDetail = getPersonDetail(d);

        return uk.gov.justice.listing.events.Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(d.getMasterDefendantId())
                .withCourtProceedingsInitiated(d.getCourtProceedingsInitiated())
                .withDateOfBirth(getDateOfBirth(personDetail))
                .withFirstName(getFirstName(personDetail))
                .withLastName(getLastName(personDetail))
                .withOrganisationName(getOrganisationName(d.getDefenceOrganisation()))
                .withOffences(d.getOffences().stream()
                        .map(offence -> buildOffence(offence, shadowListedOffences, listedCase))
                        .collect(toList()))
                .withDefenceOrganisation(getDefenceOrganisationName(ofNullable(d.getAssociatedDefenceOrganisation())))
                .withBailStatus(getBailStatus(d))
                .withRestrictFromCourtList(listedCase.getRestrictFromCourtList())
                .withIsYouth(d.getIsYouth())
                .withAddress(nonNull(personDetail) ? personDetail.getAddress() : null)
                .build();
    }

    @SuppressWarnings({"squid:S3655", "squid:S1067"})
    public static uk.gov.justice.listing.events.Defendant buildDefendantFromDomainDefendant(final uk.gov.moj.cpp.listing.domain.Defendant d) {
        final Optional<HearingLanguageNeeds> hearingLanguageNeeds = d.getHearingLanguageNeeds();
        return uk.gov.justice.listing.events.Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(d.getMasterDefendantId().orElse(null))
                .withCourtProceedingsInitiated(d.getCourtProceedingsInitiated().orElse(null))
                .withCustodyTimeLimit(d.getCustodyTimeLimit().orElse(null))
                .withDateOfBirth(d.getDateOfBirth().orElse(null))
                .withFirstName(d.getFirstName().orElse(null))
                .withLastName(d.getLastName().orElse(null))
                .withDatesToAvoid(d.getDatesToAvoid().orElse(null))
                .withHearingLanguageNeeds(nonNull(hearingLanguageNeeds) && hearingLanguageNeeds.isPresent()
                        ? uk.gov.justice.core.courts.HearingLanguage.valueFor(hearingLanguageNeeds.get().toString()).get()
                        : null)
                .withOrganisationName(d.getOrganisationName().orElse(null))
                .withSpecificRequirements(d.getSpecificRequirements().orElse(null))
                .withOffences(d.getOffences().stream()
                        .map(CourtToEventConverter::buildOffence)
                        .collect(toList()))
                .withDefenceOrganisation(d.getDefenceOrganisation().orElse(null))
                .withBailStatus(buildBailStatusEvent(d.getBailStatus()))
                .withRestrictFromCourtList(false)
                .withIsYouth(d.getIsYouth().orElse(null))
                .withAddress(nonNull(d.getAddress()) && d.getAddress().isPresent() ? buildAddress(d.getAddress().get()) : null)
                .withNationalityDescription(d.getNationalityDescription().orElse(null))
                .build();
    }

    public static ListedCase buildListedCase(final ProsecutionCase pc, final List<UUID> shadowListedOffences) {
        final List<Defendant> defendants = pc.getDefendants().stream()
                .map(defendant -> buildDefendant(defendant, shadowListedOffences))
                .collect(toList());

        final boolean caseShadowListed = defendants.stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .allMatch(Offence::getShadowListed);

        return ListedCase.listedCase()
                .withId(pc.getId())
                .withCaseIdentifier(buildCaseIdentifier(pc))
                .withProsecutor(buildProsecutor(ofNullable(pc.getProsecutor())))
                .withMarkers(isNull(pc.getCaseMarkers()) ? emptyList() : pc.getCaseMarkers().stream()
                        .map(CourtToEventConverter::convertCaseMarkersToMarkers)
                        .collect(toList()))
                .withDefendants(defendants)
                .withRestrictFromCourtList(false)
                .withCaseStatus(pc.getCaseStatus())
                .withShadowListed(caseShadowListed)
                .build();
    }

    @SuppressWarnings({"squid:S3655"})
    private static Offence buildOffence(final uk.gov.moj.cpp.listing.domain.Offence o) {
        final Offence.Builder builder = Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate().orElse(null))
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withOffenceWording(o.getOffenceWording())
                .withRestrictFromCourtList(false)
                .withLaaApplnReference(getLaaApplnReference(o))
                .withLaidDate(o.getLaidDate().orElse(null))
                .withSeedingHearing(nonNull(o.getSeedingHearing()) && o.getSeedingHearing().isPresent() ? buildSeedingHearing(o.getSeedingHearing().get()) : null)
                .withShadowListed(o.getShadowListed().orElse(null));

        if (nonNull(o.getCommittingCourt()) && o.getCommittingCourt().isPresent()) {
            builder.withCommittingCourt(buildCommittingCourt(o.getCommittingCourt().get()));
        }

        if (!isNull(o.getReportingRestrictions()) && !o.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(o.getReportingRestrictions().stream()
                    .map(CourtToEventConverter::domainToEvents)
                    .collect(toList())
            );
        }

        return builder.build();
    }

    private static uk.gov.justice.core.courts.BailStatus buildBailStatusEvent(final Optional<uk.gov.moj.cpp.listing.domain.BailStatus> bs) {
        if (nonNull(bs) && bs.isPresent()) {
            return uk.gov.justice.core.courts.BailStatus.bailStatus()
                    .withCode(bs.get().getCode())
                    .withDescription(bs.get().getDescription())
                    .withId(bs.get().getId())
                    .build();
        }
        return null;
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
                    .withAddress(buildAddress(ofNullable(prosecutor.get().getAddress())))
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

    private static uk.gov.justice.core.courts.Address buildAddress(final uk.gov.moj.cpp.listing.domain.Address address) {
        if (nonNull(address)) {
            return uk.gov.justice.core.courts.Address.address()
                    .withAddress1(ofNullable(address.getAddress1()).orElse(""))
                    .withAddress2(address.getAddress2().orElse(null))
                    .withAddress3(address.getAddress3().orElse(null))
                    .withAddress4(address.getAddress4().orElse(null))
                    .withAddress5(address.getAddress5().orElse(null))
                    .withPostcode(address.getPostcode().orElse(null))
                    .build();
        }
        return null;
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
                .withMasterDefendantId(d.getMasterDefendantId())
                .withCourtProceedingsInitiated(d.getCourtProceedingsInitiated())
                .withDateOfBirth(getDateOfBirth(personDetail))
                .withFirstName(getFirstName(personDetail))
                .withLastName(getLastName(personDetail))
                .withOrganisationName(getOrganisationName(d.getDefenceOrganisation()))
                .withOffences(d.getOffences().stream()
                        .map(offence -> buildOffence(offence, shadowListedOffences))
                        .collect(toList()))
                .withDefenceOrganisation(getDefenceOrganisationName(ofNullable(d.getAssociatedDefenceOrganisation())))
                .withBailStatus(getBailStatus(d))
                .withRestrictFromCourtList(false)
                .withIsYouth(d.getIsYouth())
                .withAddress(nonNull(personDetail) ? personDetail.getAddress() : null)
                .build();
    }

    private static String getDateOfBirth(final Person personDetail) {
        return nonNull(personDetail) ? personDetail.getDateOfBirth() : null;
    }

    private static String getFirstName(final Person personDetail) {
        return nonNull(personDetail) ? personDetail.getFirstName() : null;
    }

    private static String getLastName(final Person personDetail) {
        return nonNull(personDetail) ? personDetail.getLastName() : null;
    }

    @SuppressWarnings({"squid:S3655"})
    private static BailStatus getBailStatus(final uk.gov.justice.core.courts.Defendant d) {
        return nonNull(d.getPersonDefendant()) ? d.getPersonDefendant().getBailStatus() : null;
    }

    private static String getOrganisationName(final Organisation organisation) {
        return nonNull(organisation) ? organisation.getName() : null;
    }

    private static String getDefenceOrganisationName(final Optional<AssociatedDefenceOrganisation> organisation) {
        if (nonNull(organisation) && organisation.isPresent() && nonNull(organisation.get().getDefenceOrganisation()) && nonNull(organisation.get().getDefenceOrganisation().getOrganisation())) {
            return organisation.get().getDefenceOrganisation().getOrganisation().getName();
        }
        return null;
    }

    @SuppressWarnings({"squid:S3655"})
    private static Offence buildOffence(final uk.gov.justice.core.courts.Offence o, final List<UUID> shadowListedOffences, final ListedCase listedCase) {
        final Offence.Builder builder = Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withRestrictFromCourtList(listedCase.getRestrictFromCourtList())
                .withLaaApplnReference(buildLaaReference(ofNullable(o.getLaaApplnReference())))
                .withLaidDate(o.getLaidDate())
                .withSeedingHearing(nonNull(o.getSeedingHearing()) ? buildSeedingHearing(o.getSeedingHearing()) : null)
                .withShadowListed(isNotEmpty(shadowListedOffences) && shadowListedOffences.contains(o.getId()));

        if (!isNull(o.getReportingRestrictions()) && !o.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(o.getReportingRestrictions().stream()
                    .map(CourtToEventConverter::courtsToEvents)
                    .collect(toList())
            );
        }

        return builder.build();
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
                .withRestrictFromCourtList(false)
                .withLaaApplnReference(buildLaaReference(ofNullable(o.getLaaApplnReference())))
                .withLaidDate(o.getLaidDate())
                .withSeedingHearing(nonNull(o.getSeedingHearing()) ? buildSeedingHearing(o.getSeedingHearing()) : null)
                .withShadowListed(isNotEmpty(shadowListedOffences) && shadowListedOffences.contains(o.getId()));

        if (!isNull(o.getReportingRestrictions()) && !o.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(o.getReportingRestrictions().stream()
                    .map(CourtToEventConverter::courtsToEvents)
                    .collect(toList())
            );
        }

        return builder.build();
    }

    private static uk.gov.justice.listing.events.ReportingRestriction courtsToEvents(final uk.gov.justice.core.courts.ReportingRestriction reportingRestriction) {
        return uk.gov.justice.listing.events.ReportingRestriction.reportingRestriction()
                .withId(reportingRestriction.getId())
                .withLabel(reportingRestriction.getLabel())
                .withJudicialResultId(reportingRestriction.getJudicialResultId())
                .withOrderedDate(getOrderedDate(ofNullable(reportingRestriction.getOrderedDate())))
                .build();
    }

    private static LocalDate getOrderedDate(final Optional<String> orderedDate) {
        return orderedDate.isPresent() && !StringUtils.isEmpty(orderedDate.get()) ?
                LocalDate.parse(orderedDate.get()) : null;
    }

    @SuppressWarnings({"squid:S3655"})
    private static Person getPersonDetail(final uk.gov.justice.core.courts.Defendant d) {
        if (nonNull(d) && nonNull(d.getPersonDefendant())) {
            return d.getPersonDefendant().getPersonDetails();
        }
        return null;
    }

    private static StatementOfOffence buildStatementOfOffence(final uk.gov.justice.core.courts.Offence o) {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(o.getOffenceLegislation())
                .withTitle(o.getOffenceTitle())
                .withWelshLegislation(o.getOffenceLegislationWelsh())
                .withWelshTitle(StringUtils.isNotEmpty(o.getOffenceTitleWelsh()) ? o.getOffenceTitleWelsh() : o.getOffenceTitle())
                .build();
    }

    private static LaaReference buildLaaReference(final Optional<uk.gov.justice.core.courts.LaaReference> laaReference) {
        if (laaReference.isPresent()) {
            return LaaReference.laaReference()
                    .withApplicationReference(laaReference.get().getApplicationReference())
                    .withEffectiveEndDate(laaReference.get().getEffectiveEndDate())
                    .withEffectiveStartDate(laaReference.get().getEffectiveStartDate())
                    .withStatusCode(laaReference.get().getStatusCode())
                    .withStatusDescription(laaReference.get().getStatusDescription())
                    .withStatusDate(laaReference.get().getStatusDate())
                    .withStatusId(laaReference.get().getStatusId())
                    .build();
        }
        return null;
    }

    private static SeedingHearing buildSeedingHearing(final uk.gov.justice.core.courts.SeedingHearing seedingHearing) {
        if(nonNull(seedingHearing)) {
            return SeedingHearing.seedingHearing()
                    .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                    .withJurisdictionType(seedingHearing.getJurisdictionType())
                    .withSittingDay(seedingHearing.getSittingDay())
                    .build();
        }
        return null;
    }

    @SuppressWarnings({"squid:S3655"})
    private static LaaReference getLaaApplnReference(final uk.gov.moj.cpp.listing.domain.Offence o) {
        if(nonNull(o.getLaaApplnReference()) && o.getLaaApplnReference().isPresent()){
            return buildLaaReference(o.getLaaApplnReference().get());
        }
        return null;
    }

    private static LaaReference buildLaaReference(final uk.gov.moj.cpp.listing.domain.LaaReference laaReference) {
        if(nonNull(laaReference)) {
            return LaaReference.laaReference()
                    .withApplicationReference(laaReference.getApplicationReference())
                    .withEffectiveEndDate(laaReference.getEffectiveEndDate().orElse(null))
                    .withEffectiveEndDate(laaReference.getEffectiveStartDate().orElse(null))
                    .withStatusCode(laaReference.getStatusCode())
                    .withStatusDescription(laaReference.getStatusDescription())
                    .withStatusDate(laaReference.getStatusDate())
                    .withStatusId(laaReference.getStatusId())
                    .build();
        }
        return null;
    }

    private static CommittingCourt buildCommittingCourt(final uk.gov.moj.cpp.listing.domain.CommittingCourt committingCourt) {
        if(nonNull(committingCourt)) {
            return CommittingCourt.committingCourt()
                    .withCourtCentreId(committingCourt.getCourtCentreId())
                    .withCourtHouseCode(committingCourt.getCourtHouseCode().orElse(null))
                    .withCourtHouseName(committingCourt.getCourtHouseName())
                    .withCourtHouseShortName(committingCourt.getCourtHouseShortName().orElse(null))
                    .build();
        }
        return null;
    }

    private static uk.gov.justice.listing.events.ReportingRestriction domainToEvents(final ReportingRestriction reportingRestriction) {
        if(nonNull(reportingRestriction)) {
            return uk.gov.justice.listing.events.ReportingRestriction.reportingRestriction()
                    .withId(reportingRestriction.getId())
                    .withJudicialResultId(reportingRestriction.getJudicialResultId().orElse(null))
                    .withLabel(reportingRestriction.getLabel())
                    .withOrderedDate(reportingRestriction.getOrderedDate().orElse(null))
                    .build();
        }
        return null;
    }

    private static StatementOfOffence buildStatementOfOffence(final uk.gov.moj.cpp.listing.domain.Offence o) {
        if(nonNull(o)) {
            return StatementOfOffence.statementOfOffence()
                    .withLegislation(o.getStatementOfOffence().getLegislation().orElse(null))
                    .withTitle(o.getStatementOfOffence().getTitle())
                    .withWelshLegislation(o.getStatementOfOffence().getWelshLegislation().orElse(null))
                    .withWelshTitle(o.getStatementOfOffence().getWelshTitle())
                    .build();
        }
        return null;
    }

    private static SeedingHearing buildSeedingHearing(final uk.gov.moj.cpp.listing.domain.SeedingHearing seedingHearing) {
        if(nonNull(seedingHearing)) {
            return SeedingHearing.seedingHearing()
                    .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                    .withJurisdictionType(JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()))
                    .withSittingDay(seedingHearing.getSittingDay())
                    .build();
        }
        return null;
    }

}
