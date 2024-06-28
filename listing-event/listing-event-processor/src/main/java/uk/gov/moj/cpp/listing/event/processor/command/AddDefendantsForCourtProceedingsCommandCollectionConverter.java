package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Optional.ofNullable;
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
    public List<AddDefendantsForCourtProceedingsCommand> convert(final DefendantsToBeAddedForCourtProceedings event) {

        final List<Defendant> defendants = convertDefendants(event.getDefendants());
        return event.getHearings().stream().map(hearingId ->
                new AddDefendantsForCourtProceedingsCommand(event.getCaseId(), defendants, hearingId)).collect(toList());
    }

    private List<Defendant> convertDefendants(final List<uk.gov.justice.listing.events.Defendant> defendants) {
        return defendants.stream().map(defendant ->
                Defendant.defendant()
                        .withSpecificRequirements(ofNullable(defendant.getSpecificRequirements()))
                        .withOrganisationName(ofNullable(defendant.getOrganisationName()))
                        .withHearingLanguageNeeds(ofNullable(defendant.getHearingLanguageNeeds()).map(hearingLanguageNeeds ->
                                HearingLanguageNeeds.valueOf(hearingLanguageNeeds.toString())))
                        .withLastName(ofNullable(defendant.getLastName()))
                        .withFirstName(ofNullable(defendant.getFirstName()))
                        .withDefenceOrganisation(ofNullable(defendant.getDefenceOrganisation()))
                        .withDatesToAvoid(ofNullable(defendant.getDatesToAvoid()))
                        .withDateOfBirth(ofNullable(defendant.getDateOfBirth()))
                        .withCustodyTimeLimit(ofNullable(defendant.getCustodyTimeLimit()))
                        .withBailStatus(ofNullable(defendant.getBailStatus()).map(bailStatus -> new BailStatus.Builder().withId(bailStatus.getId()).withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).build()))
                        .withOffences(defendant.getOffences().stream().map(this::buildOffence).collect(toList()))
                        .withId(defendant.getId())
                        .withIsYouth(Optional.ofNullable(defendant.getIsYouth()))
                        .withMasterDefendantId(ofNullable(defendant.getMasterDefendantId()))
                        .withCourtProceedingsInitiated(ofNullable(defendant.getCourtProceedingsInitiated()))
                        .build()).collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence buildOffence(final uk.gov.justice.listing.events.Offence o) {
        return Offence.offence()
                .withId(o.getId())
                .withEndDate(ofNullable(o.getEndDate()))
                .withStartDate(o.getStartDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .build();
    }

    private StatementOfOffence buildStatementOfOffence(final uk.gov.justice.listing.events.Offence offence) {
        return StatementOfOffence.statementOfOffence()
                .withTitle(offence.getStatementOfOffence().getTitle())
                .withLegislation(ofNullable(offence.getStatementOfOffence().getLegislation()))
                .withWelshLegislation(ofNullable(offence.getStatementOfOffence().getWelshLegislation()))
                .withWelshTitle(offence.getStatementOfOffence().getWelshTitle())
                .build();
    }
}
