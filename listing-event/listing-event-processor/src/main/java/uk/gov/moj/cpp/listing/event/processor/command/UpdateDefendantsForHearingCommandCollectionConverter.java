package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
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
                    .withAddress2(address.get().getAddress2().isPresent() ? address.get().getAddress2() : empty())
                    .withAddress3(address.get().getAddress3().isPresent() ? address.get().getAddress3() : empty())
                    .withAddress4(address.get().getAddress4().isPresent() ? address.get().getAddress4() : empty())
                    .withAddress5(address.get().getAddress5().isPresent() ? address.get().getAddress5() : empty())
                    .withPostcode(address.get().getPostcode().isPresent() ? address.get().getPostcode() : empty())
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

    private List<Defendant> convertDefendants(final List<uk.gov.justice.listing.events.Defendant> defendants) {
        return defendants.stream().map(defendant -> Defendant.defendant()
                        .withSpecificRequirements(defendant.getSpecificRequirements())
                        .withOrganisationName(defendant.getOrganisationName())
                        .withHearingLanguageNeeds(defendant.getHearingLanguageNeeds().map(hearingLanguageNeeds ->
                                HearingLanguageNeeds.valueOf(hearingLanguageNeeds.toString())))
                        .withLastName(defendant.getLastName())
                        .withFirstName(defendant.getFirstName())
                        .withDefenceOrganisation(defendant.getDefenceOrganisation())
                        .withDatesToAvoid(defendant.getDatesToAvoid())
                        .withDateOfBirth(defendant.getDateOfBirth())
                        .withCustodyTimeLimit(defendant.getCustodyTimeLimit())
                        .withBailStatus(defendant.getBailStatus().map(bailStatus ->
                                Optional.of(new BailStatus.Builder().withId(bailStatus.getId()).withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).build())).orElse(Optional.empty()))
                        .withOffences(emptyList())
                        .withId(defendant.getId())
                        .withMasterDefendantId(defendant.getMasterDefendantId())
                        .withIsYouth(defendant.getIsYouth())
                        .withAddress(nonNull(defendant.getAddress()) && defendant.getAddress().isPresent() ? buildAddress(defendant.getAddress()) : empty())
                        .withNationalityDescription(defendant.getNationalityDescription())
                        .build()).collect(toList());
    }
}
