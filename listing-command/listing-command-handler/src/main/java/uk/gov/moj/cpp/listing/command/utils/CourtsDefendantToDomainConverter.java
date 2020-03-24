package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.listing.courts.Defendant;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;

import java.util.Optional;

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
                .withBailStatus(mapBailStatus(d))
                .withDefenceOrganisation(d.getDefenceOrganisation().isPresent() ? of(d.getDefenceOrganisation().get().getName()) : empty())
                .withOrganisationName(d.getLegalEntityDefendant().isPresent() ? of(d.getLegalEntityDefendant().get().getOrganisation().getName()) : empty())
                .withSpecificRequirements(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getSpecificRequirements() : empty())
                .withDateOfBirth(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getDateOfBirth() : empty())
                .withCustodyTimeLimit(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getCustodyTimeLimit() : empty())
                .withOffences(emptyList())
                .withIsYouth(d.getIsYouth().isPresent() ? d.getIsYouth():empty())
                .withAddress(buildAddress(d))
                .withNationalityDescription(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getNationalityDescription().isPresent() ?  d.getPersonDefendant().get().getPersonDetails().getNationalityDescription() : empty())
                .build();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.Address> buildAddress(Defendant defendant) {
        Optional<Address> d = empty();

        if (nonNull(defendant) && defendant.getPersonDefendant().isPresent()) {
            d = defendant.getPersonDefendant().get().getPersonDetails().getAddress();

        } else if (nonNull(defendant) && defendant.getLegalEntityDefendant().isPresent()) {
            d = defendant.getLegalEntityDefendant().get().getOrganisation().getAddress();
        }

        return Optional.of(uk.gov.moj.cpp.listing.domain.Address.address().
                withAddress1(d.isPresent() ? d.get().getAddress1() : "")
                .withAddress2(d.isPresent() ? d.get().getAddress2() : empty())
                .withAddress3(d.isPresent() ? d.get().getAddress3() : empty())
                .withAddress4(d.isPresent() ? d.get().getAddress4() : empty())
                .withAddress5(d.isPresent() ? d.get().getAddress5() : empty())
                .withPostcode(d.isPresent() ? d.get().getPostcode() : empty())
                .build());

    }

    private Optional<BailStatus> mapBailStatus(Defendant defendant){
        if(defendant.getPersonDefendant().isPresent()) {
            final Optional<uk.gov.justice.core.courts.BailStatus> optBailStatus = defendant.getPersonDefendant().map(PersonDefendant::getBailStatus).orElse(Optional.empty());
            return optBailStatus.map(bailStatus -> new BailStatus.Builder().withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).withId(bailStatus.getId()).build());
        }
        return empty();
    }

}


