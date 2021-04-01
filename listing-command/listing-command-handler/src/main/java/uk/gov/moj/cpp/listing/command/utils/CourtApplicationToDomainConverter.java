package uk.gov.moj.cpp.listing.command.utils;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.ORGANISATION;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON_DEFENDANT;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PROSECUTING_AUTHORITY;

import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.listing.courts.Applicant;
import uk.gov.justice.listing.courts.Respondents;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.Address;
import uk.gov.moj.cpp.listing.domain.ApplicantRespondent;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"squid:UnusedPrivateMethod"})
public class CourtApplicationToDomainConverter implements Converter<uk.gov.justice.core.courts.CourtApplication, CourtApplication> {

    @Override
    public CourtApplication convert(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return CourtApplication.courtApplication()
                .withId(commandCourtApplication.getId())
                .withLinkedCaseIds(getCaseIds(commandCourtApplication))
                .withParentApplicationId(commandCourtApplication.getParentApplicationId().orElse(null))
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
                .build();
    }

    private List<UUID> getCaseIds(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        if(isNotEmpty(commandCourtApplication.getCourtApplicationCases())) {
            return commandCourtApplication.getCourtApplicationCases().stream().map(CourtApplicationCase::getProsecutionCaseId).collect(Collectors.toList());
        }
        final Optional<CourtOrder> courtOrder = commandCourtApplication.getCourtOrder();
        return courtOrder.map(order -> order.getCourtOrderOffences().stream().map(CourtOrderOffence::getProsecutionCaseId).collect(toList())).orElseGet(ArrayList::new);
    }

    private Optional<String> getApplicationReference(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return commandCourtApplication.getApplicationReference().isPresent() ? commandCourtApplication.getApplicationReference() : empty();
    }

    private Optional<String> getApplicationParticulars(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return commandCourtApplication.getApplicationParticulars().isPresent() ? commandCourtApplication.getApplicationParticulars() : empty();
    }

    public CourtApplication convertListingCoreCourtApplication(final uk.gov.justice.listing.courts.CourtApplication listingCoreCourtApplication) {
        return CourtApplication.courtApplication()
                .withId(listingCoreCourtApplication.getId())
                .withLinkedCaseIds(listingCoreCourtApplication.getLinkedCaseIds())
                .withParentApplicationId(listingCoreCourtApplication.getParentApplicationId().orElse(null))
                .withApplicant(getApplicant(listingCoreCourtApplication.getApplicant()))
                .withRespondents(ofNullable(listingCoreCourtApplication.getRespondents())
                        .map(respondents -> respondents
                                .stream()
                                .map(this::getRespondent)
                                .filter(Objects::nonNull)
                                .collect(toList()))
                        .orElse(null))
                .withApplicationType(listingCoreCourtApplication.getApplicationType())
                .withApplicationReference(listingCoreCourtApplication.getApplicationReference())
                .withApplicationParticulars(listingCoreCourtApplication.getApplicationParticulars())
                .build();
    }

    private ApplicantRespondent buildRespondent(final CourtApplicationParty courtApplicationParty) {
        return buildApplicantRespondent(courtApplicationParty, TRUE);
    }

    private ApplicantRespondent buildCourtApplicant(final CourtApplicationParty courtApplicationParty) {
        return buildApplicantRespondent(courtApplicationParty, FALSE);
    }

