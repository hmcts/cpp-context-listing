package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.UUID;

public class AddOffencesForHearingCommandCollectionConverter implements Converter<OffencesToBeAdded, List<AddOffencesForHearingCommand> > {

    @Override
    public List<AddOffencesForHearingCommand>  convert(final OffencesToBeAdded event) {

        final List <Offence> offences = convertOffences(event.getOffences());
        final UUID caseId = event.getCaseId();
        final UUID defendantId = event.getDefendantId();
        return event.getHearings().stream().map(hearingId ->
                new AddOffencesForHearingCommand(offences, hearingId, caseId, defendantId)).collect(toList());
    }

    private List<Offence> convertOffences(List<uk.gov.justice.listing.events.Offence> offences) {
        return offences.stream().map(offence -> {
            final StatementOfOffence soo = convertStatementOfOffence(offence.getStatementOfOffence());
            return Offence.offence()
                    .withEndDate(offence.getEndDate())
                    .withId(offence.getId())
                    .withOffenceCode(offence.getOffenceCode())
                    .withStartDate(offence.getStartDate())
                    .withStatementOfOffence(soo)
                    .withOffenceWording(offence.getOffenceWording())
                    .build();

        }).collect(toList());
    }

    private StatementOfOffence convertStatementOfOffence(uk.gov.justice.listing.events.StatementOfOffence soo) {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(soo.getLegislation())
                .withTitle(soo.getTitle())
                .withWelshLegislation(soo.getWelshLegislation())
                .withWelshTitle(soo.getWelshTitle())
                .build();
    }
}
