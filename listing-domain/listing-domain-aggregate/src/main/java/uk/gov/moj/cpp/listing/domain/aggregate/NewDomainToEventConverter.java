package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CourtApplicationPartyType;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.HearingLanguageNeeds;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.LaaReference;
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

import java.util.List;
import java.util.Optional;


@SuppressWarnings({"squid:S3655"})
public class NewDomainToEventConverter {

    private NewDomainToEventConverter() {
    }

    public static uk.gov.justice.listing.events.ListedCase buildListedCase(ListedCase lc) {
        return uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(lc.getId())
                .withCaseIdentifier(buildCaseIdentifier(lc))
                .withMarkers(isNull(lc.getCaseMarkers()) ? emptyList(): lc.getCaseMarkers().stream()
                        .map(NewDomainToEventConverter::convertCaseMarkersToMarkers)
                        .collect(toList()))
                .withDefendants(lc.getDefendants().stream()
                        .map(NewDomainToEventConverter::buildDefendant)
                        .collect(toList()))
                .withRestrictFromCourtList(Optional.of(Boolean.FALSE))
                .build();
    }

    public static List<Marker> convertCaseMarkersListToMarkers(final List<CaseMarker> caseMarkers) {
        return caseMarkers.stream().map(cm -> convertCaseMarkersToMarkers(cm)).collect(toList());
    }

    public static Marker convertCaseMarkersToMarkers(final CaseMarker caseMarker) {
        return Marker.marker().withId(caseMarker.getId())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .withMarkerTypeid(caseMarker.getMarkerTypeid()).build();
    }


    public static Defendant buildDefendant(uk.gov.moj.cpp.listing.domain.Defendant d) {
        return Defendant.defendant()
                .withId(d.getId())
                .withCustodyTimeLimit(d.getCustodyTimeLimit())
                .withDateOfBirth(d.getDateOfBirth())
                .withFirstName(d.getFirstName())
                .withLastName(d.getLastName())
                .withDatesToAvoid(d.getDatesToAvoid())
                .withHearingLanguageNeeds(d.getHearingLanguageNeeds().isPresent()
                        ? HearingLanguageNeeds.valueFor(d.getHearingLanguageNeeds().get().toString())
                        : Optional.empty())
                .withOrganisationName(d.getOrganisationName())
                .withSpecificRequirements(d.getSpecificRequirements())
                .withOffences(d.getOffences().stream()
                        .map(NewDomainToEventConverter::buildOffence)
                        .collect(toList()))
                .withDefenceOrganisation(d.getDefenceOrganisation())
                .withBailStatus(buildBailStatusEvent(d.getBailStatus()))
                .withRestrictFromCourtList(Optional.of(Boolean.FALSE))
                .withIsYouth(d.getIsYouth())
                .build();
    }

