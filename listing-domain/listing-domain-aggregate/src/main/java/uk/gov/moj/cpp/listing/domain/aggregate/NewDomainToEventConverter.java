package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CommittingCourt;
import uk.gov.justice.listing.events.CourtApplicationPartyType;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.HearingLanguageNeeds;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.LaaReference;
import uk.gov.justice.listing.events.LinkedToCases;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.SimpleOffence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.ApplicantRespondent;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.aggregate.converter.ReportingRestrictionConverter;

import java.util.List;
import java.util.Optional;


@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:S2583"})
public class NewDomainToEventConverter {

    private NewDomainToEventConverter() {
    }

    public static uk.gov.justice.listing.events.ListedCase buildListedCase(final ListedCase lc) {
        return uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(lc.getId())
                .withCaseIdentifier(buildCaseIdentifier(lc))
                .withMarkers(isNull(lc.getCaseMarkers()) ? emptyList() : lc.getCaseMarkers().stream()
                        .map(NewDomainToEventConverter::convertCaseMarkersToMarkers)
                        .collect(toList()))
                .withDefendants(lc.getDefendants().stream()
                        .map(NewDomainToEventConverter::buildDefendant)
                        .collect(toList()))
                .withRestrictFromCourtList(of(Boolean.FALSE))
                .withShadowListed(lc.getShadowListed())
                .build();
    }

    public static List<Marker> convertCaseMarkersListToMarkers(final List<CaseMarker> caseMarkers) {
        return caseMarkers.stream().map(NewDomainToEventConverter::convertCaseMarkersToMarkers).collect(toList());
    }

    public static Marker convertCaseMarkersToMarkers(final CaseMarker caseMarker) {
        return Marker.marker().withId(caseMarker.getId())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .withMarkerTypeid(caseMarker.getMarkerTypeid()).build();
    }


    @SuppressWarnings({"squid:S3655", "squid:S1067"})
    public static Defendant buildDefendant(final uk.gov.moj.cpp.listing.domain.Defendant d) {
        return Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(d.getMasterDefendantId())
                .withCourtProceedingsInitiated(d.getCourtProceedingsInitiated())
                .withCustodyTimeLimit(d.getCustodyTimeLimit())
                .withDateOfBirth(d.getDateOfBirth())
                .withFirstName(d.getFirstName())
                .withLastName(d.getLastName())
                .withDatesToAvoid(d.getDatesToAvoid())
                .withHearingLanguageNeeds(d.getHearingLanguageNeeds().isPresent()
                        ? HearingLanguageNeeds.valueFor(d.getHearingLanguageNeeds().get().toString())
                        : empty())
                .withOrganisationName(d.getOrganisationName())
                .withSpecificRequirements(d.getSpecificRequirements())
                .withOffences(d.getOffences().stream()
                        .map(NewDomainToEventConverter::buildOffence)
                        .collect(toList()))
                .withDefenceOrganisation(d.getDefenceOrganisation())
                .withBailStatus(buildBailStatusEvent(d.getBailStatus()))
                .withRestrictFromCourtList(of(FALSE))
                .withIsYouth(d.getIsYouth())
                .withAddress(nonNull(d.getAddress()) && d.getAddress().isPresent() ? buildAddress(d.getAddress().get()) : empty())
                .withNationalityDescription(d.getNationalityDescription())
                .build();
    }


    @SuppressWarnings({"squid:S3655", "squid:S1067"})
    private static Optional<uk.gov.justice.core.courts.Address> buildAddress(final uk.gov.moj.cpp.listing.domain.Address address) {

        return of(uk.gov.justice.core.courts.Address.address()
                .withAddress1(ofNullable(address.getAddress1()).orElse(""))
                .withAddress2(ofNullable(address.getAddress2()).orElse(empty()))
                .withAddress3(ofNullable(address.getAddress3()).orElse(empty()))
                .withAddress4(ofNullable(address.getAddress4()).orElse(empty()))
                .withAddress5(ofNullable(address.getAddress5()).orElse(empty()))
                .withPostcode(ofNullable(address.getPostcode()).orElse(empty()))
                .build());
    }

