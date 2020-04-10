package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.LaaReference;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

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
                .withMasterDefendantId(d.getMasterDefendantId())
                .withCourtProceedingsInitiated(d.getCourtProceedingsInitiated())
                .withBailStatus(d.getBailStatus().map(bailStatus -> new BailStatus.Builder().withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).withId(bailStatus.getId()).build()))
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
                .withIsYouth(d.getIsYouth())
                .withAddress(nonNull(d.getAddress()) && d.getAddress().isPresent() ? buildAddress(d.getAddress()) : empty())
                .withNationalityDescription(nonNull(d.getNationalityDescription()) && d.getNationalityDescription().isPresent() ? d.getNationalityDescription() : empty())

                .build();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.Address> buildAddress(Optional<uk.gov.justice.core.courts.Address> a) {

        return Optional.of(uk.gov.moj.cpp.listing.domain.Address.address().
                withAddress1(a.get().getAddress1())
                .withAddress2(a.get().getAddress2().isPresent() ? a.get().getAddress2() : empty())
                .withAddress3(a.get().getAddress3().isPresent() ? a.get().getAddress3() : empty())
                .withAddress4(a.get().getAddress4().isPresent() ? a.get().getAddress4() : empty())
                .withAddress5(a.get().getAddress5().isPresent() ? a.get().getAddress5() : empty())
                .withPostcode(a.get().getPostcode().isPresent() ? a.get().getPostcode() : empty())
                .build());

    }

    private uk.gov.moj.cpp.listing.domain.Offence buildOffence(final Offence o) {
        final Optional<uk.gov.justice.listing.events.LaaReference> laaApplnReference =
                o.getLaaApplnReference();
        return uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withLaaApplnReference(laaApplnReference.isPresent() ?
                        buildLaaReference(laaApplnReference.get()) :
                        empty())
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

    private Optional<LaaReference> buildLaaReference(
            final uk.gov.justice.listing.events.LaaReference laaReference) {

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