    public static NewBaseDefendant buildNewBaseDefendant(uk.gov.moj.cpp.listing.domain.Defendant d) {
        return NewBaseDefendant.newBaseDefendant()
                .withId(d.getId())
                .withCustodyTimeLimit(d.getCustodyTimeLimit())
                .withDateOfBirth(d.getDateOfBirth())
                .withFirstName(d.getFirstName())
                .withLastName(d.getLastName())
                .withOrganisationName(d.getOrganisationName())
                .withSpecificRequirements(d.getSpecificRequirements())
                .withDefenceOrganisation(d.getDefenceOrganisation())
                .withBailStatus(buildBailStatusEvent(d.getBailStatus()))
                .withIsYouth(d.getIsYouth())
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

    public static List<Offence> buildOffences(List<uk.gov.moj.cpp.listing.domain.Offence> o) {
        return o.stream().map(NewDomainToEventConverter::buildOffence).collect(toList());
    }

    public static Offence buildOffence(uk.gov.moj.cpp.listing.domain.Offence o) {
        return Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withOffenceWording(o.getOffenceWording())
                .withRestrictFromCourtList(Optional.of(Boolean.FALSE))
                .withLaaApplnReference(o.getLaaApplnReference().isPresent() ? buildLaaReference(o.getLaaApplnReference().get()) : empty())
                .withLaidDate(o.getLaidDate())
                .build();
    }

    private static Optional<uk.gov.justice.core.courts.BailStatus> buildBailStatusEvent(Optional<uk.gov.moj.cpp.listing.domain.BailStatus> bailStatus) {
        if (bailStatus.isPresent()) {
            return bailStatus.map(bs -> uk.gov.justice.core.courts.BailStatus.bailStatus().withCode(bs.getCode()).withDescription(bs.getDescription()).withId(bs.getId()).build());
        }
        return empty();
    }

    private static StatementOfOffence buildStatementOfOffence(uk.gov.moj.cpp.listing.domain.Offence o) {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(o.getStatementOfOffence().getLegislation())
                .withTitle(o.getStatementOfOffence().getTitle())
                .withWelshLegislation(o.getStatementOfOffence().getWelshLegislation())
                .withWelshTitle(o.getStatementOfOffence().getWelshTitle())
                .build();
    }

    private static CaseIdentifier buildCaseIdentifier(ListedCase lc) {
        return CaseIdentifier.caseIdentifier()
                .withAuthorityCode(lc.getCaseIdentifier().getAuthorityCode())
                .withAuthorityId(lc.getCaseIdentifier().getAuthorityId())
                .withCaseReference(lc.getCaseIdentifier().getCaseReference())
                .build();
    }

    public static uk.gov.justice.listing.events.JudicialRole buildJudicialRole(JudicialRole domainJudicialRole) {
        return uk.gov.justice.listing.events.JudicialRole.judicialRole()
                .withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudicialRoleTypeId(domainJudicialRole.getJudicialRoleType().getJudicialRoleTypeId())
                        .withJudiciaryType(domainJudicialRole.getJudicialRoleType().getJudiciaryType())
                        .build())
                .withJudicialId(domainJudicialRole.getJudicialId())
                .withIsDeputy(domainJudicialRole.getIsDeputy())
                .withIsBenchChairman(domainJudicialRole.getIsBenchChairman())
                .build();
    }

    public static uk.gov.justice.listing.events.CourtApplication buildCourtApplications(final CourtApplication courtApplication) {
        return uk.gov.justice.listing.events.CourtApplication.courtApplication()
                .withId(courtApplication.getId())
                .withLinkedCaseId(courtApplication.getLinkedCaseId())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withApplicationType(courtApplication.getApplicationType())
                .withApplicant(buildApplicantRespondant(courtApplication.getApplicant()))
                .withRespondents(nonNull(courtApplication.getRespondents()) ? courtApplication.getRespondents().stream().map(NewDomainToEventConverter::buildApplicantRespondant).collect(toList()): null)
                .withApplicationReference(courtApplication.getApplicationReference().isPresent() ? courtApplication.getApplicationReference() : empty())
                .withRestrictFromCourtList(Optional.of(Boolean.FALSE))
                .withRestrictCourtApplicationType(Optional.of(Boolean.FALSE))
                .build();
    }
    private static uk.gov.justice.listing.events.ApplicantRespondent buildApplicantRespondant(final ApplicantRespondent applicant){
        return isNull(applicant) ? null : uk.gov.justice.listing.events.ApplicantRespondent.applicantRespondent()
                .withId(applicant.getId())
                .withFirstName(applicant.getFirstName())
                .withLastName(applicant.getLastName())
                .withIsRespondent(applicant.getIsRespondent())
                .withRestrictFromCourtList(Optional.of(Boolean.FALSE))
                .withCourtApplicationPartyType(buildCourtApplicationPartyTypeEvent(applicant.getCourtApplicationPartyType()))
                .build();
    }
    private static CourtApplicationPartyType buildCourtApplicationPartyTypeEvent(uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType courtApplicationPartyType) {

        return CourtApplicationPartyType.valueOf(courtApplicationPartyType.name());
    }

    private static Optional<LaaReference> buildLaaReference(uk.gov.moj.cpp.listing.domain.LaaReference laaReference) {

        return Optional.of(LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate((laaReference.getEffectiveEndDate()))
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusId(laaReference.getStatusId())
                .build());


    }

}