    public static NewBaseDefendant buildNewBaseDefendant(final uk.gov.moj.cpp.listing.domain.Defendant d) {
        return NewBaseDefendant.newBaseDefendant()
                .withId(d.getId())
                .withMasterDefendantId(d.getMasterDefendantId())
                .withCustodyTimeLimit(d.getCustodyTimeLimit())
                .withDateOfBirth(d.getDateOfBirth())
                .withFirstName(d.getFirstName())
                .withLastName(d.getLastName())
                .withOrganisationName(d.getOrganisationName())
                .withSpecificRequirements(d.getSpecificRequirements())
                .withDefenceOrganisation(d.getDefenceOrganisation())
                .withBailStatus(buildBailStatusEvent(d.getBailStatus()))
                .withIsYouth(d.getIsYouth())
                .withAddress(nonNull(d.getAddress()) && d.getAddress().isPresent() ? buildAddress(d.getAddress().get()) : empty())
                .withNationalityDescription(nonNull(d.getNationalityDescription()) && d.getNationalityDescription().isPresent() ? d.getNationalityDescription() : empty())
                .build();
    }

    public static List<SimpleOffence> buildSimpleOffences(final List<uk.gov.moj.cpp.listing.domain.SimpleOffence> offences) {
        return offences.stream().map(simpleOffence ->
                SimpleOffence.simpleOffence()
                        .withId(simpleOffence.getId())
                        .withDefendantId(simpleOffence.getDefendantId())
                        .build())
                .collect(toList());
    }

    public static List<Offence> buildOffences(final List<uk.gov.moj.cpp.listing.domain.Offence> o) {
        return o.stream().map(NewDomainToEventConverter::buildOffence).collect(toList());
    }

    public static List<LinkedToCases> convertDomainToLinkedToCasesEvent(final List<uk.gov.moj.cpp.listing.domain.LinkedToCases> linkedToCases) {
        return linkedToCases.stream()
                .map(NewDomainToEventConverter::buildLinkedToCases)
                .collect(toList());
    }

    private static LinkedToCases buildLinkedToCases(final uk.gov.moj.cpp.listing.domain.LinkedToCases linkedToCases) {
        return LinkedToCases.linkedToCases()
                .withCaseId(linkedToCases.getCaseId())
                .withCaseUrn(linkedToCases.getCaseUrn())
                .build();
    }

    @SuppressWarnings({"squid:S3655"})
    public static Offence buildOffence(final uk.gov.moj.cpp.listing.domain.Offence o) {
        final Offence.Builder builder = Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withOffenceWording(o.getOffenceWording())
                .withRestrictFromCourtList(of(FALSE))
                .withLaaApplnReference(o.getLaaApplnReference().isPresent() ? buildLaaReference(o.getLaaApplnReference().get()) : empty())
                .withLaidDate(o.getLaidDate())
                .withShadowListed(o.getShadowListed());

        if (nonNull(o.getCommittingCourt()) && o.getCommittingCourt().isPresent()) {
            builder.withCommittingCourt(buildCommittingCourt(o.getCommittingCourt().get()));
        }

        if (!isNull(o.getReportingRestrictions()) && !o.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(o.getReportingRestrictions().stream()
                    .map(ReportingRestrictionConverter::domainToEvents)
                    .collect(toList())
            );
        }

        return builder.build();
    }

    private static Optional<uk.gov.justice.core.courts.BailStatus> buildBailStatusEvent(final Optional<uk.gov.moj.cpp.listing.domain.BailStatus> bailStatus) {
        if (bailStatus.isPresent()) {
            return bailStatus.map(bs -> uk.gov.justice.core.courts.BailStatus.bailStatus().withCode(bs.getCode()).withDescription(bs.getDescription()).withId(bs.getId()).build());
        }
        return empty();
    }

    private static StatementOfOffence buildStatementOfOffence(final uk.gov.moj.cpp.listing.domain.Offence o) {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(o.getStatementOfOffence().getLegislation())
                .withTitle(o.getStatementOfOffence().getTitle())
                .withWelshLegislation(o.getStatementOfOffence().getWelshLegislation())
                .withWelshTitle(o.getStatementOfOffence().getWelshTitle())
                .build();
    }

