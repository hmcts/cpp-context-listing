package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import org.apache.commons.collections.CollectionUtils;

public class AddHearingToCaseCommandFromHearingAddedToCaseConverter implements Converter<CasesAddedToHearing, List<AddHearingToCaseCommand>> {

    @Override
    public List<AddHearingToCaseCommand> convert(final CasesAddedToHearing event) {
        final UUID hearingId = event.getHearingId();
        // Collect unique case ids
        final Set<UUID> caseIds = CollectionUtils.isEmpty(event.getUnAllocatedListedCases()) ? emptySet() : event.getUnAllocatedListedCases().stream()
                .map(listedCase -> listedCase.getId()).collect(Collectors.toSet());

        return CollectionUtils.isEmpty(caseIds) ? emptyList() : caseIds.stream()
                .map(caseId -> AddHearingToCaseCommand.addHearingToCaseCommand()
                        .withCaseId(caseId)
                        .withHearingId(hearingId)
                        .build())
                .collect(Collectors.toList());
    }
}
