package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CustodyTimeLimit;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.aggregate.converter.ReportingRestrictionConverter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;


@SuppressWarnings({"squid:S3655"})
public abstract class CourtsOffenceToDomainOffenceConverter {


    protected CaseOffences createOffences(UUID caseId, UUID defendantId, List<uk.gov.justice.core.courts.Offence> offencesToBeConverted) {

        final List<Offence> offences = convertOffence(offencesToBeConverted);

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

        final LaaReference laaReference = courtOffence.getLaaApplnReference();
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

        final uk.gov.moj.cpp.listing.domain.Offence.Builder builder = uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(courtOffence.getId())
                .withOffenceCode(courtOffence.getOffenceCode())
                .withStartDate(courtOffence.getStartDate())
                .withEndDate(ofNullable(courtOffence.getEndDate()))
                .withCount(courtOffence.getCount())
                .withIndictmentParticular(courtOffence.getIndictmentParticular())
                .withOrderIndex(courtOffence.getOrderIndex())
                .withStatementOfOffence(statement)
                .withOffenceWording(courtOffence.getWording())
                .withCustodyTimeLimit(custodyTimeLimit)
                .withSeedingHearing(buildSeedingHearing(courtOffence.getSeedingHearing()))
                .withLaaApplnReference(buildLaaReference(laaReference));


        if (!isNull(courtOffence.getReportingRestrictions()) && !courtOffence.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(courtOffence.getReportingRestrictions().stream()
                    .map(ReportingRestrictionConverter::courtsToDomain)
                    .collect(toList())
            );
        }

        return builder.build();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.LaaReference> buildLaaReference(final LaaReference laaReference) {
        if(nonNull(laaReference)) {
            return Optional.of(uk.gov.moj.cpp.listing.domain.LaaReference.laaReference()
                    .withApplicationReference(laaReference.getApplicationReference())
                    .withEffectiveEndDate(ofNullable(laaReference.getEffectiveEndDate()))
                    .withEffectiveStartDate(ofNullable(laaReference.getEffectiveStartDate()))
                    .withStatusCode(laaReference.getStatusCode())
                    .withStatusDate(laaReference.getStatusDate())
                    .withStatusDescription(laaReference.getStatusDescription())
                    .withStatusId(laaReference.getStatusId())
                    .build());
        }
        return empty();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.SeedingHearing> buildSeedingHearing(final SeedingHearing seedingHearing) {
        if(nonNull(seedingHearing)) {
            return Optional.of(uk.gov.moj.cpp.listing.domain.SeedingHearing.seedingHearing()
                    .withJurisdictionType(JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()))
                    .withSittingDay(seedingHearing.getSittingDay())
                    .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                    .build());
        }
        return empty();
    }

}