    private static CaseIdentifier buildCaseIdentifier(final ListedCase lc) {
        return CaseIdentifier.caseIdentifier()
                .withAuthorityCode(lc.getCaseIdentifier().getAuthorityCode())
                .withAuthorityId(lc.getCaseIdentifier().getAuthorityId())
                .withCaseReference(lc.getCaseIdentifier().getCaseReference())
                .build();
    }

    public static uk.gov.justice.listing.events.JudicialRole buildJudicialRole(final JudicialRole domainJudicialRole) {
        return uk.gov.justice.listing.events.JudicialRole.judicialRole()
                .withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudicialRoleTypeId(domainJudicialRole.getJudicialRoleType().getJudicialRoleTypeId())
                        .withJudiciaryType(domainJudicialRole.getJudicialRoleType().getJudiciaryType())
                        .build())
                .withJudicialId(domainJudicialRole.getJudicialId())
                .withIsDeputy(domainJudicialRole.getIsDeputy())
                .withIsBenchChairman(domainJudicialRole.getIsBenchChairman())
                .withUserId(domainJudicialRole.getUserId())
                .build();
    }

    public static uk.gov.justice.listing.events.CourtApplication buildCourtApplications(final CourtApplication courtApplication) {
        return uk.gov.justice.listing.events.CourtApplication.courtApplication()
                .withId(courtApplication.getId())
                .withLinkedCaseIds(courtApplication.getLinkedCaseIds())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withApplicationType(courtApplication.getApplicationType())
                .withApplicant(buildApplicantRespondent(courtApplication.getApplicant()))
                .withRespondents(ofNullable(courtApplication.getRespondents())
                        .map(respondents -> respondents
                                .stream()
                                .map(NewDomainToEventConverter::buildApplicantRespondent)
                                .collect(toList()))
                        .orElse(null))
                .withApplicationReference(ofNullable(courtApplication.getApplicationReference()).orElse(empty()))
                .withApplicationParticulars(ofNullable(courtApplication.getApplicationParticulars()).orElse(empty()))
                .withRestrictFromCourtList(of(FALSE))
                .withRestrictCourtApplicationType(of(FALSE))
                .build();
    }

    private static uk.gov.justice.listing.events.ApplicantRespondent buildApplicantRespondent(final ApplicantRespondent applicant) {
        return isNull(applicant) ? null : uk.gov.justice.listing.events.ApplicantRespondent.applicantRespondent()
                .withId(applicant.getId())
                .withFirstName(applicant.getFirstName())
                .withLastName(applicant.getLastName())
                .withIsRespondent(applicant.getIsRespondent())
                .withRestrictFromCourtList(of(FALSE))
                .withCourtApplicationPartyType(buildCourtApplicationPartyTypeEvent(applicant.getCourtApplicationPartyType()))
                .withAddress(ofNullable(applicant.getAddress()).flatMap(NewDomainToEventConverter::buildAddress))
                .build();
    }

    private static CourtApplicationPartyType buildCourtApplicationPartyTypeEvent(final uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType courtApplicationPartyType) {

        return CourtApplicationPartyType.valueOf(courtApplicationPartyType.name());
    }

    private static Optional<LaaReference> buildLaaReference(final uk.gov.moj.cpp.listing.domain.LaaReference laaReference) {

        return of(LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate((laaReference.getEffectiveEndDate()))
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusId(laaReference.getStatusId())
                .build());
    }

    private static Optional<CommittingCourt> buildCommittingCourt(final uk.gov.moj.cpp.listing.domain.CommittingCourt committingCourt) {

        return of(CommittingCourt.committingCourt()
                .withCourtCentreId(committingCourt.getCourtCentreId())
                .withCourtHouseCode(committingCourt.getCourtHouseCode())
                .withCourtHouseName(committingCourt.getCourtHouseName())
                .withCourtHouseShortName(committingCourt.getCourtHouseShortName())
                .build());
    }
}
