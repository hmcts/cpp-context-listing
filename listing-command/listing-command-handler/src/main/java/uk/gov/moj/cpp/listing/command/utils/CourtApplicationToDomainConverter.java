package uk.gov.moj.cpp.listing.command.utils;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.ORGANISATION;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON_DEFENDANT;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PROSECUTING_AUTHORITY;

import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.listing.courts.Applicant;
import uk.gov.justice.listing.courts.Respondents;
import uk.gov.justice.listing.courts.Subject;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.Address;
import uk.gov.moj.cpp.listing.domain.CourtApplicationParty;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({"squid:UnusedPrivateMethod"})
public class CourtApplicationToDomainConverter implements Converter<uk.gov.justice.core.courts.CourtApplication, CourtApplication> {

    @Override
    public CourtApplication convert(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return CourtApplication.courtApplication()
                .withId(commandCourtApplication.getId())
                .withLinkedCaseIds(getCaseIds(commandCourtApplication))
                .withParentApplicationId(commandCourtApplication.getParentApplicationId())
                .withApplicationType(commandCourtApplication.getType().getType())
                .withApplicant(buildCourtApplicant(commandCourtApplication.getApplicant()))
                .withRespondents(ofNullable(commandCourtApplication.getRespondents())
                        .map(respondents -> respondents
                                .stream()
                                .map(this::buildRespondent)
                                .filter(Objects::nonNull)
                                .collect(toList()))
                        .orElse(emptyList()))
                .withApplicationReference(getApplicationReference(commandCourtApplication))
                .withApplicationParticulars(getApplicationParticulars(commandCourtApplication))
                .withOffences(buildOffences(commandCourtApplication))
                .withSubject(buildCourtSubject(ofNullable(commandCourtApplication.getSubject()).orElse(null)))
                .build();
    }

    private List<UUID> getCaseIds(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        if(isNotEmpty(commandCourtApplication.getCourtApplicationCases())) {
            return commandCourtApplication.getCourtApplicationCases().stream().map(CourtApplicationCase::getProsecutionCaseId).collect(Collectors.toList());
        }
        final Optional<CourtOrder> courtOrder = ofNullable(commandCourtApplication.getCourtOrder());
        return courtOrder.map(order -> order.getCourtOrderOffences().stream().map(CourtOrderOffence::getProsecutionCaseId).collect(toList())).orElseGet(ArrayList::new);
    }

    private Optional<String> getApplicationReference(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return ofNullable(commandCourtApplication.getApplicationReference());
    }

    private Optional<String> getApplicationParticulars(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return ofNullable(commandCourtApplication.getApplicationParticulars());
    }

    public CourtApplication convertListingCoreCourtApplication(final uk.gov.justice.listing.courts.CourtApplication listingCoreCourtApplication) {
        return CourtApplication.courtApplication()
                .withId(listingCoreCourtApplication.getId())
                .withLinkedCaseIds(listingCoreCourtApplication.getLinkedCaseIds())
                .withParentApplicationId(listingCoreCourtApplication.getParentApplicationId())
                .withApplicant(getApplicant(listingCoreCourtApplication.getApplicant()))
                .withRespondents(ofNullable(listingCoreCourtApplication.getRespondents())
                        .map(respondents -> respondents
                                .stream()
                                .map(this::getRespondent)
                                .filter(Objects::nonNull)
                                .collect(toList()))
                        .orElse(null))
                .withApplicationType(listingCoreCourtApplication.getApplicationType())
                .withApplicationReference(ofNullable(listingCoreCourtApplication.getApplicationReference()))
                .withApplicationParticulars(ofNullable(listingCoreCourtApplication.getApplicationParticulars()))
                .withOffences(isNotEmpty(listingCoreCourtApplication.getOffences()) ? listingCoreCourtApplication.getOffences().stream().map(this::getDomainOffence).collect(Collectors.toList()) : emptyList())
                .withSubject(isNull(listingCoreCourtApplication.getSubject()) ? null : getSubject(listingCoreCourtApplication.getSubject()))
                .build();
    }

    private CourtApplicationParty buildRespondent(final uk.gov.justice.core.courts.CourtApplicationParty courtApplicationParty) {
        return buildCourtApplicationParty(courtApplicationParty, TRUE);
    }

    private CourtApplicationParty buildCourtApplicant(final uk.gov.justice.core.courts.CourtApplicationParty courtApplicationParty) {
        return buildCourtApplicationParty(courtApplicationParty, FALSE);
    }

