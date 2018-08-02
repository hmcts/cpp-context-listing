package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class UpdateOffencesForHearingCommandCollectionConverter implements Converter<OffencesToBeUpdated, List<UpdateOffencesForHearingCommand> > {

    @Override
    public List<UpdateOffencesForHearingCommand>  convert(final OffencesToBeUpdated event) {

        final List <Offence> offences = convertOffences(event.getOffences());
        return event.getHearings().stream().map(hearingId ->
                new UpdateOffencesForHearingCommand(offences, hearingId)).collect(toList());
    }

    private List<Offence> convertOffences(List<uk.gov.justice.listing.events.Offence> offences) {
        return offences.stream().map(offence -> {
            final LocalDate endDate = offence.getEndDate().map(LocalDates::from).orElse(null);
            final StatementOfOffence soo = convertStatementOfOffence(offence.getStatementOfOffence());
            return Offence.createOffenceBuilder()
                    .setDefendantId(offence.getDefendantId().toString())
                    .setEndDate(endDate)
                    .setId(offence.getId().toString())
                    .setOffenceCode(offence.getOffenceCode())
                    .setStartDate(LocalDates.from(offence.getStartDate()))
                    .setStatementOfOffence(soo)
                    .build();

        }).collect(toList());
    }

    private StatementOfOffence convertStatementOfOffence(uk.gov.justice.listing.events.StatementOfOffence soo) {
            return new StatementOfOffence(soo.getTitle(), soo.getLegislation());
    }
}