    private ApplicantRespondent buildApplicantRespondent(final CourtApplicationParty courtApplicationParty, final boolean isRespondent) {

        ApplicantRespondent applicantRespondent = courtApplicationParty.getPersonDetails()
                .map(person -> getApplicantRespondent(
                        courtApplicationParty.getId(),
                        isRespondent,
                        person.getFirstName(),
                        person.getLastName(),
                        PERSON,
                        person.getAddress()))
                .orElse(null);
        if (isNull(applicantRespondent)) {
            applicantRespondent = courtApplicationParty.getMasterDefendant()
                    .map(defendant -> getApplicantRespondentForLegalEntityDefendant(
                            courtApplicationParty.getId(),
                            isRespondent,
                            defendant.getLegalEntityDefendant()))
                    .orElse(null);
        }

        if (isNull(applicantRespondent)) {
            applicantRespondent = courtApplicationParty.getOrganisation()
                    .map(organisation -> getApplicantRespondent(
                            courtApplicationParty.getId(),
                            isRespondent, empty(),
                            organisation.getName(),
                            ORGANISATION,
                            organisation.getAddress()))
                    .orElse(null);
        }
        if (isNull(applicantRespondent)) {
            applicantRespondent = courtApplicationParty.getProsecutingAuthority()
                    .map(prosecutingAuthority -> getApplicantRespondent(
                            courtApplicationParty.getId(),
                            isRespondent,
                            empty(),
                            prosecutingAuthority.getProsecutionAuthorityCode(),
                            PROSECUTING_AUTHORITY,
                            prosecutingAuthority.getAddress()))
                    .orElse(null);
        }
        if (isNull(applicantRespondent)) {
            applicantRespondent = courtApplicationParty.getMasterDefendant()
                    .map(defendant -> getApplicantRespondent(
                            courtApplicationParty.getId(),
                            isRespondent,
                            defendant.getPersonDefendant()))
                    .orElse(null);
        }
        return applicantRespondent;
    }

    private ApplicantRespondent getApplicantRespondent(final UUID id, final boolean isRespondent, final Optional<PersonDefendant> personDefendant) {
        return personDefendant.map(defendant -> getApplicantRespondent(
                id,
                isRespondent,
                defendant.getPersonDetails().getFirstName(),
                defendant.getPersonDetails().getLastName(),
                PERSON_DEFENDANT,
                defendant.getPersonDetails().getAddress()))
                .orElse(null);
    }

    private ApplicantRespondent getApplicantRespondentForLegalEntityDefendant(final UUID id, final boolean isRespondent, final Optional<LegalEntityDefendant> legalEntityDefendant) {
        return legalEntityDefendant.isPresent() ? getApplicantRespondent(
                id,
                isRespondent,
                empty(),
                legalEntityDefendant.get().getOrganisation().getName(),
                PERSON,
                legalEntityDefendant.get().getOrganisation().getAddress()) : null;
    }

    private ApplicantRespondent getApplicantRespondent(final UUID id, final boolean isRespondent, final Optional<String> firstName, final String lastName, final CourtApplicationPartyType type, Optional<uk.gov.justice.core.courts.Address> address) {
        return ApplicantRespondent.applicantRespondent()
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
                .withAddress2(addr.getAddress2())
                .withAddress3(addr.getAddress3())
                .withAddress4(addr.getAddress4())
                .withAddress5(addr.getAddress5())
                .withPostcode(addr.getPostcode())
                .withWelshAddress1(addr.getWelshAddress1())
                .withWelshAddress2(addr.getWelshAddress2())
                .withWelshAddress3(addr.getWelshAddress3())
                .withWelshAddress4(addr.getWelshAddress4())
                .withWelshAddress5(addr.getWelshAddress5())
                .build())
                .orElse(null);
    }

    private ApplicantRespondent getApplicant(final Applicant applicant) {
        return isNull(applicant) ? null : ApplicantRespondent.applicantRespondent()
                .withId(applicant.getId())
                .withFirstName(applicant.getFirstName().orElse(null))
                .withLastName(applicant.getLastName())
                .withIsRespondent(false)
                .withCourtApplicationPartyType(buildCourtApplicationPartyType(applicant.getCourtApplicationPartyType()))
                .withAddress(buildAddress(applicant.getAddress()))
                .build();
    }

    private ApplicantRespondent getRespondent(final Respondents respondents) {
        return isNull(respondents) ? null : ApplicantRespondent.applicantRespondent()
                .withId(respondents.getId())
                .withFirstName(respondents.getFirstName().orElse(null))
                .withLastName(respondents.getLastName())
                .withIsRespondent(true)
                .withCourtApplicationPartyType(buildCourtApplicationPartyType(respondents.getCourtApplicationPartyType()))
                .withAddress(buildAddress(respondents.getAddress()))
                .build();
    }

    private CourtApplicationPartyType buildCourtApplicationPartyType(uk.gov.justice.listing.courts.CourtApplicationPartyType courtApplicationPartyType) {
        return CourtApplicationPartyType.valueOf(courtApplicationPartyType.name());
    }
}