    private CourtApplicationParty buildCourtSubject(final uk.gov.justice.core.courts.CourtApplicationParty courtApplicationParty) {
        return isNull(courtApplicationParty) ? null : buildCourtApplicationParty(courtApplicationParty, FALSE);
    }

    private CourtApplicationParty buildCourtApplicationParty(final uk.gov.justice.core.courts.CourtApplicationParty courtApplicationParty, final boolean isRespondent) {

        CourtApplicationParty applicationParty = ofNullable(courtApplicationParty.getPersonDetails())
                .map(person -> getCourtApplicationParty(
                        courtApplicationParty.getId(),
                        isRespondent,
                        ofNullable(person.getFirstName()),
                        person.getLastName(),
                        PERSON,
                        ofNullable(person.getAddress())))
                .orElse(null);
        if (isNull(applicationParty)) {
            applicationParty = ofNullable(courtApplicationParty.getMasterDefendant())
                    .map(defendant -> getCourtApplicationPartyForLegalEntityDefendant(
                            courtApplicationParty.getId(),
                            isRespondent,
                            ofNullable(defendant.getLegalEntityDefendant())))
                    .orElse(null);
        }

        if (isNull(applicationParty)) {
            applicationParty = ofNullable(courtApplicationParty.getOrganisation())
                    .map(organisation -> getCourtApplicationParty(
                            courtApplicationParty.getId(),
                            isRespondent, empty(),
                            organisation.getName(),
                            ORGANISATION,
                            ofNullable(organisation.getAddress())))
                    .orElse(null);
        }
        if (isNull(applicationParty)) {
            applicationParty = ofNullable(courtApplicationParty.getProsecutingAuthority())
                    .map(prosecutingAuthority -> getCourtApplicationParty(
                            courtApplicationParty.getId(),
                            isRespondent,
                            empty(),
                            prosecutingAuthority.getProsecutionAuthorityCode(),
                            PROSECUTING_AUTHORITY,
                            ofNullable(prosecutingAuthority.getAddress())))
                    .orElse(null);
        }
        if (isNull(applicationParty)) {
            applicationParty = ofNullable(courtApplicationParty.getMasterDefendant())
                    .map(defendant -> getCourtApplicationParty(
                            courtApplicationParty.getId(),
                            isRespondent,
                            ofNullable(defendant.getPersonDefendant())))
                    .orElse(null);
        }
        return applicationParty;
    }

    private CourtApplicationParty getCourtApplicationParty(final UUID id, final boolean isRespondent, final Optional<PersonDefendant> personDefendant) {
        return personDefendant.map(defendant -> getCourtApplicationParty(
                id,
                isRespondent,
                        ofNullable(defendant.getPersonDetails().getFirstName()),
                defendant.getPersonDetails().getLastName(),
                PERSON_DEFENDANT,
                        ofNullable(defendant.getPersonDetails().getAddress())))
                .orElse(null);
    }

    private CourtApplicationParty getCourtApplicationPartyForLegalEntityDefendant(final UUID id, final boolean isRespondent, final Optional<LegalEntityDefendant> legalEntityDefendant) {
        return legalEntityDefendant.map(entityDefendant -> getCourtApplicationParty(
                id,
                isRespondent,
                empty(),
                entityDefendant.getOrganisation().getName(),
                PERSON,
                ofNullable(entityDefendant.getOrganisation().getAddress()))).orElse(null);
    }

