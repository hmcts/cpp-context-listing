package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Collections.emptyList;

import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class AddHearingToCaseCommandCollectionConverter implements Converter<HearingListed, List<AddHearingToCaseCommand> > {

    @Override
    public List<AddHearingToCaseCommand>  convert(final HearingListed event) {

        UUID hearingId = event.getHearing().getId();
        // Standalone applications will not have a case associated
        return Objects.isNull(event.getHearing().getListedCases()) ? emptyList() : event.getHearing().getListedCases().stream()
                .map(listedCase -> AddHearingToCaseCommand.addHearingToCaseCommand()
                        .withCaseId(listedCase.getId())
                        .withHearingId(hearingId)
                        .build())
                .collect(Collectors.toList());
    }
}
