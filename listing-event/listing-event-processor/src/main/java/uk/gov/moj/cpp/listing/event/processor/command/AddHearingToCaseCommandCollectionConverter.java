package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AddHearingToCaseCommandCollectionConverter implements Converter<HearingListed, List<AddHearingToCaseCommand> > {

    @Override
    public List<AddHearingToCaseCommand>  convert(final HearingListed event) {

        UUID hearingId = event.getHearing().getId();
        return event.getHearing().getListedCases().stream()
                .map(listedCase -> AddHearingToCaseCommand.addHearingToCaseCommand()
                        .withCaseId(listedCase.getId())
                        .withHearingId(hearingId)
                        .build())
                .collect(Collectors.toList());
    }
}
