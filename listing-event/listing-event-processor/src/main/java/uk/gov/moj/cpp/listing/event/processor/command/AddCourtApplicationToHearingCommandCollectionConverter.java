package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;
import java.util.UUID;

public class AddCourtApplicationToHearingCommandCollectionConverter implements Converter<HearingListed, List<AddApplicationToHearingCommand> > {

    @Override
    public List<AddApplicationToHearingCommand>  convert(final HearingListed event) {

        UUID hearingId = event.getHearing().getId();
        return event.getHearing().getCourtApplications().stream()
                .map(courtApplication ->
                        AddApplicationToHearingCommand.addApplicationToHearingCommand().withApplicationId(courtApplication.getId())
                                .withHearingId(hearingId).build()).collect(toList());
    }
}
