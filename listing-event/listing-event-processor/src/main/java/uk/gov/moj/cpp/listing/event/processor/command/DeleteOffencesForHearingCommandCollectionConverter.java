package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.SimpleOffence;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class DeleteOffencesForHearingCommandCollectionConverter implements Converter<OffencesToBeDeleted, List<DeleteOffencesForHearingCommand> > {

    @Override
    public List<DeleteOffencesForHearingCommand>  convert(final OffencesToBeDeleted event) {

        final List <SimpleOffence> offences = convertOffences(event.getOffences());
        return event.getHearings().stream().map(hearingId ->
                new DeleteOffencesForHearingCommand(offences, hearingId)).collect(toList());
    }

    private List<SimpleOffence> convertOffences(List<uk.gov.justice.listing.events.SimpleOffence> offences) {
        return offences.stream().map(offence -> SimpleOffence.createSimpleOffenceBuilder()
                .setDefendantId(offence.getDefendantId().toString())
                .setId(offence.getId().toString())
                .build()
        ).collect(toList());
    }
}
