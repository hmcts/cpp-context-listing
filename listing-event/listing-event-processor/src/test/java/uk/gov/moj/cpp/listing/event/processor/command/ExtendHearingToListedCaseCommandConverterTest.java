package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.progression.courts.HearingExtended.hearingExtended;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.progression.courts.HearingExtended;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class ExtendHearingToListedCaseCommandConverterTest {

    private ExtendHearingToListedCaseCommandConverter extendHearingToListedCaseCommandConverter = new ExtendHearingToListedCaseCommandConverter();

    @Test
    public void convert() {
        final UUID hearingId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final HearingExtended event = hearingExtended()
                .withHearingId(hearingId)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId).build()))
                .build();
        final List<AddHearingToCaseCommand> result = extendHearingToListedCaseCommandConverter.convert(event);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCaseId(), is(prosecutionCaseId));
        assertThat(result.get(0).getHearingId(), is(hearingId));
    }
}