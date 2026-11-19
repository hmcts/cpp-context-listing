package uk.gov.moj.cpp.listing.command.hearing.processor.command;

import org.springframework.stereotype.Component;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.moj.cpp.listing.domain.SimpleOffence;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Component
public class DeleteOffencesForHearingCommandCollectionConverter {

    public List<DeleteOffencesForHearingCommand>  convert(final OffencesToBeDeleted event) {

        final List <SimpleOffence> offences = convertOffences(event.getOffences());
        final UUID caseId = event.getCaseId();
        final UUID defendantId = event.getDefendantId();
        return event.getHearings().stream().map(hearingId ->
                new DeleteOffencesForHearingCommand(offences, hearingId, caseId, defendantId)).collect(toList());
    }

    private List<SimpleOffence> convertOffences(List<uk.gov.justice.listing.events.SimpleOffence> offences) {
        return offences.stream().map(offence -> SimpleOffence.createSimpleOffenceBuilder()
                .withDefendantId(offence.getDefendantId())
                .withId(offence.getId())
                .build()
        ).collect(toList());
    }
}
