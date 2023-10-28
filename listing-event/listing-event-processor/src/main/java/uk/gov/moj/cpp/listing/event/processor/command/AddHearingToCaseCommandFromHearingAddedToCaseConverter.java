package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class AddHearingToCaseCommandFromHearingAddedToCaseConverter  implements Converter<CasesAddedToHearing, List<AddHearingToCaseCommand>> {

    @Override
    public List<AddHearingToCaseCommand> convert(final CasesAddedToHearing event) {
        final UUID hearingId = event.getHearingId();
        return Objects.isNull(event.getUnAllocatedListedCases()) ? emptyList() : event.getUnAllocatedListedCases().stream()
                .map(listedCase -> AddHearingToCaseCommand.addHearingToCaseCommand()
                        .withCaseId(listedCase.getId())
                        .withHearingId(hearingId)
                        .build())
                .collect(Collectors.toList());
    }
}
