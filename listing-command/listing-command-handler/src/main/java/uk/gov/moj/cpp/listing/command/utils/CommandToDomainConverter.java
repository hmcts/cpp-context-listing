package uk.gov.moj.cpp.listing.command.utils;


import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.listing.domain.Defendant.defendant;
import static uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.valueFor;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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
                    .collect(toList());
        }

        List<ListedCase> domainListedCases = listStandAloneApplications(commandHearing) ? emptyList() :commandHearing.getProsecutionCases().stream()
                .map(prosecutionCase -> buildListedCases(commandHearing, prosecutionCase))
                .collect(toList());

        return uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(commandHearing.getId())
                .withType(buildHearingType(commandHearing.getType()))
                .withHearingLanguage(empty())
                .withEstimatedMinutes(commandHearing.getEstimatedMinutes())
                .withStartDateTime(ZonedDateTimes.fromString(getStartDateTime(commandHearing).toString()))
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
                .withCourtApplication(isNull(commandHearing.getCourtApplications())? emptyList(): commandHearing.getCourtApplications()
                        .stream().map(ca ->  new CourtApplicationToDomainConverter().convert(ca)).collect(toList()))
                .withCourtApplicationPartyNeeds(isNull(commandHearing.getCourtApplicationPartyListingNeeds())
                        ? emptyList(): commandHearing.getCourtApplicationPartyListingNeeds().stream()
                        .map(this::buildCourtApplicationPartyNeeds).collect(toList()))
                .build();
    }

    private ZonedDateTime getStartDateTime(HearingListingNeeds commandHearing) {
        final ZonedDateTime listedStartDateTime = commandHearing.getListedStartDateTime().isPresent()? commandHearing.getListedStartDateTime().get() : null;
        final ZonedDateTime earliestStartDateTime =  commandHearing.getEarliestStartDateTime().isPresent()? commandHearing.getEarliestStartDateTime().get() : null;

        return Optional.ofNullable(listedStartDateTime).orElseGet(() -> earliestStartDateTime);
    }


    private Type buildHearingType(HearingType type) {
        return Type.type()
                .withId(type.getId())
                .withDescription(type.getDescription())
                .build();
    }

    private boolean listStandAloneApplications(final HearingListingNeeds hearingListingNeeds) {
        if (isNull(hearingListingNeeds.getProsecutionCases())) {
            if(linkedCourtApplications(hearingListingNeeds)) {
                throw new DataValidationException("List of prosecution cases must be supplied for a linked case application");
            }
            return true;
        }
        return false;
    }

    private boolean linkedCourtApplications(final HearingListingNeeds hearingListingNeeds) {
        return  hearingListingNeeds.getCourtApplications().stream()
                .anyMatch(courtApplication -> courtApplication.getLinkedCaseId().isPresent());
    }

    private JudicialRole buildJudiciary(final uk.gov.justice.core.courts.JudicialRole judicialRole) {
        return JudicialRole.judicialRole()
                .withJudicialId(judicialRole.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(judicialRole.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(judicialRole.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
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
                        .collect(toList()))

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
                        .collect(toList()))
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
        return isNull(listDefendantRequests) ? empty() : listDefendantRequests.stream().filter(ldr -> ldr.getDefendantId().equals(defendantId)).findFirst();
    }

    private CourtApplicationPartyListingNeeds buildCourtApplicationPartyNeeds(uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds partyNeeds) {
        return CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                .withCourtApplicationId(partyNeeds.getCourtApplicationId())
                .withCourtApplicationPartyId(partyNeeds.getCourtApplicationPartyId())
                .withHearingLanguageNeeds(HearingLanguageNeeds.valueFor(
                        partyNeeds.getHearingLanguageNeeds().isPresent()?  partyNeeds.getHearingLanguageNeeds().get().toString() : null).orElse(null))
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendantsForCourtProceedings(final List<ListHearingRequest> listHearingRequests, uk.gov.justice.core.courts.Defendant d) {

        return defendant()
                .withId(d.getId())
                .withFirstName(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getFirstName().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getFirstName().get()) : empty())
                .withLastName(d.getPersonDefendant().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getLastName()) : empty())
                .withBailStatus(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getBailStatus().isPresent()
                        ? BailStatus.valueFor(d.getPersonDefendant().get().getBailStatus().get().toString()) : empty())
                .withDefenceOrganisation(d.getDefenceOrganisation().isPresent() ? of(d.getDefenceOrganisation().get().getName()) : empty())
                .withOrganisationName(d.getLegalEntityDefendant().isPresent() ? of(d.getLegalEntityDefendant().get().getOrganisation().getName()) : empty())
                .withSpecificRequirements(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getSpecificRequirements() : empty())
                .withDatesToAvoid(getDatesToAvoidForListHearingRequest(listHearingRequests, d))
                .withDateOfBirth(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getDateOfBirth() : empty())
                .withCustodyTimeLimit(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getCustodyTimeLimit() : empty())
                .withHearingLanguageNeeds(getHearingLanguageNeeds(listHearingRequests, d))
                .withOffences(d.getOffences().stream()
                        .map(this::buildOffence)
                        .collect(toList()))
                .withProsecutionCaseId(d.getProsecutionCaseId())
                .build();
    }

    private Optional<String> getDatesToAvoidForListHearingRequest(List<ListHearingRequest> commandHearing, Defendant d) {
        final Optional<ListDefendantRequest> listDefendantRequest = findListDefendantRequestForDefendantId(
                commandHearing.stream().flatMap(lhr->lhr.getListDefendantRequests().stream()).collect(Collectors.toList()), d.getId());
        if(listDefendantRequest.isPresent() && listDefendantRequest.get().getDatesToAvoid().isPresent()){
            return listDefendantRequest.get().getDatesToAvoid();
        }
        return empty();
    }

    private Optional<HearingLanguageNeeds> getHearingLanguageNeeds(List<ListHearingRequest> commandHearing, Defendant d) {
        final Optional<ListDefendantRequest> listDefendantRequest = findListDefendantRequestForDefendantId(
                commandHearing.stream().flatMap(lhr->lhr.getListDefendantRequests().stream()).collect(Collectors.toList()), d.getId());
        if(listDefendantRequest.isPresent() && listDefendantRequest.get().getHearingLanguageNeeds().isPresent()){
            return valueFor(listDefendantRequest.orElseThrow(IllegalArgumentException::new)
                    .getHearingLanguageNeeds().orElseThrow(IllegalArgumentException::new).toString());
        }
        return empty();
    }
    public Optional<ListDefendantRequest> findListDefendantRequestForDefendantId(List<ListDefendantRequest> listDefendantRequests, UUID defendantId) {
        return isNull(listDefendantRequests) ? empty() : listDefendantRequests.stream().filter(ldr -> ldr.getDefendantId().toString().equals(defendantId.toString())).findFirst();
    }


    public List<uk.gov.moj.cpp.listing.domain.Defendant> convertDefendant(final List<Defendant> commandDefendants, final List<ListHearingRequest> listHearingRequests) {
        return commandDefendants.stream().map(d -> buildDefendantsForCourtProceedings(listHearingRequests, d)).collect(Collectors.toList());
    }

}


