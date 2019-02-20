package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import uk.gov.justice.listing.courts.Defendant;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;

@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity"})
public class CourtsDefendantToDomainConverter implements Converter<uk.gov.justice.listing.courts.Defendant, uk.gov.moj.cpp.listing.domain.Defendant> {

    @Override
    public uk.gov.moj.cpp.listing.domain.Defendant convert(final uk.gov.justice.listing.courts.Defendant courtsDefendant) {
        return buildDefendants(courtsDefendant);
    }

    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendants(Defendant d) {
        return uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(d.getId())
                .withHearingLanguageNeeds(empty())
                .withFirstName(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getFirstName().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getFirstName().get()) : empty())
                .withLastName(d.getPersonDefendant().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getLastName()) : empty())
                .withBailStatus(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getBailStatus().isPresent()
                        ? BailStatus.valueFor(d.getPersonDefendant().get().getBailStatus().get().toString()) : empty())
                .withDefenceOrganisation(d.getDefenceOrganisation().isPresent() ? of(d.getDefenceOrganisation().get().getName()) : empty())
                .withOrganisationName(d.getLegalEntityDefendant().isPresent() ? of(d.getLegalEntityDefendant().get().getOrganisation().getName()) : empty())
                .withSpecificRequirements(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getSpecificRequirements() : empty())
                .withDateOfBirth(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getDateOfBirth() : empty())
                .withCustodyTimeLimit(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getCustodyTimeLimit() : empty())
                .withOffences(emptyList())
                .build();
    }

}


