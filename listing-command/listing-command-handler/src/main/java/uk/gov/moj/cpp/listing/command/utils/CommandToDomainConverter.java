package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.listing.domain.Defendant.defendant;
import static uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.valueFor;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.justice.services.common.converter.Converter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity"})
public class CommandToDomainConverter implements Converter<uk.gov.justice.core.courts.HearingListingNeeds, Hearing> {

    @Override
    public uk.gov.moj.cpp.listing.domain.Hearing convert(final uk.gov.justice.core.courts.HearingListingNeeds commandHearing) {
        List<uk.gov.moj.cpp.listing.domain.JudicialRole> domainJudicialRoles = emptyList();
        if (commandHearing.getJudiciary() != null) {
            domainJudicialRoles = commandHearing.getJudiciary().stream()
                    .map(this::buildJudiciary)
                    .collect(Collectors.toList());
        }

        List<ListedCase> domainListedCases = commandHearing.getProsecutionCases().stream()
                .map(prosecutionCase -> buildListedCases(commandHearing, prosecutionCase))
                .collect(Collectors.toList());

        return uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(commandHearing.getId())
                .withType(buildHearingType(commandHearing.getType()))
                .withHearingLanguage(empty())
                .withEstimatedMinutes(commandHearing.getEstimatedMinutes())
                .withStartDate(commandHearing.getEarliestStartDateTime().toLocalDate())
                .withCourtCentreId(commandHearing.getCourtCentre().getId())
                .withCourtRoomId(commandHearing.getCourtCentre().getRoomId())
                .withListingDirections(commandHearing.getListingDirections())
                .withProsecutorDatesToAvoid(commandHearing.getProsecutorDatesToAvoid())
                .withReportingRestrictionReason(commandHearing.getReportingRestrictionReason())
                .withJudiciary(domainJudicialRoles)
                .withJurisdictionType(JurisdictionType.valueFor(commandHearing.getJurisdictionType().name())
                        .orElseThrow(IllegalArgumentException::new))
                .withListedCases(domainListedCases)
                .withEndDate(commandHearing.getEndDate().isPresent()? of(LocalDate.parse(commandHearing.getEndDate().get())) : empty())
                .withNonSittingDays(emptyList())
                .withNonDefaultDays(emptyList())
                .withHearingDays(emptyList())
                .build();
    }

    private Type buildHearingType(HearingType type) {
        return Type.type()
                .withId(type.getId())
                .withDescription(type.getDescription())
                .build();
    }


    private JudicialRole buildJudiciary(final uk.gov.justice.core.courts.JudicialRole judicialRole) {
        return JudicialRole.judicialRole()
                .withJudicialId(judicialRole.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(judicialRole.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(judicialRole.getJudicialRoleType().getJudicialRoleTypeId())
                        .build())
                .withIsDeputy(judicialRole.getIsDeputy())
                .withIsBenchChairman(judicialRole.getIsBenchChairman())
                .build();
    }

    private ListedCase buildListedCases(final HearingListingNeeds commandHearing, ProsecutionCase prosecutionCase) {
        return ListedCase.listedCase()
                .withId(prosecutionCase.getId())
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                        .withAuthorityId(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())
                        .withCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference() != null
                                ? prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()
                                : prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                        .build())
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(d -> buildDefendants(commandHearing, d))
                        .collect(Collectors.toList()))

                .build();
    }

    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendants(final HearingListingNeeds commandHearing, uk.gov.justice.core.courts.Defendant d) {
        return defendant()
                .withId(d.getId())
                .withFirstName(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getFirstName().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getFirstName().get()) : empty())
                .withLastName(d.getPersonDefendant().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getLastName()) : empty())
                .withBailStatus(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getBailStatus().isPresent()
                        ? BailStatus.valueFor(d.getPersonDefendant().get().getBailStatus().get().toString()) : empty())
                .withDefenceOrganisation(d.getDefenceOrganisation().isPresent() ? of(d.getDefenceOrganisation().get().getName()) : empty())
                .withOrganisationName(d.getLegalEntityDefendant().isPresent() ? of(d.getLegalEntityDefendant().get().getOrganisation().getName()) : empty())
                .withSpecificRequirements(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getSpecificRequirements() : empty())
                .withDatesToAvoid(getDatesToAvoid(commandHearing, d))
                .withDateOfBirth(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getDateOfBirth() : empty())
                .withCustodyTimeLimit(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getCustodyTimeLimit() : empty())
                .withHearingLanguageNeeds(getHearingLanguageNeeds(commandHearing, d))
                .withOffences(d.getOffences().stream()
                        .map(this::buildOffence)
                        .collect(Collectors.toList()))
                .build();
    }

    private Optional<String> getDatesToAvoid(HearingListingNeeds commandHearing, Defendant d) {
        Optional<DefendantListingNeeds> listDefendantRequest = findListDefendantRequestByDefendantId(commandHearing.getDefendantListingNeeds(), d.getId());
        if(listDefendantRequest.isPresent() && listDefendantRequest.get().getDatesToAvoid().isPresent()){
            return listDefendantRequest.get().getDatesToAvoid();
        }
        return empty();
    }

    private Optional<HearingLanguageNeeds> getHearingLanguageNeeds(HearingListingNeeds commandHearing, Defendant d) {
        Optional<DefendantListingNeeds> listDefendantRequest = findListDefendantRequestByDefendantId(commandHearing.getDefendantListingNeeds(), d.getId());
        if(listDefendantRequest.isPresent() && listDefendantRequest.get().getHearingLanguageNeeds().isPresent()){
            return valueFor(listDefendantRequest.orElseThrow(IllegalArgumentException::new)
                    .getHearingLanguageNeeds().orElseThrow(IllegalArgumentException::new).toString());
        }
        return empty();
    }


    private uk.gov.moj.cpp.listing.domain.Offence buildOffence(final uk.gov.justice.core.courts.Offence o) {
        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .build();
    }

    private StatementOfOffence buildStatementOfOffence(final uk.gov.justice.core.courts.Offence offence) {
        return StatementOfOffence.statementOfOffence()
                .withTitle(offence.getOffenceTitle())
                .withLegislation(offence.getOffenceLegislation())
                .withWelshLegislation(offence.getOffenceLegislationWelsh())
                .withWelshTitle(offence.getOffenceTitleWelsh().orElse(offence.getOffenceTitle()))
                .build();
    }

    private Optional<DefendantListingNeeds> findListDefendantRequestByDefendantId(List<DefendantListingNeeds> listDefendantRequests, UUID defendantId) {
        return listDefendantRequests.stream().filter(ldr -> ldr.getDefendantId().equals(defendantId)).findFirst();
    }


}


