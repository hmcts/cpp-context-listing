package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.LaaReference;
import uk.gov.moj.cpp.listing.domain.CustodyTimeLimit;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S3655"})
public class CommandOffenceToDomainOffence implements Converter<List<Offence>, List<uk.gov.moj.cpp.listing.domain.Offence>> {

    public List<uk.gov.moj.cpp.listing.domain.Offence> convert(final List<Offence> commandOffences) {

        return commandOffences
                .stream()
                .map(this::convertToDomainOffence)
                .collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence convertToDomainOffence(final Offence commandOffence) {

        final Optional<uk.gov.justice.listing.commands.LaaReference> laaReference =
                commandOffence.getLaaApplnReference();
        StatementOfOffence statementOfOffence = null;
        if (commandOffence.getStatementOfOffence() != null) {
            final uk.gov.justice.listing.commands.StatementOfOffence commandSoo = commandOffence.getStatementOfOffence();

            statementOfOffence = StatementOfOffence.statementOfOffence()
                    .withTitle(commandSoo.getTitle())
                    .withWelshTitle(commandSoo.getWelshTitle())
                    .withLegislation(commandSoo.getLegislation())
                    .withWelshLegislation(commandSoo.getWelshLegislation())
                    .build();
        }

        Optional<CustodyTimeLimit> custodyTimeLimit = Optional.empty();
        final Optional<uk.gov.justice.listing.commands.CustodyTimeLimit> commandSoo = commandOffence.getCustodyTimeLimit();

        if (commandSoo.isPresent()){
             custodyTimeLimit = Optional.ofNullable(CustodyTimeLimit.custodyTimeLimit()
                    .withTimeLimit(commandSoo.get().getTimeLimit())
                    .withDaysSpent(commandSoo.get().getDaysSpent())
                    .build());
        }

        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(commandOffence.getId())
                .withOffenceCode(commandOffence.getOffenceCode())
                .withStartDate(commandOffence.getStartDate())
                .withEndDate(commandOffence.getEndDate())
                .withStatementOfOffence(statementOfOffence)
                .withOffenceWording(commandOffence.getOffenceWording())
                .withCustodyTimeLimit(custodyTimeLimit)
                .withLaaApplnReference(laaReference.isPresent() ? buildLaaReference((laaReference.get())) : empty())
                .build();
    }

    private Optional<LaaReference> buildLaaReference(final uk.gov.justice.listing.commands.LaaReference laaReference) {

        return Optional.of(uk.gov.moj.cpp.listing.domain.LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate((laaReference.getEffectiveEndDate()))
                .withEffectiveStartDate((laaReference.getEffectiveStartDate()))
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusId(laaReference.getStatusId())
                .build());
    }

}


