package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.CustodyTimeLimit;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S3655"})
public class CourtsOffenceToDomainOffence implements Converter<List<Offence>, List<uk.gov.moj.cpp.listing.domain.Offence>> {

    public List<uk.gov.moj.cpp.listing.domain.Offence> convert(final List<Offence> courtOffences) {

        return courtOffences
                .stream()
                .map(this::convertToDomainOffence)
                .collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence convertToDomainOffence(final Offence courtOffence) {

        StatementOfOffence statement = StatementOfOffence.statementOfOffence()
                .withTitle(courtOffence.getOffenceTitle())
                .withWelshTitle(courtOffence.getOffenceTitleWelsh().orElse(courtOffence.getOffenceTitle()))
                .withLegislation(courtOffence.getOffenceLegislation())
                .withWelshLegislation(courtOffence.getOffenceLegislationWelsh())
                .build();

        Optional<CustodyTimeLimit> custodyTimeLimit = Optional.empty();
        if (courtOffence.getCustodyTimeLimit().isPresent()) {
            custodyTimeLimit = Optional.ofNullable(CustodyTimeLimit.custodyTimeLimit()
                    .withTimeLimit(courtOffence.getCustodyTimeLimit().get().getTimeLimit())
                    .withDaysSpent(courtOffence.getCustodyTimeLimit().get().getDaysSpent().orElse(null))

                    .build());
        }

        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(courtOffence.getId())
                .withOffenceCode(courtOffence.getOffenceCode())
                .withStartDate(courtOffence.getStartDate())
                .withEndDate(courtOffence.getEndDate())
                .withStatementOfOffence(statement)
                .withOffenceWording(courtOffence.getWording())
                .withCustodyTimeLimit(custodyTimeLimit)
                .build();
    }

}