    private CourtApplicationParty getCourtApplicationParty(final UUID id, final boolean isRespondent, final Optional<String> firstName, final String lastName, final CourtApplicationPartyType type, Optional<uk.gov.justice.core.courts.Address> address) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(id)
                .withFirstName(firstName.orElse(null))
                .withLastName(lastName)
                .withIsRespondent(isRespondent)
                .withCourtApplicationPartyType(type)
                .withAddress(buildAddress(address))
                .build();
    }

    private Address buildAddress(final Optional<uk.gov.justice.core.courts.Address> address) {
        return address.map(addr -> Address.address()
                .withAddress1(addr.getAddress1())
                .withAddress2(ofNullable(addr.getAddress2()))
                .withAddress3(ofNullable(addr.getAddress3()))
                .withAddress4(ofNullable(addr.getAddress4()))
                .withAddress5(ofNullable(addr.getAddress5()))
                .withPostcode(ofNullable(addr.getPostcode()))
                .withWelshAddress1(ofNullable(addr.getWelshAddress1()))
                .withWelshAddress2(ofNullable(addr.getWelshAddress2()))
                .withWelshAddress3(ofNullable(addr.getWelshAddress3()))
                .withWelshAddress4(ofNullable(addr.getWelshAddress4()))
                .withWelshAddress5(ofNullable(addr.getWelshAddress5()))
                .build())
                .orElse(null);
    }

    private CourtApplicationParty getApplicant(final Applicant applicant) {
        return isNull(applicant) ? null : CourtApplicationParty.courtApplicationParty()
                .withId(applicant.getId())
                .withFirstName(applicant.getFirstName())
                .withLastName(applicant.getLastName())
                .withIsRespondent(false)
                .withCourtApplicationPartyType(buildCourtApplicationPartyType(applicant.getCourtApplicationPartyType()))
                .withAddress(buildAddress(ofNullable(applicant.getAddress())))
                .build();
    }

    private CourtApplicationParty getRespondent(final Respondents respondents) {
        return isNull(respondents) ? null : CourtApplicationParty.courtApplicationParty()
                .withId(respondents.getId())
                .withFirstName(respondents.getFirstName())
                .withLastName(respondents.getLastName())
                .withIsRespondent(true)
                .withCourtApplicationPartyType(buildCourtApplicationPartyType(respondents.getCourtApplicationPartyType()))
                .withAddress(buildAddress(ofNullable(respondents.getAddress())))
                .build();
    }

    private CourtApplicationParty getSubject(final Subject subject) {
        return isNull(subject) ? null : CourtApplicationParty.courtApplicationParty()
                .withId(subject.getId())
                .withFirstName(subject.getFirstName())
                .withLastName(subject.getLastName())
                .withIsRespondent(false)
                .withCourtApplicationPartyType(buildCourtApplicationPartyType(subject.getCourtApplicationPartyType()))
                .withAddress(buildAddress(ofNullable(subject.getAddress())))
                .build();
    }

    private CourtApplicationPartyType buildCourtApplicationPartyType(uk.gov.justice.listing.courts.CourtApplicationPartyType courtApplicationPartyType) {
        return CourtApplicationPartyType.valueOf(courtApplicationPartyType.name());
    }

    private List<uk.gov.moj.cpp.listing.domain.Offence> buildOffences(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        final ArrayList<uk.gov.moj.cpp.listing.domain.Offence> offences = new ArrayList<>();
        if(isNotEmpty(commandCourtApplication.getCourtApplicationCases())) {
            commandCourtApplication.getCourtApplicationCases().stream().filter(ca -> nonNull(ca.getOffences())).forEach(courtApplicationCase ->
                offences.addAll(courtApplicationCase.getOffences().stream().map(this::getDomainOffence).collect(Collectors.toList()))
            );
        }
        if(nonNull(commandCourtApplication.getCourtOrder())) {
            commandCourtApplication.getCourtOrder().getCourtOrderOffences().stream().filter(coo -> nonNull(coo.getOffence())).forEach(courtOrderOffence -> offences.add(getDomainOffence(courtOrderOffence.getOffence())));
        }

        return offences;
    }

    private uk.gov.moj.cpp.listing.domain.Offence getDomainOffence(uk.gov.justice.core.courts.Offence offence) {
        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(offence.getId())
                .withStartDate(offence.getStartDate())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceWording(offence.getWording())
                .withCount(offence.getCount())
                .withOrderIndex(offence.getOrderIndex())
                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                        .withTitle(offence.getOffenceTitle())
                        .withWelshTitle(StringUtils.isNotEmpty(offence.getOffenceTitleWelsh()) ? offence.getOffenceTitleWelsh() : offence.getOffenceTitle())
                        .withLegislation(ofNullable(offence.getOffenceLegislation()))
                        .withWelshLegislation(ofNullable(offence.getOffenceLegislationWelsh()))
                        .build())
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.Offence getDomainOffence(uk.gov.justice.listing.events.Offence offence) {
        return uk.gov.moj.cpp.listing.domain.Offence.offence().withId(offence.getId())
                .withStartDate(offence.getStartDate())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceWording(offence.getOffenceWording())
                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                        .withTitle(offence.getStatementOfOffence().getTitle())
                        .withWelshTitle(offence.getStatementOfOffence().getWelshTitle())
                        .withLegislation(ofNullable(offence.getStatementOfOffence().getLegislation()))
                        .withWelshLegislation(ofNullable(offence.getStatementOfOffence().getWelshLegislation()))
                        .build())
                .build();
    }
}


