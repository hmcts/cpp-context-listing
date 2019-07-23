package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

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
                .withOffences(d.getOffences().stream()
                        .map(this::buildOffence)
                        .collect(toList()))
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.Offence buildOffence(final Offence o) {
        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .build();
    }

    private StatementOfOffence buildStatementOfOffence(final Offence offence) {
        return StatementOfOffence.statementOfOffence()
                .withTitle(offence.getStatementOfOffence().getTitle())
                .withLegislation(offence.getStatementOfOffence().getLegislation())
                .withWelshLegislation(offence.getStatementOfOffence().getWelshLegislation())
                .withWelshTitle(offence.getStatementOfOffence().getWelshTitle())
                .build();
    }

}


