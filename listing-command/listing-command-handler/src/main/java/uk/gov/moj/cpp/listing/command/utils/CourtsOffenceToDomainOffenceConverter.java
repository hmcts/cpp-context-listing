package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public abstract class CourtsOffenceToDomainOffenceConverter {

    protected CaseOffences createOffences(UUID caseId, UUID defendantId, List<uk.gov.justice.core.courts.Offence> offencesToBeConverted) {

        List<Offence> offences = convertOffence(offencesToBeConverted);

        return CaseOffences.createCaseOffencesBuilder()
                .setCaseId(caseId)
                .setDefendantId(defendantId)
                .setOffences(offences)
                .build();
    }

    private List<uk.gov.moj.cpp.listing.domain.Offence> convertOffence(final List<uk.gov.justice.core.courts.Offence> courtOffences) {

        return courtOffences
                .stream()
                .map(this::convertToDomainOffence)
                .collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence convertToDomainOffence(final uk.gov.justice.core.courts.Offence courtOffence) {

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
