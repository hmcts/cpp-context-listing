package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CustodyTimeLimit;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@SuppressWarnings({"squid:S3655"})
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

        final Optional<LaaReference> laaReference = courtOffence.getLaaApplnReference();
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
                .withLaaApplnReference(laaReference.isPresent() ? buildLaaReference((laaReference.get())) : empty())
                .build();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.LaaReference> buildLaaReference(final LaaReference laaReference) {

        return Optional.of(uk.gov.moj.cpp.listing.domain.LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate(laaReference.getEffectiveEndDate())
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusId(laaReference.getStatusId())
                .build());
    }

}
