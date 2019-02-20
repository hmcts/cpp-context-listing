package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;

public class CommandOffenceToDomainOffence implements Converter<List<Offence>, List<uk.gov.moj.cpp.listing.domain.Offence>> {

    public List<uk.gov.moj.cpp.listing.domain.Offence> convert(final List<Offence> commandOffences) {

        return commandOffences
                .stream()
                .map(this::convertToDomainOffence)
                .collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence convertToDomainOffence(final Offence commandOffence) {

        StatementOfOffence statementOfOffence = null;
        if (commandOffence.getStatementOfOffence() != null) {
            uk.gov.justice.listing.commands.StatementOfOffence commandSoo = commandOffence.getStatementOfOffence();

            statementOfOffence = StatementOfOffence.statementOfOffence()
                    .withTitle(commandSoo.getTitle())
                    .withWelshTitle(commandSoo.getWelshTitle())
                    .withLegislation(commandSoo.getLegislation())
                    .withWelshLegislation(commandSoo.getWelshLegislation())
                    .build();
        }

        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(commandOffence.getId())
                .withOffenceCode(commandOffence.getOffenceCode())
                .withStartDate(commandOffence.getStartDate())
                .withEndDate(commandOffence.getEndDate())
                .withStatementOfOffence(statementOfOffence)
                .withOffenceWording(commandOffence.getOffenceWording())
                .build();
    }

}


