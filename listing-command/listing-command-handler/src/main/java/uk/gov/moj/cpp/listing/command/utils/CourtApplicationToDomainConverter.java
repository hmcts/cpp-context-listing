package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.listing.courts.Applicant;
import uk.gov.justice.listing.courts.Respondents;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.ApplicantRespondent;
import uk.gov.moj.cpp.listing.domain.CourtApplication;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import java.util.Objects;
import java.util.Optional;


@SuppressWarnings({"squid:S3655"})
public class CourtApplicationToDomainConverter implements Converter<uk.gov.justice.core.courts.CourtApplication, CourtApplication> {

    @Override
    public CourtApplication convert(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return CourtApplication.courtApplication()
                .withId(commandCourtApplication.getId())
                .withLinkedCaseId(commandCourtApplication.getLinkedCaseId().isPresent() ? commandCourtApplication.getLinkedCaseId().get() : null)
                .withParentApplicationId(commandCourtApplication.getParentApplicationId().isPresent() ? commandCourtApplication.getParentApplicationId().get() : null)
                .withApplicationType(commandCourtApplication.getType().getApplicationType())
                .withApplicant(buildCourtApplicant(commandCourtApplication.getApplicant()))
                .withRespondents(isNull(commandCourtApplication.getRespondents())? emptyList(): commandCourtApplication.getRespondents().stream().map(resp ->
                        buildRespondents(resp.getPartyDetails())).filter(Objects::nonNull).collect(toList()))
                .withApplicationReference(getApplicationReference(commandCourtApplication))
                .build();
    }

    private Optional<String> getApplicationReference(uk.gov.justice.core.courts.CourtApplication commandCourtApplication) {
        return commandCourtApplication.getApplicationReference().isPresent() ? commandCourtApplication.getApplicationReference() : Optional.empty();
    }

    public CourtApplication convertListingCoreCourtApplication(final uk.gov.justice.listing.courts.CourtApplication listingCoreCourtApplication){
        return CourtApplication.courtApplication()
                .withId(listingCoreCourtApplication.getId())
                .withLinkedCaseId(listingCoreCourtApplication.getLinkedCaseId().orElse(null))
                .withParentApplicationId(listingCoreCourtApplication.getParentApplicationId().orElse(null))
                .withApplicant(getApplicant(listingCoreCourtApplication.getApplicant()))
                .withRespondents(nonNull(listingCoreCourtApplication.getRespondents()) ? listingCoreCourtApplication.getRespondents().stream().map(this::getRespondent)
                        .filter(Objects::nonNull).collect(toList()): null)
                .withApplicationType(listingCoreCourtApplication.getApplicationType())
                .withApplicationReference(listingCoreCourtApplication.getApplicationReference())
                .build();
    }
    private ApplicantRespondent buildRespondents(final CourtApplicationParty courtApplicationParty) {
        return buildApplicantRespondent(courtApplicationParty, TRUE);
    }

    private ApplicantRespondent buildCourtApplicant(final CourtApplicationParty courtApplicationParty) {
        return buildApplicantRespondent(courtApplicationParty, FALSE);
    }
    private ApplicantRespondent buildApplicantRespondent
            (final CourtApplicationParty courtApplicationParty, final boolean isRespondent){
        ApplicantRespondent applicantRespondent = courtApplicationParty.getPersonDetails()
                .map(person-> getApplicantRespondent(isRespondent, person.getFirstName(), person.getLastName()))
                .orElse(null);
        if(Objects.isNull(applicantRespondent)){
            applicantRespondent = courtApplicationParty.getOrganisation()
                    .map(organisation -> getApplicantRespondent(isRespondent, Optional.empty(), organisation.getName())).orElse(null);
        }
        if(Objects.isNull(applicantRespondent)){
            applicantRespondent = courtApplicationParty.getProsecutingAuthority()
                    .map(prosecutingAuthority -> getApplicantRespondent(isRespondent, Optional.empty(), prosecutingAuthority.getProsecutionAuthorityCode()))
                    .orElse(null);
        }
        if(Objects.isNull(applicantRespondent)){
            applicantRespondent = courtApplicationParty.getDefendant()
                    .map(defendant -> getApplicantRespondent(isRespondent, defendant.getPersonDefendant())).orElse(null);
        }
        return applicantRespondent;
    }
    private ApplicantRespondent getApplicantRespondent(final boolean isRespondent, final Optional<PersonDefendant> personDefendant) {

        return personDefendant.isPresent() ? getApplicantRespondent(
                isRespondent,personDefendant.get().getPersonDetails().getFirstName(),
                personDefendant.get().getPersonDetails().getLastName() ) : null;
    }
    private ApplicantRespondent getApplicantRespondent(final boolean isRespondent,final Optional<String> firstName, final String lastName) {
        return ApplicantRespondent.applicantRespondent()
                .withFirstName(firstName.orElse(null))
                .withLastName(lastName)
                .withIsRespondent(isRespondent)
                .build();
    }
    private ApplicantRespondent getApplicant(final Applicant applicant) {
        return isNull(applicant) ? null :ApplicantRespondent.applicantRespondent()
                .withFirstName(applicant.getFirstName().orElse(null))
                .withLastName(applicant.getLastName())
                .withIsRespondent(false)
                .build();
    }
    private ApplicantRespondent getRespondent(final Respondents respondents) {
        return isNull(respondents)? null : ApplicantRespondent.applicantRespondent()
                .withFirstName(respondents.getFirstName().orElse(null))
                .withLastName(respondents.getLastName())
                .withIsRespondent(true)
                .build();
    }
}


