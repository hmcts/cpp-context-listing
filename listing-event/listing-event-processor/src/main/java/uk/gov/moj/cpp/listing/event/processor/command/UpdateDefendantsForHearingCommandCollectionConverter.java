package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S1067"})
public class UpdateDefendantsForHearingCommandCollectionConverter implements Converter<DefendantsToBeUpdated, List<UpdateDefendantsForHearingCommand>> {

    public static Optional<uk.gov.moj.cpp.listing.domain.Address> buildAddress(Optional<uk.gov.justice.core.courts.Address> address) {

        if (nonNull(address) && address.isPresent()) {
            return Optional.of(uk.gov.moj.cpp.listing.domain.Address.address().
                    withAddress1(nonNull(address.get().getAddress1()) ? address.get().getAddress1() : "")
                    .withAddress2(ofNullable(address.get().getAddress2()))
                    .withAddress3(ofNullable(address.get().getAddress3()))
                    .withAddress4(ofNullable(address.get().getAddress4()))
                    .withAddress5(ofNullable(address.get().getAddress5()))
                    .withPostcode(ofNullable(address.get().getPostcode()))
                    .build());
        }
        return empty();
    }

    @Override
    public List<UpdateDefendantsForHearingCommand> convert(final DefendantsToBeUpdated event) {

        final List<Defendant> defendants = convertDefendants(event.getDefendants());
        return event.getHearings().stream().map(hearingId ->
                new UpdateDefendantsForHearingCommand(event.getCaseId(), defendants, hearingId)).collect(toList());
    }

    public List<Defendant> convertDefendants(final List<uk.gov.justice.listing.events.Defendant> defendants) {
        return defendants.stream().map(defendant -> Defendant.defendant()
                        .withSpecificRequirements(ofNullable(defendant.getSpecificRequirements()))
                        .withOrganisationName(ofNullable(defendant.getOrganisationName()))
                        .withHearingLanguageNeeds(ofNullable(defendant.getHearingLanguageNeeds()).map(hearingLanguageNeeds ->
                                HearingLanguageNeeds.valueOf(hearingLanguageNeeds.toString())))
                        .withLastName(ofNullable(defendant.getLastName()))
                        .withFirstName(ofNullable(defendant.getFirstName()))
                        .withDefenceOrganisation(ofNullable(defendant.getDefenceOrganisation()))
                        .withDatesToAvoid(ofNullable(defendant.getDatesToAvoid()))
                        .withDateOfBirth(ofNullable(defendant.getDateOfBirth()))
                        .withCustodyTimeLimit(ofNullable(defendant.getCustodyTimeLimit()))
                        .withBailStatus(ofNullable(defendant.getBailStatus()).map(bailStatus -> new BailStatus.Builder().withId(bailStatus.getId()).withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).build()))
                        .withOffences(emptyList())
                        .withId(defendant.getId())
                        .withMasterDefendantId(ofNullable(defendant.getMasterDefendantId()))
                        .withIsYouth(ofNullable(defendant.getIsYouth()))
                        .withAddress(nonNull(defendant.getAddress()) ? buildAddress(ofNullable(defendant.getAddress())) : empty())
                        .withNationalityDescription(ofNullable(defendant.getNationalityDescription()))
                        .build()).collect(toList());
    }
}
