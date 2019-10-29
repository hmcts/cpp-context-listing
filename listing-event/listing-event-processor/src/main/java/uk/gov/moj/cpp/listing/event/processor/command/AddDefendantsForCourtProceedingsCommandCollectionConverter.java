package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.Optional;

public class AddDefendantsForCourtProceedingsCommandCollectionConverter implements Converter<DefendantsToBeAddedForCourtProceedings, List<AddDefendantsForCourtProceedingsCommand>> {

    @Override
    public List<AddDefendantsForCourtProceedingsCommand>  convert(final DefendantsToBeAddedForCourtProceedings event) {

        final List <Defendant> defendants = convertDefendants(event.getDefendants());
        return event.getHearings().stream().map(hearingId ->
                new AddDefendantsForCourtProceedingsCommand(event.getCaseId(), defendants, hearingId)).collect(toList());
    }

    private List<Defendant> convertDefendants(List<uk.gov.justice.listing.events.Defendant> defendants) {
        return defendants.stream().map(defendant ->
                Defendant.defendant()
                    .withSpecificRequirements(defendant.getSpecificRequirements())
                    .withOrganisationName(defendant.getOrganisationName())
                    .withHearingLanguageNeeds(defendant.getHearingLanguageNeeds().map(hearingLanguageNeeds ->
                            HearingLanguageNeeds.valueOf(hearingLanguageNeeds.toString())))
                    .withLastName(defendant.getLastName())
                    .withFirstName(defendant.getFirstName())
                    .withDefenceOrganisation(defendant.getDefenceOrganisation())
                    .withDatesToAvoid(defendant.getDatesToAvoid())
                    .withDateOfBirth(defendant.getDateOfBirth())
                    .withCustodyTimeLimit(defendant.getCustodyTimeLimit())
                    .withBailStatus(defendant.getBailStatus().map(bailStatus ->
                            Optional.of(new BailStatus.Builder().withId(bailStatus.getId()).withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).build())).orElse(Optional.empty()))
                    .withOffences(defendant.getOffences().stream()
                            .map(this::buildOffence)
                            .collect(toList()))
                    .withId(defendant.getId())
                    .build())
                .collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence buildOffence(uk.gov.justice.listing.events.Offence o) {
        return Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .build();
    }
    private StatementOfOffence buildStatementOfOffence(final uk.gov.justice.listing.events.Offence offence) {
        return StatementOfOffence.statementOfOffence()
                .withTitle(offence.getStatementOfOffence().getTitle())
                .withLegislation(offence.getStatementOfOffence().getLegislation())
                .withWelshLegislation(offence.getStatementOfOffence().getWelshLegislation())
                .withWelshTitle(offence.getStatementOfOffence().getWelshTitle())
                .build();
    }
}
