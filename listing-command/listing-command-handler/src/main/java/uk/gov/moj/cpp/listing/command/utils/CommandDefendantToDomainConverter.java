package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;

import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;

import java.util.List;
import java.util.stream.Collectors;


public class CommandDefendantToDomainConverter implements Converter<List<Defendant>, List<uk.gov.moj.cpp.listing.domain.Defendant>> {

    @Override
    public List<uk.gov.moj.cpp.listing.domain.Defendant> convert(final List<Defendant> commandDefendants) {
        return commandDefendants.stream().map(this::buildDefendants).collect(Collectors.toList());
    }

    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendants(Defendant d) {
        return uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(d.getId())
                .withBailStatus(d.getBailStatus().map(bailStatus -> (BailStatus.valueFor(bailStatus.toString())).get()))
                .withCustodyTimeLimit(d.getCustodyTimeLimit())
                .withDateOfBirth(d.getDateOfBirth())
                .withDatesToAvoid(d.getDatesToAvoid())
                .withDefenceOrganisation(d.getDefenceOrganisation())
                .withFirstName(d.getFirstName())
                .withLastName(d.getLastName())
                .withHearingLanguageNeeds(d.getHearingLanguageNeeds().map(hearingLanguageNeeds -> HearingLanguageNeeds.valueOf(hearingLanguageNeeds.toString())))
                .withOrganisationName(d.getOrganisationName())
                .withSpecificRequirements(d.getSpecificRequirements())
                .withOffences(convertOffences())
                .build();
    }

    private List<uk.gov.moj.cpp.listing.domain.Offence> convertOffences() {
        // Offences are not being updated as part of this event flow originating from
        // event public.progression.events.defendant-updated
        return emptyList();
    }

}


