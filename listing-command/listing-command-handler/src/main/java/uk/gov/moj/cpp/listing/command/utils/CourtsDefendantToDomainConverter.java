package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.listing.courts.Defendant;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity"})
public class CourtsDefendantToDomainConverter implements Converter<uk.gov.justice.listing.courts.Defendant, uk.gov.moj.cpp.listing.domain.Defendant> {

    @Override
    public uk.gov.moj.cpp.listing.domain.Defendant convert(final uk.gov.justice.listing.courts.Defendant courtsDefendant) {
        return buildDefendants(courtsDefendant);
    }

    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendants(final uk.gov.justice.listing.courts.Defendant d) {
        return uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(Objects.nonNull(d.getMasterDefendantId()) ? ofNullable(d.getMasterDefendantId()) : empty())
                .withCourtProceedingsInitiated(empty())
                .withHearingLanguageNeeds(empty())
                .withFirstName(nonNull(d.getPersonDefendant()) && nonNull(d.getPersonDefendant().getPersonDetails()) ? ofNullable(d.getPersonDefendant().getPersonDetails().getFirstName()) : empty())
                .withLastName(nonNull(d.getPersonDefendant())? ofNullable(d.getPersonDefendant().getPersonDetails().getLastName()) : empty())
                .withBailStatus(mapBailStatus(d))
                .withDefenceOrganisation(nonNull(d.getDefenceOrganisation()) ? ofNullable(d.getDefenceOrganisation().getName()) : empty())
                .withOrganisationName(nonNull(d.getLegalEntityDefendant()) ? ofNullable(d.getLegalEntityDefendant().getOrganisation().getName()) : empty())
                .withSpecificRequirements(nonNull(d.getPersonDefendant()) ? ofNullable(d.getPersonDefendant().getPersonDetails().getSpecificRequirements()) : empty())
                .withDateOfBirth(nonNull(d.getPersonDefendant())
                        && nonNull(d.getPersonDefendant().getPersonDetails())
                        && nonNull(d.getPersonDefendant().getPersonDetails().getDateOfBirth()) ? Optional.of(d.getPersonDefendant().getPersonDetails().getDateOfBirth()) : empty())
                .withCustodyTimeLimit(nonNull(d.getPersonDefendant())
                        && nonNull(d.getPersonDefendant().getCustodyTimeLimit()) ? Optional.of(d.getPersonDefendant().getCustodyTimeLimit()) : empty())
                .withOffences(emptyList())
                .withIsYouth(ofNullable(d.getIsYouth()))
                .withAddress(buildAddress(d))
                .withNationalityDescription(nonNull(d.getPersonDefendant()) && nonNull(d.getPersonDefendant().getPersonDetails().getNationalityDescription()) ? ofNullable(d.getPersonDefendant().getPersonDetails().getNationalityDescription()) : empty())
                .build();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.Address> buildAddress(Defendant defendant) {
        Optional<Address> d = empty();

        if (nonNull(defendant) && nonNull(defendant.getPersonDefendant())) {
            d = ofNullable(defendant.getPersonDefendant().getPersonDetails().getAddress());

        } else if (nonNull(defendant) && nonNull(defendant.getLegalEntityDefendant())) {
            d = ofNullable(defendant.getLegalEntityDefendant().getOrganisation().getAddress());
        }

        return Optional.of(uk.gov.moj.cpp.listing.domain.Address.address().
                withAddress1(d.isPresent() ? d.get().getAddress1() : "")
                .withAddress2(d.map(Address::getAddress2))
                .withAddress3(d.map(Address::getAddress3))
                .withAddress4(d.map(Address::getAddress4))
                .withAddress5(d.map(Address::getAddress5))
                .withPostcode(d.map(Address::getPostcode))
                .build());

    }

    private Optional<BailStatus> mapBailStatus(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())) {
            final Optional<uk.gov.justice.core.courts.BailStatus> optBailStatus = ofNullable(defendant.getPersonDefendant().getBailStatus());
            return optBailStatus.map(bailStatus -> new BailStatus.Builder().withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).withId(bailStatus.getId()).build());
        }
        return empty();
    }

}


