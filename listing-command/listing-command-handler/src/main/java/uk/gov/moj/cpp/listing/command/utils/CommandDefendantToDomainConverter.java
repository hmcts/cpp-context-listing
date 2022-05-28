package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.LaaReference;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.aggregate.converter.ReportingRestrictionConverter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@SuppressWarnings({"squid:S3655", "squid:S1067"})
public class CommandDefendantToDomainConverter implements Converter<List<Defendant>, List<uk.gov.moj.cpp.listing.domain.Defendant>> {

    @Override
    public List<uk.gov.moj.cpp.listing.domain.Defendant> convert(final List<Defendant> commandDefendants) {
        return commandDefendants.stream().map(this::buildDefendants).collect(Collectors.toList());
    }

    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendants(final Defendant d) {
        return uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(ofNullable(d.getMasterDefendantId()))
                .withCourtProceedingsInitiated(ofNullable(d.getCourtProceedingsInitiated()))
                .withBailStatus(nonNull(d.getBailStatus()) ? of(new BailStatus.Builder().withCode(d.getBailStatus().getCode()).withDescription(d.getBailStatus().getDescription()).withId(d.getBailStatus().getId()).build()) : empty())
                .withCustodyTimeLimit(ofNullable(d.getCustodyTimeLimit()))
                .withDateOfBirth(ofNullable(d.getDateOfBirth()))
                .withDatesToAvoid(ofNullable(d.getDatesToAvoid()))
                .withDefenceOrganisation(ofNullable(d.getDefenceOrganisation()))
                .withFirstName(ofNullable(d.getFirstName()))
                .withLastName(ofNullable(d.getLastName()))
                .withHearingLanguageNeeds(nonNull(d.getHearingLanguageNeeds()) ? HearingLanguageNeeds.valueFor(d.getHearingLanguageNeeds().name()) : empty())
                .withOrganisationName(ofNullable(d.getOrganisationName()))
                .withSpecificRequirements(ofNullable(d.getSpecificRequirements()))
                .withOffences(d.getOffences().stream()
                        .map(this::buildOffence)
                        .collect(toList()))
                .withIsYouth(ofNullable(d.getIsYouth()))
                .withAddress(buildAddress(ofNullable(d.getAddress())))
                .withNationalityDescription(nonNull(d.getNationalityDescription()) ? ofNullable(d.getNationalityDescription()) : empty())

                .build();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.Address> buildAddress(Optional<uk.gov.justice.core.courts.Address> a) {
        return a.map(address -> uk.gov.moj.cpp.listing.domain.Address.address().
                withAddress1(address.getAddress1())
                .withAddress2(ofNullable(address.getAddress2()))
                .withAddress3(ofNullable(address.getAddress3()))
                .withAddress4(ofNullable(address.getAddress4()))
                .withAddress5(ofNullable(address.getAddress5()))
                .withPostcode(ofNullable(address.getPostcode()))
                .build());
    }

    private uk.gov.moj.cpp.listing.domain.Offence buildOffence(final Offence o) {
        final uk.gov.justice.listing.events.LaaReference laaApplnReference = o.getLaaApplnReference();

        final uk.gov.moj.cpp.listing.domain.Offence.Builder builder = uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(o.getId())
                .withEndDate(ofNullable(o.getEndDate()))
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withLaaApplnReference(buildLaaReference(laaApplnReference));

        if (!isNull(o.getReportingRestrictions()) && !o.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(o.getReportingRestrictions().stream()
                    .map(ReportingRestrictionConverter::eventsToDomain)
                    .collect(toList())
            );
        }

        return builder.build();
    }

    private StatementOfOffence buildStatementOfOffence(final Offence offence) {
        if(nonNull(offence)) {
            return StatementOfOffence.statementOfOffence()
                    .withTitle(offence.getStatementOfOffence().getTitle())
                    .withLegislation(ofNullable(offence.getStatementOfOffence().getLegislation()))
                    .withWelshLegislation(ofNullable((offence.getStatementOfOffence().getWelshLegislation())))
                    .withWelshTitle(offence.getStatementOfOffence().getWelshTitle())
                    .build();
        }
        return null;
    }

    private Optional<LaaReference> buildLaaReference(final uk.gov.justice.listing.events.LaaReference laaReference) {
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

}


