package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.LaaReference;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.util.List;
import java.util.Optional;
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
                    .withLaaApplnReference(offence.getLaaApplnReference().isPresent() ? convertLaaReference(offence.getLaaApplnReference().get()) : Optional.empty())
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

    private Optional<LaaReference> convertLaaReference(uk.gov.justice.listing.events.LaaReference laaReference){
        return Optional.ofNullable(LaaReference.laaReference()
                .withStatusCode(laaReference.getStatusCode())
                .withStatusId(laaReference.getStatusId())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusDate(laaReference.getStatusDate())
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withEffectiveEndDate(laaReference.getEffectiveEndDate())
                .withApplicationReference(laaReference.getApplicationReference())
                .build());
    }
}
