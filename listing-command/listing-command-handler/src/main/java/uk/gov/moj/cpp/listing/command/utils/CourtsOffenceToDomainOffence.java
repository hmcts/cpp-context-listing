package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.CustodyTimeLimit;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({"squid:S3655"})
public class CourtsOffenceToDomainOffence implements Converter<List<Offence>, List<uk.gov.moj.cpp.listing.domain.Offence>> {

    public List<uk.gov.moj.cpp.listing.domain.Offence> convert(final List<Offence> courtOffences) {

        return courtOffences
                .stream()
                .map(this::convertToDomainOffence)
                .collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence convertToDomainOffence(final Offence courtOffence) {

        final StatementOfOffence statement = StatementOfOffence.statementOfOffence()
                .withTitle(courtOffence.getOffenceTitle())
                .withWelshTitle(StringUtils.isNotEmpty(courtOffence.getOffenceTitleWelsh()) ? courtOffence.getOffenceTitleWelsh() : courtOffence.getOffenceTitle())
                .withLegislation(ofNullable(courtOffence.getOffenceLegislation()))
                .withWelshLegislation(ofNullable(courtOffence.getOffenceLegislationWelsh()))
                .build();

        Optional<CustodyTimeLimit> custodyTimeLimit = Optional.empty();
        if (nonNull(courtOffence.getCustodyTimeLimit())) {
            custodyTimeLimit = Optional.ofNullable(CustodyTimeLimit.custodyTimeLimit()
                    .withTimeLimit(courtOffence.getCustodyTimeLimit().getTimeLimit())
                    .withDaysSpent(courtOffence.getCustodyTimeLimit().getDaysSpent())

                    .build());
        }

        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(courtOffence.getId())
                .withOffenceCode(courtOffence.getOffenceCode())
                .withStartDate(courtOffence.getStartDate())
                .withEndDate(ofNullable(courtOffence.getEndDate()))
                .withStatementOfOffence(statement)
                .withOffenceWording(courtOffence.getWording())
                .withCustodyTimeLimit(custodyTimeLimit)
                .build();
    }

}


