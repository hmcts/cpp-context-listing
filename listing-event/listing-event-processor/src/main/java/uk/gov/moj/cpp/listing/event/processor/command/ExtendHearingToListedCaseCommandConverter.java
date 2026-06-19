package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.progression.courts.HearingExtended;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class ExtendHearingToListedCaseCommandConverter implements Converter<HearingExtended, List<AddHearingToCaseCommand> > {

    @Override
    public List<AddHearingToCaseCommand>  convert(final HearingExtended event) {

        final UUID hearingId = event.getHearingId();
        final List<ProsecutionCase> prosecutionCases = event.getProsecutionCases();
        // Standalone applications will not have a case associated
        return Objects.isNull(prosecutionCases) ? emptyList() : prosecutionCases.stream()
                .map(prosecutionCase -> AddHearingToCaseCommand.addHearingToCaseCommand()
                        .withCaseId(prosecutionCase.getId())
                        .withHearingId(hearingId)
                        .build())
                .collect(toList());
    }
}
