package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;

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

        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(courtOffence.getId())
                .withOffenceCode(courtOffence.getOffenceCode())
                .withStartDate(courtOffence.getStartDate())
                .withEndDate(courtOffence.getEndDate())
                .withStatementOfOffence(statement)
                .withOffenceWording(courtOffence.getWording())
                .build();
    }

}


