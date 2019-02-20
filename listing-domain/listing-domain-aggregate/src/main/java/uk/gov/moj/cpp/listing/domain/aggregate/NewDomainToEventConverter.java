package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.BailStatus;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.HearingLanguageNeeds;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.SimpleOffence;
import uk.gov.justice.listing.events.StatementOfOffence;
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
                .withDefendants(lc.getDefendants().stream()
                        .map(NewDomainToEventConverter::buildDefendant)
                        .collect(toList()))
                .build();
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
                .build();
    }

    private static Optional<BailStatus> buildBailStatusEvent(Optional<uk.gov.moj.cpp.listing.domain.BailStatus> bailStatus) {
        if (bailStatus.isPresent()) {

            return BailStatus.valueFor(bailStatus.get().toString());
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
}
