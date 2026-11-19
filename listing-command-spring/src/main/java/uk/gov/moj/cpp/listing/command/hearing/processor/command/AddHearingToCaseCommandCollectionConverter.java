package uk.gov.moj.cpp.listing.command.hearing.processor.command;

import org.springframework.stereotype.Component;
import static java.util.Collections.emptyList;

import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.HearingListed;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AddHearingToCaseCommandCollectionConverter {

    public List<AddHearingToCaseCommand> convert(final HearingListed event) {

        final UUID hearingId = event.getHearing().getId();
        // Standalone applications will not have a case associated
        return Objects.isNull(event.getHearing().getListedCases()) ? emptyList() : event.getHearing().getListedCases().stream()
                .map(listedCase -> AddHearingToCaseCommand.addHearingToCaseCommand()
                        .withCaseId(listedCase.getId())
                        .withHearingId(hearingId)
                        .build())
                .collect(Collectors.toList());
    }
}
