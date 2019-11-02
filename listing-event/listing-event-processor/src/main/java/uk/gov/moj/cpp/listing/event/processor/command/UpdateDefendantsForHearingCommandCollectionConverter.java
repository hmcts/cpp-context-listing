package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;

import java.util.List;
import java.util.Optional;

public class UpdateDefendantsForHearingCommandCollectionConverter implements Converter<DefendantsToBeUpdated, List<UpdateDefendantsForHearingCommand> > {

    @Override
    public List<UpdateDefendantsForHearingCommand>  convert(final DefendantsToBeUpdated event) {

        final List <Defendant> defendants = convertDefendants(event.getDefendants());
        return event.getHearings().stream().map(hearingId ->
                new UpdateDefendantsForHearingCommand(event.getCaseId(), defendants, hearingId)).collect(toList());
    }

    private List<Defendant> convertDefendants(List<uk.gov.justice.listing.events.Defendant> defendants) {
        return defendants.stream().map(defendant ->
                Defendant.defendant()
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
                    .withOffences(convertOffences())
                    .withId(defendant.getId())
                    .withIsYouth(defendant.getIsYouth())
                    .build())
                .collect(toList());
    }

    private List<uk.gov.moj.cpp.listing.domain.Offence> convertOffences() {
        // Offences are not being updated as part of this event flow originating from
        // event public.progression.events.defendant-updated
        return emptyList();
    }
}
