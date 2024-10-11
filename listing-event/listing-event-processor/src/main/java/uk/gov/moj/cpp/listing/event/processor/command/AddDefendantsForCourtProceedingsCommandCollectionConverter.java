package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.ReportingRestriction;
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
                        .withAddress(nonNull(defendant.getAddress()) ? buildAddress(defendant.getAddress()):null)
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


    private uk.gov.moj.cpp.listing.domain.Offence buildOffence(final uk.gov.justice.listing.events.Offence offence) {

        final Offence.Builder builder =  Offence.offence()
                .withId(offence.getId())
                .withEndDate(ofNullable(offence.getEndDate()))
                .withStartDate(offence.getStartDate())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceWording(offence.getOffenceWording())
                .withStatementOfOffence(buildStatementOfOffence(offence))
                .withReportingRestrictions(nonNull(offence.getReportingRestrictions()) ? buildReportingRestrictions(offence) : null);

        if (nonNull(offence.getReportingRestrictions()) && !offence.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(buildReportingRestrictions(offence));
        }
        return builder.build();
    }

    private Optional<uk.gov.moj.cpp.listing.domain.Address> buildAddress(final  uk.gov.justice.core.courts.Address address) {
        return Optional.ofNullable(uk.gov.moj.cpp.listing.domain.Address.address()
                .withAddress1(address.getAddress1())
                .withAddress2(Optional.ofNullable(address.getAddress2()))
                .withAddress3(Optional.ofNullable(address.getAddress3()))
                .withAddress4(Optional.ofNullable(address.getAddress4()))
                .withAddress5(Optional.ofNullable(address.getAddress5()))
                .withPostcode(Optional.ofNullable(address.getPostcode()))
                .withWelshAddress1(Optional.ofNullable(address.getWelshAddress1()))
                .withWelshAddress2(Optional.ofNullable(address.getWelshAddress2()))
                .withWelshAddress3(Optional.ofNullable(address.getWelshAddress3()))
                .withWelshAddress4(Optional.ofNullable(address.getWelshAddress4()))
                .withWelshAddress5(Optional.ofNullable(address.getWelshAddress5()))
                .build());

    }

    private List<ReportingRestriction> buildReportingRestrictions(uk.gov.justice.listing.events.Offence offence) {
        return offence.getReportingRestrictions()
                .stream()
                .map(restriction ->
                        ReportingRestriction.reportingRestriction()
                                .withId(restriction.getId())
                                .withLabel(restriction.getLabel())
                                .withJudicialResultId(ofNullable(restriction.getJudicialResultId()))
                                .withOrderedDate(ofNullable(restriction.getOrderedDate()))
                                .build()).collect(toList());
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
