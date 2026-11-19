package uk.gov.moj.cpp.listing.command.hearing.processor.command;

import org.springframework.stereotype.Component;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.events.HearingListed;

import java.util.List;
import java.util.UUID;

@Component
public class AddCourtApplicationToHearingCommandCollectionConverter {

    public List<AddApplicationToHearingCommand>  convert(final HearingListed event) {

        UUID hearingId = event.getHearing().getId();
        return event.getHearing().getCourtApplications().stream()
                .map(courtApplication ->
                        AddApplicationToHearingCommand.addApplicationToHearingCommand().withApplicationId(courtApplication.getId())
                                .withHearingId(hearingId).build()).collect(toList());
    }
}
